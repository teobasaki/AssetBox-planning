# 06. API 명세서 — Request (요청 게시판)

> ⚠️ **2026-05-22 회의 반영분과 충돌 시** [`02_엔드포인트_목록_상태코드분리_수정본.md`](./02_엔드포인트_목록_상태코드분리_수정본.md) **이 진실의 단일 원본.** 이 파일은 회의 반영 핵심 변경만 머리에 박아둠. 본문 상세는 회의록과 합쳐서 읽기.


> 담당 파트: **Post-B**
> 베이스 경로: `/api/requests/**`
> 댓글 엔드포인트는 `07_명세서_Comment.md` 참고.

| # | Method | Path | Auth | 요약 |
|---|---|---|---|---|
| R-1 | POST | `/api/requests` | USER | 요청 작성 |
| R-2 | GET | `/api/requests` | USER | 요청 목록 |
| R-3 | GET | `/api/requests/{id}` | USER | 요청 상세 |
| R-4 | PUT | `/api/requests/{id}` | USER (요청자) | 요청 수정 (REQUESTED 상태만) |
| R-5 | PATCH | `/api/requests/{id}/status` | USER/ADMIN | 상태 전이 |
| R-6 | PATCH | `/api/requests/{id}/assign` | ADMIN | TA 배정 |
| R-7 | PATCH | `/api/requests/{id}/link-post` | USER (assignee) | 결과 게시글 연결 (자동 COMPLETED) |
| R-8 | DELETE | `/api/requests/{id}` | USER (요청자) | 요청 삭제 (soft) |

---

## 상태 전이 다이어그램 (R-5 검증 규칙)

```
REQUESTED ──── (ADMIN) ──▶ IN_REVIEW ──── (ADMIN/assign) ──▶ IN_PROGRESS ──(assignee/linkPost)──▶ COMPLETED
    │                          │                                 │
    └────── (ADMIN) ───────────┴────────── (ADMIN) ──────────────┴─────▶ REJECTED
```

| from | to | 허용 actor |
|---|---|---|
| REQUESTED → IN_REVIEW | ADMIN, SUPER_ADMIN |
| REQUESTED → IN_PROGRESS | ADMIN (assign과 함께, R-6 사용 권장) |
| REQUESTED → REJECTED | ADMIN |
| IN_REVIEW → IN_PROGRESS | ADMIN, assignee |
| IN_REVIEW → REJECTED | ADMIN |
| IN_REVIEW → REQUESTED | ADMIN (복원) |
| IN_PROGRESS → COMPLETED | assignee (단, `link-post` 와 함께 R-7 사용 권장) |
| IN_PROGRESS → REJECTED | ADMIN |
| 그 외 전이 | 400 `REQUEST_STATUS_TRANSITION_INVALID` |

---

## R-1. POST `/api/requests`

**설명**: 새 에셋 의뢰. status=REQUESTED, teamId 스냅샷.
**인증**: USER

### 요청

```json
{
  "title": "캐주얼 의자가 필요해요",
  "content": "로우폴리. 카페 씬에 둘 작은 1인용 의자.",
  "assetType": "Furniture",
  "preferredStyle": "Stylized / Casual",
  "engine": "Blender",
  "deadline": "2026-06-15"
}
```

| 필드 | 타입 | 제약 |
|---|---|---|
| title | string | NotBlank, max 100 |
| content | string | NotBlank |
| assetType | string? | max 60 |
| preferredStyle | string? | max 60 |
| engine | string? | max 60 |
| deadline | date? | 오늘 이후 권장 (검증은 정책 합의) |

### 응답 201

