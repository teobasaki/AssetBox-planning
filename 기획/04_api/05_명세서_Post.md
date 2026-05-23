# 05. API 명세서 — Post

> ⚠️ **2026-05-22 회의 반영분과 충돌 시** [`02_엔드포인트_목록_상태코드분리_수정본.md`](./02_엔드포인트_목록_상태코드분리_수정본.md) **이 진실의 단일 원본.** 이 파일은 회의 반영 핵심 변경만 머리에 박아둠. 본문 상세는 회의록과 합쳐서 읽기.


> 담당 파트: **Post-A**
> 베이스 경로: `/api/posts/**`, `/api/admin/posts/**`

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| P-1 | POST | `/api/posts` | USER | 게시글 작성 (multipart) |
| P-2 | GET | `/api/posts` | 익명 | 게시글 목록 / 검색 |
| P-3 | GET | `/api/posts/popular-tags` | 익명 | 인기 태그 |
| P-4 | GET | `/api/posts/liked` | USER | 내가 좋아요 누른 글 |
| P-5 | GET | `/api/posts/{postId}` | 익명 | 게시글 상세 (view +1) |
| P-6 | PUT | `/api/posts/{postId}` | USER (작성자) | 게시글 수정 |
| P-7 | DELETE | `/api/posts/{postId}` | USER (작성자) | 게시글 삭제 (soft) |
| P-8 | POST | `/api/posts/{postId}/like` | USER | 좋아요 토글 |
| P-9 | GET | `/api/admin/posts` | ADMIN | 어드민 게시글 목록 |
| P-10 | DELETE | `/api/admin/posts/{id}` | ADMIN | 어드민 강제 삭제 (hard) |

---

## P-1. POST `/api/posts`

**설명**: 게시글 + 파일 + 태그를 한 번에 작성. multipart.
**인증**: USER

### 요청

```http
POST /api/posts
Content-Type: multipart/form-data; boundary=...
Authorization: Bearer <jwt>
```

```
--boundary
Content-Disposition: form-data; name="data"
Content-Type: application/json

{
  "title": "캐주얼 의자 (low poly)",
  "content": "Blender 4.0 / 트라이앵글 1.2k\n자유롭게 사용 가능합니다.",
  "categoryId": 12,
  "tags": ["furniture", "low-poly", "chair"]
}
--boundary
Content-Disposition: form-data; name="files"; filename="chair-low-poly.fbx"
Content-Type: application/octet-stream

<binary>
--boundary
Content-Disposition: form-data; name="thumbnail"; filename="thumb.png"
Content-Type: image/png

<binary>
--boundary--
```

| Part | 필드 | 타입 | 제약 |
|---|---|---|---|
| data | title | string | NotBlank, max 100 |
| data | content | string | NotBlank |
| data | categoryId | long? | null이면 미지정 |
| data | tags | string[] | 0~10개, 각 30자 |
| files | binary | - | 1개 이상 권장 |
| thumbnail | image/png \| jpeg | - | 0 또는 1개, ≤ 1MB |

### 응답 201

```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "캐주얼 의자 (low poly)",
    "content": "Blender 4.0 ...",
    "authorNickname": "김TA",
    "authorId": 12,
    "categoryId": 12,
    "categoryName": "소품",
    "tags": ["furniture", "low-poly", "chair"],
    "files": [
      {
        "id": 137,
        "originalName": "chair-low-poly.fbx",
        "extension": "fbx",
        "sizeBytes": 524288,
        "thumbnail": false
      },
      {
        "id": 138,
        "originalName": "thumb.png",
        "extension": "png",
        "sizeBytes": 23456,
        "thumbnail": true
      }
    ],
    "viewCount": 0,
    "likeCount": 0,
    "downloadCount": 0,
    "liked": false,
    "createdAt": "2026-05-21T14:30:15"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | title/content 누락 |
| 400 | `FILE_EXTENSION_NOT_ALLOWED` | 확장자 위반 |
| 400 | `FILE_TOO_LARGE` | 파일 합계 20MB 초과 |
| 400 | `FILE_EMPTY` | 0바이트 파일 |
| 400 | `THUMBNAIL_MULTIPLE_NOT_ALLOWED` | 썸네일 2개 이상 |
| 400 | `TAGS_TOO_MANY` | 11개 이상 |
| 401 | `UNAUTHORIZED` | |
| 404 | `CATEGORY_NOT_FOUND` | categoryId 미존재 |
| 500 | `STORAGE_WRITE_FAILED` | 디스크 저장 실패 (트랜잭션 롤백 + cleanup) |

---

## P-2. GET `/api/posts`

**설명**: 게시글 목록 + 검색의 단일 입구. 카테고리/태그/키워드/작성자/팀 필터 + 정렬.
**인증**: 익명

### 요청

```http
GET /api/posts?page=0&size=20&sort=likeCount,desc&q=chair&tag=low-poly&categoryId=12
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page | int | 0 | |
| size | int | 20 | 최대 50 |
| sort | string | `createdAt,desc` | 화이트리스트: `createdAt`, `likeCount`, `viewCount` |
| q | string? | - | title + content 부분 매치 |
| tag | string? | - | 정확 매치 (정규화 후) |
| categoryId | long? | - | 정확 매치 (자식 미포함, v1.1) |
| authorId | long? | - | |
| teamId | long? | - | |
| includeDeleted | bool | false | 어드민만 true 허용 (USER가 true로 보내면 무시) |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 42,
        "title": "캐주얼 의자 (low poly)",
        "authorNickname": "김TA",
        "authorId": 12,
        "categoryId": 12,
        "tags": ["furniture", "low-poly", "chair"],
        "thumbnailUrl": "/api/posts/42/files/138",
        "fileExtension": "fbx",
        "viewCount": 173,
        "likeCount": 24,
        "commentCount": 5,
        "downloadCount": 89,
        "createdAt": "2026-05-21T14:30:15"
      }
    ],
    "page": 0, "size": 20, "totalElements": 137, "totalPages": 7,
    "first": true, "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `PAGINATION_SIZE_TOO_LARGE` | size > 50 |
