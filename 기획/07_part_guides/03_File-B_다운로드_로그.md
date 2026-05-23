# 파트 가이드 — File-B (다운로드/로그/통계) — ⚠️ MVP에서 비활성

> ⚠️ **2026-05-22 회의 반영** — 본 문서는 **v1.1 참고용 보관**. 본 MVP 에서는 사용하지 않음.
>
> 회의 결정: File-A/B 페어 구분 폐기 → **File 통합 도메인**. 다운로드 로그·통계는 v1.1로 보류.
> 현재 유효한 파트 가이드는 [`02_File-A_저장.md`](./02_File-A_저장.md) (File 통합).



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **택배 보관소의 출고 창구.** 보관 중인 박스를 정확한 사람에게 건네주고, 누가 언제 가져갔는지 장부(DownloadLog)에 적는다. 장부는 사후 분쟁 대응의 핵심.

> 짝꿍: **File-A (저장)**. 둘이 같은 `file/` 패키지 안에 있어 매일 5분 페어 동기화 필요.

---

## 1. 책임 한 줄

파일 다운로드 응답 스트리밍, `DownloadLog` 적재(비동기), 어드민 다운로드 통계 API.

---

## 2. 패키지 / 파일 소유권

### 내 소유
```
com.assetbox.file/
├─ controller/  DownloadLogController.java
├─ service/     DownloadService.java
├─ repository/  DownloadLogRepository.java
└─ domain/      DownloadLog.java
```

### 공동 (File-A와)
- `FileStorageService` 인터페이스의 **load 쪽** 메서드 (`loadAsResource(key)`)

### 절대 손대지 말 것
- File-A의 `LocalFileStorageService` (구현체)
- File-A의 `AssetFile`, `FileService.saveAndAttach`

> 단, 다운로드 시 `AssetFileRepository.findByIdAndPostId(...)` 같은 조회는 허용. **쓰기는 절대 X.**

---

## 3. 외부 컨트랙트 (내가 제공)

```java
public interface DownloadService {
    /**
     * 파일 다운로드 응답을 만든다. 로그는 비동기.
     * @param postId, fileId  대상
     * @param requesterUserId 비로그인 시 null
     * @param ipAddress, userAgent  요청 메타
     * @return Resource + HTTP 헤더(Content-Disposition, Content-Type)
     */
    DownloadResult download(Long postId, Long fileId, Long requesterUserId, String ipAddress, String userAgent);
}

public record DownloadResult(Resource resource, String originalName, String contentType, long sizeBytes) {}
```

### REST 엔드포인트
- `GET /api/posts/{postId}/files/{fileId}` — Post-A 컨트롤러에 등록되어 있지만 **본체는 DownloadService 호출**
  - 위치는 PostController 안이지만 **로직은 File-B 소유** (한 메서드만 DownloadService 위임)
  - 합의된 컨벤션: 컨트롤러 메서드의 본문은 5줄 이내, 다른 도메인 서비스 호출 위주
- `/api/admin/download-logs/**` (전부 내 소유)

---

## 4. 의존하는 컨트랙트

- `FileStorageService.loadAsResource(storedPath)` — File-A
- `AssetFileRepository.findByIdAndPostId(...)` — File-A (read-only)
- `@Async` 풀 — Infra의 `AsyncConfig`

---

## 5. 단계별 작업 가이드

### Day 1-2
- [ ] 짝꿍과 인터페이스 합의, `DownloadService` 시그니처 결정
- [ ] `DownloadLog` 엔티티/리포지토리 확인

### Day 3-5: MVP
- [ ] 다운로드 흐름: AssetFile 조회 → Resource 로딩 → 응답 헤더 작성 → @Async로 DownloadLog 저장
- [ ] 응답 헤더: `Content-Disposition: attachment; filename*=UTF-8''<encoded>` (한글 파일명 깨짐 방지)
- [ ] AssetFile 미존재 시 404 `FILE_NOT_FOUND`, Post 미존재면 404 `POST_NOT_FOUND`

### Day 6-9: 통합
- [ ] Post-A 컨트롤러의 다운로드 엔드포인트 위치 확정 (이미 `PostController.java` 에 있음 — 본체만 본 서비스 사용)
- [ ] 통합 테스트 데이 — 한 사이클 (가입 → 게시글 → 다운로드 → 로그 확인) 끊김 없이

### M2: SHOULD
- [ ] 통계 API: top-files, count-by-file, 기간 필터
- [ ] 어드민 페이지가 호출하는 형태 확정

### M3
- [ ] 비로그인 다운로드 정책 재확인 (회의록: 비로그인 허용. v1.1에서 제한 검토)
- [ ] 다운로드 평균/총 횟수 메트릭 (Prometheus, Infra 협업)

---

## 6. 인수 기준 (AC)

### F-03 다운로드 + 로그
- [ ] 비로그인 다운로드 가능, DownloadLog.userId = null
- [ ] 로그인 시 userId 적재
- [ ] DownloadLog 적재 실패가 다운로드 응답을 깨뜨리지 않음 (@Async + 예외 격리)
- [ ] 같은 파일 N회 다운로드 시 N개 로그
- [ ] 응답에 정확한 `Content-Length`

### F-05 통계
- [ ] `top-files` 쿼리는 인덱스 사용 (`idx_dl_file_id`)
- [ ] 기간 필터 from/to inclusive
- [ ] 페이지네이션 size 최대 50

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| `@Async` 가 트랜잭션 전파를 끊는 줄 모름 | 로그가 DB에 안 들어감 | `@Async` 메서드 안에 `@Transactional` 명시 + Infra의 AsyncConfig 풀 사용 |
| 응답 헤더의 filename에 인코딩 누락 | 한글 깨짐 | `filename*=UTF-8''<URLEncoded>` 사용 |
| 파일 미존재인데 디스크 read에서 NPE | 500 노출 | 404 `FILE_NOT_FOUND` 명시 |
| AssetFile.deleted 같은 컬럼 없는데 soft delete 가정 | 통계 오류 | 본 프로젝트는 AssetFile hard delete. 통계는 그대로 |
| File-A의 saveAndAttach 와 동시 PR로 인터페이스 변경 | 컴파일 깨짐 | 인터페이스 변경 PR은 양측이 같이 |
