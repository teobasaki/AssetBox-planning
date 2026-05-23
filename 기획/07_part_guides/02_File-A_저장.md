# 파트 가이드 — File (통합 도메인, 2명 페어)

> ⚠️ **2026-05-22 회의 반영** — 본 문서는 회의 결과 패치본. 회의록 단일 진실 원본은 [`../04_api/02_엔드포인트_목록_상태코드분리_수정본.md`](../04_api/02_엔드포인트_목록_상태코드분리_수정본.md).

## 회의 반영 핵심 변경 (File 통합 도메인)

> 회의 결정: **File-A / File-B 분리가 없어지고 통합 1개 도메인.** 두 명이 같은 도메인을 페어 프로그래밍으로 같이 맡되, 내부에서 자유롭게 분담. (03번 File-B 가이드는 v1.1 참고용으로만 보관)

| 영역 | 새 내용 |
|---|---|
| **단일 도메인** | 저장 + 조회 + 메타 + 삭제 모두 한 도메인. `purpose` 컬럼으로 4종 구분: `ASSET / POST_THUMBNAIL / USER_AVATAR / REQUEST_REFERENCE` |
| **단일 입구** | `GET /api/files/{fileId}` 가 모든 파일/이미지 조회의 입구. 게시글·아바타·참고이미지 전부 여기 |
| **업로드 위임** | 업로드 자체는 Post / User / Request API 가 multipart 로 받고 `FileService.save(purpose, ownerId, file)` 에 위임 |
| **두 명 내부 분담 (권장)** | A=업로드/저장 백엔드/검증 정책 · B=조회/메타/삭제/권한. 둘이 페어로 인터페이스 합의 |
| **8가지 책임** | 저장 / 조회 / 메타 / 삭제 / 공용 서비스 / purpose 정책 / 권한 검증 / 저장 백엔드 추상화 |
| **MVP 제외 (v1.1)** | DownloadLog, top-files, 통계, S3 전환, presigned URL |

> 아래 본문 옛 가이드(File-A 단독)와 충돌 시 본 박스 우선.



> 모르는 단어는 [`../00_overview/01_용어집.md`](../00_overview/01_용어집.md) 참고.
> 비유: **택배 보관소.** 사람들이 맡긴 박스(파일)를 깔끔하게 분류해서 선반(디스크/S3)에 올린다. 받을 때 박스의 무게·내용물(확장자)을 검사하고, 손상되면 거절한다.

> 짝꿍: **File-B (다운로드/로그)**. 같은 `file/` 패키지를 공유하지만, A는 "저장·연결", B는 "전달·기록". 매일 5분 데일리 페어 동기화 권장.

---

## 1. 책임 한 줄

파일 업로드, 확장자/사이즈 검증, 저장 추상화(`FileStorageService`), `AssetFile` 엔티티 라이프사이클 (저장/삭제/연결).

---

## 2. 패키지 / 파일 소유권

### 내 소유 (자유 수정)
```
com.assetbox.file/
├─ service/
│   ├─ FileStorageService.java        (인터페이스 — 짝꿍과 공동)
│   ├─ LocalFileStorageService.java   ★ 내 책임
│   └─ FileService.java               ★ 내 책임 (saveAndAttach, deleteByPost)
├─ repository/
│   └─ AssetFileRepository.java
└─ domain/
    └─ AssetFile.java
```

### 공동 (페어 — File-B와 매일 동기화)
- `FileStorageService.java` 인터페이스 → A가 저장 메서드, B가 로딩 메서드를 함께 정의

### 절대 손대지 말 것
- File-B의 `DownloadService.java`, `DownloadLog.java`, `DownloadLogController.java`
- `Post` / `Request` / 다른 도메인 패키지

---

## 3. 외부 컨트랙트 (내가 제공)

```java
public interface FileService {
    /**
     * Post 작성/수정 시 호출. 파일들을 디스크/S3에 저장하고 AssetFile 엔티티 생성.
     * @param postId       대상 Post id (이미 저장된 상태)
     * @param files        업로드된 MultipartFile 목록
     * @param thumbnail    썸네일 한 장 (없으면 null)
     * @return 생성된 AssetFile 목록
     * @throws BusinessException 확장자/사이즈 위반 시 (400)
     */
    List<AssetFile> saveAndAttach(Long postId, List<MultipartFile> files, MultipartFile thumbnail);

    /** 게시글 삭제 시 모든 파일 삭제 (디스크 + DB). hard. */
    void deleteByPost(Long postId);

    /** 게시글에 붙은 모든 파일 (썸네일 포함). DTO 매핑은 호출자 책임. */
    List<AssetFile> findByPost(Long postId);
}
```

