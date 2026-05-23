# 04. API 명세서 — File

> ⚠️ **2026-05-22 회의 반영분과 충돌 시** [`02_엔드포인트_목록_상태코드분리_수정본.md`](./02_엔드포인트_목록_상태코드분리_수정본.md) **이 진실의 단일 원본.** 이 파일은 회의 반영 핵심 변경만 머리에 박아둠. 본문 상세는 회의록과 합쳐서 읽기.


> 담당 파트: **File-A (저장) + File-B (다운로드/로그)**
> 베이스 경로: `/api/posts/{postId}/files/**`, `/api/admin/download-logs/**`
> 파일 업로드는 Post 작성/수정 API의 multipart 안에 들어가므로 별도 엔드포인트는 없습니다. (`05_명세서_Post.md` 의 P-1 참고)

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| F-1 | GET | `/api/posts/{postId}/files/{fileId}` | 익명 | 파일 다운로드 + 로그 |
| F-2 | GET | `/api/admin/download-logs` | ADMIN | 로그 페이지 조회 |
| F-3 | GET | `/api/admin/download-logs/posts/{postId}` | ADMIN | 게시글별 로그 |
| F-4 | GET | `/api/admin/download-logs/files/{fileId}/count` | ADMIN | 파일별 다운로드 횟수 |
| F-5 | GET | `/api/admin/download-logs/top-files` | ADMIN | 인기 파일 통계 |

---

## F-1. GET `/api/posts/{postId}/files/{fileId}`

**설명**: 게시글에 첨부된 파일 다운로드. 비로그인도 가능 (회의록 결정). 다운로드 직후 `DownloadLog` 비동기 적재.
**인증**: 익명 (로그인 시 userId 적재됨)

### 요청

```http
GET /api/posts/42/files/137
User-Agent: Mozilla/5.0 ...
```

| Path | 타입 | 비고 |
|---|---|---|
| postId | long | 대상 게시글 |
| fileId | long | 대상 파일 (`postId` 와 일치해야 함) |

### 응답 200

```
Content-Type: application/octet-stream (또는 image/png 등 추론)
Content-Disposition: attachment; filename*=UTF-8''chair-low-poly.fbx
Content-Length: 524288
Cache-Control: no-store

<binary>
```

> `filename*=UTF-8''` 인코딩 형식으로 한글 파일명 깨짐 방지.

### 부수효과

- DownloadLog 1행 적재 (`@Async`):
  - `user_id`: 로그인 사용자면 그 id, 아니면 null
  - `post_id`, `file_id`, `original_name`
  - `ip_address`: 요청 IP (X-Forwarded-For 우선)
  - `user_agent`: 요청 User-Agent (512자 truncate)

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 404 | `POST_NOT_FOUND` | postId 미존재 |
| 404 | `FILE_NOT_FOUND` | fileId 미존재 |
| 400 | `FILE_POST_MISMATCH` | fileId가 다른 게시글의 파일 |
| 410 | `FILE_DELETED` | (정책 합의 시) 게시글 soft delete 후 다운로드 차단 |
| 500 | `STORAGE_READ_FAILED` | 디스크/S3 읽기 실패 |

---

## F-2. GET `/api/admin/download-logs`

**설명**: 다운로드 로그 페이지 조회. 필터 결합.
**인증**: ADMIN

### 요청

```http
GET /api/admin/download-logs?page=0&size=20&fileId=137&userId=12&from=2026-05-20&to=2026-06-16
Authorization: Bearer <admin-jwt>
```

| Query | 타입 | 비고 |
|---|---|---|
| page, size | int | 표준 |
| fileId | long? | 특정 파일만 |
| postId | long? | 특정 게시글만 |
| userId | long? | 특정 사용자만 (비로그인 다운로드 조회 시 `userId=null` 쿼리는 미지원 — 별도 정책) |
| from | date? | 시작일 (inclusive) |
| to | date? | 종료일 (inclusive) |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 9001,
        "userId": 12,
        "userNickname": "김TA",
        "postId": 42,
        "fileId": 137,
        "originalName": "chair-low-poly.fbx",
        "ipAddress": "10.0.0.42",
        "userAgent": "Mozilla/5.0 ...",
        "createdAt": "2026-05-21T14:30:15"
      }
    ],
    "page": 0, "size": 20, "totalElements": 350, "totalPages": 18,
    "first": true, "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `DATE_RANGE_INVALID` | from > to |
| 400 | `PAGINATION_SIZE_TOO_LARGE` | size > 50 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | USER 호출 |

---

## F-3. GET `/api/admin/download-logs/posts/{postId}`

**설명**: 특정 게시글의 다운로드 로그 전체 (시간 DESC). 페이지네이션.
**인증**: ADMIN

### 요청

```http
GET /api/admin/download-logs/posts/42?page=0&size=50
```

### 응답 200

`F-2` 와 동일 스키마.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |
| 404 | `POST_NOT_FOUND` | |

---

## F-4. GET `/api/admin/download-logs/files/{fileId}/count`

**설명**: 특정 파일의 다운로드 총 횟수.
**인증**: ADMIN

### 요청

```http
GET /api/admin/download-logs/files/137/count
```

### 응답 200

```json
{
  "success": true,
  "data": { "fileId": 137, "count": 248 }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |
| 404 | `FILE_NOT_FOUND` | (정책: 0 반환할지 404 낼지 합의. 본 명세는 404) |

---

## F-5. GET `/api/admin/download-logs/top-files`

**설명**: 다운로드 횟수 상위 파일.
**인증**: ADMIN

### 요청

```http
GET /api/admin/download-logs/top-files?limit=10&from=2026-05-20&to=2026-06-16
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| limit | int | 10 | 최대 50 |
| from | date? | - | |
| to | date? | - | |

### 응답 200

```json
{
  "success": true,
  "data": [
    {
      "fileId": 137,
      "originalName": "chair-low-poly.fbx",
      "postId": 42,
      "postTitle": "캐주얼 의자 (low poly)",
      "count": 248
    },
    {
      "fileId": 201,
      "originalName": "stool.glb",
      "postId": 78,
      "postTitle": "스툴",
      "count": 173
    }
  ]
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `DATE_RANGE_INVALID` | from > to |
| 400 | `LIMIT_TOO_LARGE` | limit > 50 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |

---

## 부록 — 업로드 컨트랙트 (Post 도메인에서 호출)

업로드는 별도 엔드포인트가 없고 Post 작성/수정 시 호출됩니다. File-A의 서비스 계약:

```java
List<AssetFile> saveAndAttach(Long postId, List<MultipartFile> files, MultipartFile thumbnail);
```

| 입력 | 제약 | 위반 시 |
|---|---|---|
| 에셋 파일 확장자 | `fbx, blend, obj, glb, gltf, zip` | 400 `FILE_EXTENSION_NOT_ALLOWED` |
| 썸네일 확장자 | `png, jpg, jpeg` | 400 `FILE_EXTENSION_NOT_ALLOWED` |
| 에셋 합계 사이즈 | ≤ 20MB | 400 `FILE_TOO_LARGE` |
| 썸네일 사이즈 | ≤ 1MB | 400 `FILE_TOO_LARGE` |
| 0바이트 파일 | 금지 | 400 `FILE_EMPTY` |
| 게시글당 썸네일 수 | 최대 1 | 400 `THUMBNAIL_MULTIPLE_NOT_ALLOWED` |

자세한 동작 흐름은 `07_part_guides/02_File-A_저장.md` 참고.
