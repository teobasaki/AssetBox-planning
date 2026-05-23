# 03. API 명세서 — User

> ⚠️ **2026-05-22 회의 반영분과 충돌 시** [`02_엔드포인트_목록_상태코드분리_수정본.md`](./02_엔드포인트_목록_상태코드분리_수정본.md) **이 진실의 단일 원본.** 이 파일은 회의 반영 핵심 변경만 머리에 박아둠. 본문 상세는 회의록과 합쳐서 읽기.


> 담당 파트: **User**
> 베이스 경로: `/api/users/**`, `/api/admin/users/**`
> 응답 래퍼: `{success, data}` 또는 `{success, error}` (표준 01 참고)

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| U-1 | POST | `/api/users/signup` | 익명 | 회원가입 |
| U-2 | POST | `/api/users/login` | 익명 | 로그인 + JWT 발급 |
| U-3 | GET  | `/api/users/me` | USER | 내 정보 |
| U-4 | PUT  | `/api/users/me` | USER | 내 정보 수정 |
| U-5 | POST | `/api/users/me/avatar` | USER | 아바타 업로드 |
| U-6 | GET  | `/api/users/directory` | USER | 유저 디렉토리 |
| U-7 | GET  | `/api/users/search` | USER | 닉네임 자동완성 |
| U-8 | GET  | `/api/users/{id}` | USER | 특정 유저 정보 |
| U-9 | GET  | `/api/users/{id}/avatar` | 익명 | 아바타 이미지 |
| U-10 | GET  | `/api/admin/users` | ADMIN | 어드민 유저 목록 |
| U-11 | PATCH | `/api/admin/users/{id}/role` | SUPER_ADMIN | 권한 변경 |
| U-12 | DELETE | `/api/admin/users/{id}` | SUPER_ADMIN | 유저 삭제 |

---

## U-1. POST `/api/users/signup`

**설명**: 이메일·비밀번호·닉네임으로 회원가입. 가입 즉시 USER 권한.
**인증**: 익명

### 요청

```http
POST /api/users/signup
Content-Type: application/json
```