### 컨트랙트 보장
- 확장자 화이트리스트: `fbx, blend, obj, glb, gltf, zip, png, jpg, jpeg`
- 합계 사이즈 ≤ 20MB
- 썸네일은 `png/jpg/jpeg` 만, 별도 합계 외 1MB
- 저장 실패 시 이미 디스크에 쓴 파일은 cleanup (`@Transactional` + try-catch + 파일 삭제)
- `AssetFile.storedPath` 는 **storage backend가 바뀌어도 의미를 유지하는 key** (로컬: 상대경로, S3: object key)

---

## 4. 의존하는 컨트랙트

- **없음(엔티티 의존 X)**: postId 만 받는다. 굳이 PostService를 부를 필요 없음 (호출 시점에 이미 Post가 저장된 상태).

---

## 5. 단계별 작업 가이드

### Day 1-2
- [ ] 기존 `file/` 패키지 정독, 인터페이스가 잘 분리되었는지 확인
- [ ] 짝꿍(File-B)와 30분 미팅 — `FileStorageService` 인터페이스 합의

### Day 3-5: MVP
- [ ] `FileService.saveAndAttach` 트랜잭션 + 보상 로직 (디스크 cleanup) 견고화
- [ ] 확장자 검증 상수 `FileExtensionPolicy` 추출, 변경 한 곳에서
- [ ] AssetFile 응답 DTO `AssetFileResponse{id, originalName, extension, sizeBytes, thumbnail, downloadUrl}` 통일

### Day 6-9: 통합
- [ ] Post-A와 페어 작업: `POST /api/posts` 의 multipart 파싱 + saveAndAttach 호출 흐름
- [ ] 게시글 수정 시 파일 부분 교체 정책 합의 (전체 재업로드 vs 추가/삭제 별도 API) — 본 MVP는 **전체 재업로드** 로 시작 (간단)
- [ ] 통합 테스트 데이(5/30) 참여

### M2
- [ ] 게시글 삭제 시 cascading 정리, 디스크에서도 제거
- [ ] 저장 백엔드 인터페이스 점검 (S3 전환 v1.1 준비) — `FileStorageService` 만 구현체 바꾸면 되는 설계 유지

### M3
- [ ] 업로드 실패 케이스 정리 (네트워크 중단, 디스크 가득 참 등)
- [ ] 로그/메트릭: 업로드 횟수/평균 사이즈 (Infra 협업)

---

## 6. 인수 기준 (AC)

### F-01 업로드
- [ ] 확장자 위반 → 400 `FILE_EXTENSION_NOT_ALLOWED`
- [ ] 합계 20MB 초과 → 400 `FILE_TOO_LARGE`
- [ ] 0바이트 파일 → 400 `FILE_EMPTY`
- [ ] DB에 AssetFile 저장 + 디스크에 파일 존재 확인 (통합 테스트)
- [ ] 도중 실패 시 cleanup (트랜잭션 롤백 + 디스크 파일 삭제)

### F-02 썸네일
- [ ] thumbnail=true 는 게시글당 최대 1개
- [ ] 기존 썸네일이 있으면 교체 (디스크에서도 삭제)

### F-04 게시글 삭제 시
- [ ] 모든 AssetFile DB 삭제 + 디스크 파일 삭제 (best-effort, 실패 시 로깅)

---

## 7. 충돌 방지 / 함정

| 함정 | 결과 | 회피 |
|---|---|---|
| 저장 실패 시 cleanup 누락 | 디스크에 쓰레기 누적 | try-finally 로 보장. 통합 테스트에서 의도적 실패 케이스 검증 |
| Post에서 AssetFile 직접 생성 | 책임 경계 깨짐 | "FileService.saveAndAttach 만 사용" 룰. Post-A와 매주 확인 |
| 저장 경로를 절대경로로 박음 | 운영 환경 깨짐 | 항상 상대 경로 + Storage 인터페이스가 base 결합 |
| `MultipartFile.getOriginalFilename()` 그대로 저장 | 경로 트래버설 위험 | UUID 또는 hash 기반으로 storedName 생성, originalName은 메타로만 |
| File-B와 같은 인터페이스를 동시에 수정 | 머지 충돌 | 인터페이스 변경은 양측 합의 후 한 PR에 같이 |