| 400 | `SORT_KEY_NOT_ALLOWED` | sort 키 위반 |
| 404 | `CATEGORY_NOT_FOUND` | categoryId 미존재 |

---

## P-3. GET `/api/posts/popular-tags`

**설명**: 사용 빈도 기준 상위 태그. Caffeine 캐시 60초.
**인증**: 익명

### 요청

```http
GET /api/posts/popular-tags?limit=10
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| limit | int | 10 | 최대 50 |

### 응답 200

```json
{
  "success": true,
  "data": [
    { "name": "low-poly", "count": 87 },
    { "name": "furniture", "count": 64 },
    { "name": "stylized", "count": 41 }
  ]
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `LIMIT_TOO_LARGE` | limit > 50 |

---

## P-4. GET `/api/posts/liked`

**설명**: 현재 사용자가 좋아요 누른 게시글 목록.
**인증**: USER

### 요청

```http
GET /api/posts/liked?page=0&size=20
Authorization: Bearer <jwt>
```

### 응답 200

`P-2` 와 동일 스키마 (Summary).

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |

---

## P-5. GET `/api/posts/{postId}`

**설명**: 게시글 상세. 조회수 +1 (별도 트랜잭션).
**인증**: 익명 (로그인 시 `liked` 필드가 의미 있음)

### 요청

```http
GET /api/posts/42
Authorization: Bearer <jwt>   # 선택. 있으면 liked 정확
```

### 응답 200

`P-1` 의 응답과 동일 스키마. 단 viewCount 가 +1 된 값.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 404 | `POST_NOT_FOUND` | |
| 410 | `POST_DELETED` | 작성자 soft delete (어드민이 보려면 `/api/admin/posts/{id}` 사용) |

---

## P-6. PUT `/api/posts/{postId}`

**설명**: 게시글 수정. 본문 + 카테고리 + 태그. 파일은 별도 정책 (현 MVP: 전체 재업로드만 가능 → 새 게시글로 권장).
**인증**: USER (작성자 본인만)

### 요청

```http
PUT /api/posts/42
Content-Type: application/json
Authorization: Bearer <jwt>
```

```json
{
  "title": "캐주얼 의자 (low poly, v2)",
  "content": "트라이앵글 800으로 축소.",
  "categoryId": 12,
  "tags": ["furniture", "low-poly", "chair", "v2"]
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| title | string | NotBlank |
| content | string | NotBlank |
| categoryId | long? | |
| tags | string[]? | |

### 응답 200

`P-1` 응답과 동일.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 작성자 아님 |
| 404 | `POST_NOT_FOUND` | |
| 404 | `CATEGORY_NOT_FOUND` | |

---

## P-7. DELETE `/api/posts/{postId}`

**설명**: 게시글 삭제. **soft delete** (`deleted=true`). 어드민 외엔 목록/상세에서 안 보임. 작성자 본인만.
**인증**: USER (작성자)

### 요청

```http
DELETE /api/posts/42
Authorization: Bearer <jwt>
```

### 응답 200

```json
{ "success": true, "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 작성자 아님 |
| 404 | `POST_NOT_FOUND` | |

---

## P-8. POST `/api/posts/{postId}/like`

**설명**: 좋아요 토글. 누른 적 없으면 +1, 있으면 -1. 단일 POST로 처리.
**인증**: USER

### 요청

```http
POST /api/posts/42/like
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "likeCount": 25,
    "liked": true
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `POST_NOT_FOUND` | |

---

## P-9. GET `/api/admin/posts`

**설명**: 어드민용 게시글 목록. soft delete된 글도 포함 가능.
**인증**: ADMIN

### 요청

```http
GET /api/admin/posts?page=0&size=20&includeDeleted=true&authorId=12
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| includeDeleted | bool | true | 기본 어드민은 모두 보기 |
| 그 외 | - | - | P-2 와 동일 |

### 응답 200

P-2 응답 + 각 항목에 `deleted` (bool) 필드 추가.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | |

---

## P-10. DELETE `/api/admin/posts/{id}`

**설명**: 어드민 강제 삭제. **hard delete** (DB row + 디스크 파일 모두 삭제). 분쟁 / 부적절 콘텐츠 대응.
**인증**: ADMIN

### 요청

```http
DELETE /api/admin/posts/42
Authorization: Bearer <admin-jwt>
```

### 응답 200

```json
{ "success": true, "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | USER 호출 |
| 404 | `POST_NOT_FOUND` | |
| 500 | `STORAGE_DELETE_FAILED` | 디스크 삭제 실패 (DB는 성공해도 best-effort) |

---

## 부록 — 응답 스키마 요약

### PostSummaryResponse (목록)

```ts
{
  id: long, title: string, authorNickname: string, authorId: long,
  categoryId: long | null, tags: string[],
  thumbnailUrl: string | null, fileExtension: string | null,
  viewCount: long, likeCount: long, commentCount: long, downloadCount: long,
  createdAt: ISODateTime
}
```

### PostDetailResponse (상세)

```ts
{
  id, title, content, authorNickname, authorId,
  categoryId, categoryName, tags: string[],
  files: FileInfo[],
  viewCount, likeCount, downloadCount, liked: boolean,
  createdAt
}

FileInfo = { id, originalName, extension, sizeBytes, thumbnail }
```