```json
{
  "success": true,
  "data": {
    "id": 11,
    "title": "캐주얼 의자가 필요해요",
    "content": "로우폴리. 카페 씬에 둘 작은 1인용 의자.",
    "assetType": "Furniture",
    "preferredStyle": "Stylized / Casual",
    "engine": "Blender",
    "deadline": "2026-06-15",
    "status": "REQUESTED",
    "requesterId": 12,
    "requesterNickname": "김TA",
    "assigneeId": null,
    "assigneeNickname": null,
    "linkedPostId": null,
    "createdAt": "2026-05-21T14:30:15",
    "updatedAt": "2026-05-21T14:30:15"
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | title/content 누락, 길이 위반 |
| 400 | `REQUEST_DEADLINE_PAST` | deadline이 과거 (정책 합의 시) |
| 401 | `UNAUTHORIZED` | |

---

## R-2. GET `/api/requests`

**설명**: 요청 목록.
**인증**: USER

### 요청

```http
GET /api/requests?page=0&size=20&status=IN_PROGRESS&assigneeId=18
```

| Query | 타입 | 기본 | 비고 |
|---|---|---|---|
| page, size | int | 0/20 | |
| sort | string | `createdAt,desc` | 화이트리스트: `createdAt`, `deadline` |
| status | string? | - | `REQUESTED`, `IN_REVIEW`, `IN_PROGRESS`, `COMPLETED`, `REJECTED` |
| assigneeId | long? | - | |
| requesterId | long? | - | |
| teamId | long? | - | |
| q | string? | - | title LIKE |

### 응답 200

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 11,
        "title": "캐주얼 의자가 필요해요",
        "assetType": "Furniture",
        "engine": "Blender",
        "deadline": "2026-06-15",
        "status": "IN_PROGRESS",
        "requesterId": 12,
        "requesterNickname": "김TA",
        "assigneeId": 18,
        "assigneeNickname": "박TA",
        "createdAt": "2026-05-21T14:30:15"
      }
    ],
    "page": 0, "size": 20, "totalElements": 22, "totalPages": 2,
    "first": true, "last": false
  }
}
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `REQUEST_STATUS_UNKNOWN` | status 값이 enum 아님 |
| 400 | `SORT_KEY_NOT_ALLOWED` | |
| 401 | `UNAUTHORIZED` | |

---

## R-3. GET `/api/requests/{id}`

**설명**: 요청 상세.
**인증**: USER

### 요청

```http
GET /api/requests/11
```

### 응답 200

`R-1` 의 응답 스키마와 동일 (`RequestDetailResponse`).

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 404 | `REQUEST_NOT_FOUND` | |
| 410 | `REQUEST_DELETED` | soft delete된 요청 (어드민만 조회 가능) |

---

## R-4. PUT `/api/requests/{id}`

**설명**: 요청 수정. **status=REQUESTED 상태일 때만** 수정 가능. 요청자 본인만.
**인증**: USER (요청자)

### 요청

```json
{
  "title": "캐주얼 의자가 필요해요 (추가 정보)",
  "content": "...",
  "assetType": "Furniture",
  "preferredStyle": "Stylized",
  "engine": "Blender",
  "deadline": "2026-06-20"
}
```

### 응답 200

`R-1` 응답.

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | |
| 400 | `REQUEST_NOT_EDITABLE` | status가 REQUESTED 아님 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 요청자 아님 |
| 404 | `REQUEST_NOT_FOUND` | |

---

## R-5. PATCH `/api/requests/{id}/status`

**설명**: 상태 전이. 위 매트릭스에 따른 actor 검증.
**인증**: USER (특정 전이만) / ADMIN

### 요청

```json
{ "status": "IN_REVIEW" }
```

### 응답 200

`R-1` 응답 (status 변경된 상태).

### 부수효과

- 상태 변경 성공 시 **요청자에게 시스템 DM 1통** 발송 (`MessageService.send(systemUserId, requesterId, "...")`)
- DM 송신이 실패하면 상태 변경도 롤백 (학습 효과상 같은 트랜잭션)

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `REQUEST_STATUS_UNKNOWN` | status 값 enum 아님 |
| 400 | `REQUEST_STATUS_TRANSITION_INVALID` | 표 외 전이 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 권한 없는 actor (예: 일반 USER가 IN_REVIEW → IN_PROGRESS 시도) |
| 404 | `REQUEST_NOT_FOUND` | |
| 409 | `REQUEST_COMPLETED_LOCKED` | 이미 COMPLETED/REJECTED 인데 변경 시도 |

---

## R-6. PATCH `/api/requests/{id}/assign`

**설명**: TA 배정. ADMIN 전용. 배정 시 자동으로 IN_PROGRESS (REQUESTED/IN_REVIEW 이었던 경우).
**인증**: ADMIN

### 요청

```json
{ "assigneeId": 18 }
```

### 응답 200

`R-1` 응답.

### 부수효과

- 요청자에게 DM: "요청이 박TA에게 배정되었습니다."
- assignee에게 DM: "[요청 #11] 캐주얼 의자가 필요해요 — 작업 부탁드립니다." (선택)

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | assigneeId 누락 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | USER 호출 |
| 404 | `REQUEST_NOT_FOUND` | |
| 404 | `USER_NOT_FOUND` | assigneeId 미존재 |
| 409 | `REQUEST_COMPLETED_LOCKED` | 이미 COMPLETED/REJECTED |

---

## R-7. PATCH `/api/requests/{id}/link-post`

**설명**: 결과물(게시글) 연결 + 자동 COMPLETED. assignee만 호출.
**인증**: USER (assignee)

### 요청

```json
{ "postId": 42 }
```

### 응답 200

```json
{
  "success": true,
  "data": {
    "id": 11,
    "title": "캐주얼 의자가 필요해요",
    "...": "...",
    "status": "COMPLETED",
    "linkedPostId": 42,
    "updatedAt": "2026-05-23T11:00:00"
  }
}
```

### 부수효과

- 요청자에게 DM: "요청이 완료되었습니다 → /posts/42"

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 400 | `VALIDATION_FAILED` | postId 누락 |
| 400 | `REQUEST_LINK_NOT_IN_PROGRESS` | status가 IN_PROGRESS 아님 |
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | assignee 아님 |
| 404 | `REQUEST_NOT_FOUND` | |
| 404 | `POST_NOT_FOUND` | |
| 409 | `REQUEST_ALREADY_COMPLETED` | 이미 COMPLETED |

---

## R-8. DELETE `/api/requests/{id}`

**설명**: 요청 삭제. soft. 요청자 본인만.
**인증**: USER (요청자)

### 요청

```http
DELETE /api/requests/11
```

### 응답 200

```json
{ "success": true, "data": null }
```

### 에러

| HTTP | code | 발생 조건 |
|---|---|---|
| 401 | `UNAUTHORIZED` | |
| 403 | `FORBIDDEN` | 요청자 아님 |
| 404 | `REQUEST_NOT_FOUND` | |
| 409 | `REQUEST_IN_PROGRESS_LOCKED` | IN_PROGRESS / COMPLETED 인 경우 삭제 거부 |