```json
{
  "email": "kim@example.com",
  "password": "p@ssw0rd!",
  "nickname": "김TA"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| email | string | RFC 5322, max 50 |
| password | string | 8~50자 |
| nickname | string | 2~30자 |

### 응답 201

```json
{
  "success": true,
  "data": {
    "id": 12,
    "email": "kim@example.com",
    "nickname": "김TA",
    "role": "USER",
    "bio": null,
    "avatarUrl": null
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 이메일 형식 위반 / 비번 길이 / 닉네임 길이 |
| 409 | `USER_EMAIL_DUPLICATED` | 이미 가입된 이메일 |
| 409 | `USER_NICKNAME_DUPLICATED` | (정책 합의 시) 닉네임 중복 |

> 가입 직후 자동 로그인은 하지 않음. 프론트가 즉시 `/login` 호출 또는 응답에 토큰 포함 옵션은 v1.1.

---

## U-2. POST `/api/users/login`

**설명**: 비밀번호 검증 후 JWT 발급. 응답에는 토큰 + 토큰 타입만. 사용자 정보는 `/me` 로 별도 조회.
**인증**: 익명

### 요청

```json
{
  "email": "kim@example.com",
  "password": "p@ssw0rd!"
}
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 이메일/비번 누락 |
| 401 | `LOGIN_FAILED` | 잘못된 비번 **또는** 존재하지 않는 이메일 (계정 존재 노출 X) |

> 로그인 실패 시 두 케이스를 동일 코드로 묶는 게 보안 권장.

---

## U-3. GET `/api/users/me`

**설명**: 현재 토큰의 사용자 정보.
**인증**: USER

### 요청

```http
GET /api/users/me
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "id": 12,
    "email": "kim@example.com",
    "nickname": "김TA",
    "role": "USER",
    "bio": "캐주얼 모델링 좋아함",
    "avatarUrl": "/api/users/12/avatar"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | 토큰 없음 |
| 401 | `TOKEN_EXPIRED` | 토큰 만료 |

---

## U-4. PUT `/api/users/me`

**설명**: 닉네임 / 자기소개 수정. 이메일 / 비밀번호는 변경 불가 (별도 엔드포인트 — v1.1).
**인증**: USER

### 요청

```json
{
  "nickname": "TA김씨",
  "bio": "Substance Painter 작업 환영"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| nickname | string? | 2~30자. null이면 미변경 |
| bio | string? | max 500자 |

### 응답 200

`U-3` 과 동일 스키마.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | 길이 위반 |
| 401 | `UNAUTHORIZED` | 토큰 없음 |
| 409 | `USER_NICKNAME_DUPLICATED` | (정책 적용 시) |

---

## U-5. POST `/api/users/me/avatar`

**설명**: 아바타 이미지 업로드. 기존 아바타가 있으면 교체.
**인증**: USER

### 요청

```http
POST /api/users/me/avatar
Content-Type: multipart/form-data
Authorization: Bearer <jwt>

file=@avatar.png
```

| Part | 타입 | 제약 |
|---|---|---|
| file | image/png, image/jpeg | ≤ 1MB, 정사각 권장 (강제 X) |

### 응답 200

```json
{
  "success": true,
  "data": {
    "id": 12,
    "email": "kim@example.com",
    "nickname": "김TA",
    "role": "USER",
    "bio": "...",
    "avatarUrl": "/api/users/12/avatar"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `FILE_EMPTY` | 0바이트 |
| 400 | `FILE_EXTENSION_NOT_ALLOWED` | png/jpg/jpeg 외 |
| 400 | `FILE_TOO_LARGE` | 1MB 초과 |

---

## U-6. GET `/api/users/directory`

**설명**: TA 멤버(USER) 목록. 게시글 수·총 좋아요 통계 포함. DM 상대 검색 및 명예의 전당용.
**인증**: USER

### 요청

```http
GET /api/users/directory?page=0&size=20&sort=totalLikes,desc
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page | int | 0 | |
| size | int | 20 | 최대 50 |
| sort | string | `postCount,desc` | 화이트리스트: `nickname`, `postCount`, `totalLikes` |
| q | string? | - | 닉네임 부분 매치 |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 12,
        "nickname": "김TA",
        "bio": "캐주얼 모델링",
        "avatarUrl": "/api/users/12/avatar",
        "postCount": 7,
        "totalLikes": 23
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 41,
    "totalPages": 3,
    "first": true,
    "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `PAGINATION_SIZE_TOO_LARGE` | size > 50 |
| 400 | `SORT_KEY_NOT_ALLOWED` | sort 키 화이트리스트 외 |
| 401 | `UNAUTHORIZED` | |

---

## U-7. GET `/api/users/search`

**설명**: 닉네임 자동완성용 간단 검색. 최대 10개.
**인증**: USER

### 요청

```http
GET /api/users/search?q=김
```

| Query | 타입 | 비고 |
|---|---|---|
| q | string | 1자 이상. 짧으면 빈 배열 |

### 응답 200

```json
{
  "success": true,
  "data": [
    { "id": 12, "nickname": "김TA", "avatarUrl": "/api/users/12/avatar" },
    { "id": 18, "nickname": "김디자이너", "avatarUrl": null }
  ]
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | q 빈 문자열 |
| 401 | `UNAUTHORIZED` | |

---

## U-8. GET `/api/users/{id}`

**설명**: 특정 유저의 공개 프로필.
**인증**: USER

### 요청

```http
GET /api/users/12
Authorization: Bearer <jwt>
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "id": 12,
    "email": "kim@example.com",
    "nickname": "김TA",
    "role": "USER",
    "bio": "캐주얼 모델링",
    "avatarUrl": "/api/users/12/avatar"
  }
}
```

> 정책: 이메일을 일반 USER에게도 공개할지 합의 필요. 노출 안 한다면 `email` 필드 제거.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `USER_NOT_FOUND` | 미존재 id |

---

## U-9. GET `/api/users/{id}/avatar`

**설명**: 아바타 이미지 바이너리. 정적 캐시 가능.
**인증**: 익명

### 요청

```http
GET /api/users/12/avatar
```

### 응답 200

```
Content-Type: image/png (또는 image/jpeg)
Cache-Control: public, max-age=3600
Content-Length: 12345

<binary>
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 404 | `USER_AVATAR_NOT_FOUND` | 아바타 미설정 또는 파일 없음 |

> 미설정 시 기본 아바타 이미지(static 자원)로 리다이렉트하는 옵션은 v1.1.

---

## U-10. GET `/api/admin/users`

**설명**: 어드민용 유저 목록. 가입 통계 포함.
**인증**: ADMIN

### 요청

```http
GET /api/admin/users?page=0&size=20&role=USER
Authorization: Bearer <admin-jwt>
```

| Query | 타입 | 비고 |
|---|---|---|
| page, size | int | 표준 |
| role | string? | `USER` / `ADMIN` / `SUPER_ADMIN` 필터 |
| q | string? | 닉네임 또는 이메일 부분 매치 |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 12,
        "email": "kim@example.com",
        "nickname": "김TA",
        "role": "USER",
        "bio": "...",
        "postCount": 7,
        "totalLikes": 23
      }
    ],
    "page": 0, "size": 20, "totalElements": 41, "totalPages": 3,
    "first": true, "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | USER 권한이 호출 |

---

## U-11. PATCH `/api/admin/users/{id}/role`

**설명**: 유저 권한 변경. **SUPER_ADMIN 전용.**
**인증**: SUPER_ADMIN

### 요청

```json
{ "role": "ADMIN" }
```

| 필드 | 타입 | 허용값 |
|---|---|---|
| role | string | `USER`, `ADMIN`, `SUPER_ADMIN` |

### 응답 200

`U-10` 의 단일 항목과 동일.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | role 누락 / 알 수 없는 값 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | ADMIN(SUPER_ADMIN 아님) 호출 |
| 403 | `FORBIDDEN_SELF_ROLE_CHANGE` | 본인 role 변경 시도 |
| 404 | `USER_NOT_FOUND` | |

---

## U-12. DELETE `/api/admin/users/{id}`

**설명**: 유저 삭제. SUPER_ADMIN 전용. 본 MVP는 hard delete.
**인증**: SUPER_ADMIN

### 요청

```http
DELETE /api/admin/users/12
Authorization: Bearer <super-admin-jwt>
```

### 응답 200

```json
{ "success": true, "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | SUPER_ADMIN 아님 |
| 403 | `FORBIDDEN_SELF_DELETE` | 본인 삭제 시도 |
| 404 | `USER_NOT_FOUND` | |
| 409 | `USER_HAS_DEPENDENCIES` | (정책 합의 시) 게시글/요청이 남아 있으면 거부 |

> 삭제 시 작성 글의 author는 어떻게 처리할지 합의: (a) 익명 처리, (b) 글도 함께 삭제, (c) 거부. 본 MVP는 **(a) 익명 처리** 권장.
