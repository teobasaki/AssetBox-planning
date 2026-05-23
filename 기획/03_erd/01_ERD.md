# 01. ERD (Entity Relationship Diagram)

> ⚠️ **2026-05-22 회의 반영** — 본 문서는 회의 결과를 반영한 패치본. 회의록 단일 진실 원본은 [`04_api/02_엔드포인트_목록_상태코드분리_수정본.md`](../04_api/02_엔드포인트_목록_상태코드분리_수정본.md).

## 회의 반영 ERD 변경 (핵심 — 본 박스 기준이 최신)

| 엔티티 | 변경 |
|---|---|
| **User** | 컬럼 추가: `real_name VARCHAR(30) NOT NULL`, `major VARCHAR(50) NULL` (보완 페이지에서 입력) |
| **User** | `password VARCHAR(255) NULL` 로 변경 (OAuth 가입자는 비밀번호 없음). 컬럼 추가: `oauth_provider VARCHAR(20) NULL`, `oauth_subject VARCHAR(100) NULL` |
| **Post** | 컬럼 추가: `linked_request_id BIGINT NULL` — 요청 게시판과 1:1 연결. 게시글 작성 시 박으면 요청 자동 COMPLETED |
| **Post** | 인덱스 추가: `idx_posts_linked_request (linked_request_id)` |
| **AssetFile (File 통합 도메인)** | 컬럼 추가: `purpose VARCHAR(30) NOT NULL` — `ASSET / POST_THUMBNAIL / USER_AVATAR / REQUEST_REFERENCE` |
| **AssetFile** | 컬럼 추가: `owner_id BIGINT NOT NULL` (업로더), `deleted BOOLEAN NOT NULL DEFAULT FALSE` (soft delete) |
| **RequestPost** | 구조 변경 없음. `linked_post_id` 는 게시글 작성에서 역방향으로 자동 세팅됨 |
| **DownloadLog** | **MVP 테이블 생성하지 않음** — v1.1 로 보류 |
| **Feedback** | `user_id BIGINT NOT NULL` 로 NULL 불허 (익명 차단) |

> 본 박스 기준이 최신. 아래 본문 옛 ERD 와 충돌 시 본 박스 우선.


> 비유: **연락처 앱에서 사람과 사람을 잇는 화살표.** 화살표가 어디로 향하는지(소유 관계), 점선인지(선택), 1:N인지 N:M인지가 곧 ERD다.
>
> 이 문서는 **현재 코드(2026-05-20 기준)** 와 1:1로 맞춰 정리한 최종본입니다. 변경 시 PR 본문에 "[ERD]" 태그 + 본 문서 동시 수정.

---

## 한 장 다이어그램

```
┌────────────┐         ┌─────────────┐
│   User     │ 1     N │   Post      │ N    M  ┌────────┐
│            │─────────┤             │─────────│  Tag   │
│ id PK      │ author  │ id PK       │ post_   └────────┘
│ email UK   │         │ author_id FK│ tags
│ password   │         │ category_id │
│ nickname   │         │ team_id     │  (post_tags 조인 테이블)
│ team_id    │         │ ...         │
│ bio        │         └──┬──────────┘
│ role(enum) │            │ 1
│ avatar_path│            │
│            │            │ N
│            │         ┌──┴──────────┐         ┌────────────┐
│            │ 1     N │ AssetFile   │         │ Category   │
│            │         │ id PK       │         │ id PK      │
│            │         │ post_id     │         │ parent_id  │◀─┐
│            │         │ stored_path │         │ depth(1~3) │  │ 셀프 참조
│            │         │ thumbnail   │         │ sort_order │──┘
│            │         └─────────────┘         └────────────┘
│            │
│            │         ┌─────────────┐
│            │ 1     N │ DownloadLog │   (file_id, post_id, user_id 모두 FK 안 잡음 — 운영 분리 준비)
│            │─────────┤ id PK       │
│            │         │ user_id?    │
│            │         │ post_id     │
│            │         │ file_id     │
│            │         │ ip / agent  │
│            │         └─────────────┘
│            │
│            │         ┌─────────────┐
│            │ 1     N │  PostLike   │  (user_id, post_id) UNIQUE
│            │─────────┤             │
│            │         └─────────────┘
│            │
│            │         ┌─────────────┐         ┌─────────────┐
│            │ 1     N │  Comment    │ N     1 │   Post      │
│            │─────────┤ post_id FK  │─────────┤ (위 박스)   │
│            │ author  │ parent_id FK│◀─┐      └─────────────┘
│            │         │ deleted     │  │ 대댓글 셀프 참조
│            │         └─────────────┘──┘
│            │
│            │         ┌─────────────┐
│            │ 1     N │ RequestPost │ N    1  ┌────────────┐
│            │─────────┤ requester_id│         │ Post       │ (linked_post_id, nullable)
│            │ assignee│ assignee_id │─────────┤            │
│            │ (NULL)  │ status(enum)│         └────────────┘
│            │         │ team_id     │
│            │         │ deadline    │
│            │         └─────┬───────┘
│            │               │ 1
│            │               │
│            │               │ N
│            │         ┌─────┴───────┐
│            │         │RequestComm. │ (대댓글 parent_id, 셀프 참조)
│            │         └─────────────┘
│            │
│            │         ┌─────────────┐
│            │ 1     N │  Message    │ (sender_id, receiver_id 모두 User)
│            │─────────┤ sender_id   │
│            │         │ receiver_id │
│            │         │ content     │
│            │         │ read        │
│            │         └─────────────┘
└────────────┘

┌──────────────┐
│  Feedback    │ (userId nullable — 익명 허용, FK 안 잡음)
│  id PK       │
│  title       │
│  content     │
│  status(enum)│
└──────────────┘
```

---

## 테이블 상세

### users

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | |
| email | VARCHAR(50) | NOT NULL, UNIQUE | 로그인 ID |
| password | VARCHAR(255) | NOT NULL | BCrypt 해시 |
| nickname | VARCHAR(30) | NOT NULL | |
| team_id | BIGINT | NULL | 통합 전까지 NULL 허용 |
| bio | VARCHAR(500) | NULL | |
| avatar_path | VARCHAR(255) | NULL | 로컬 파일 → S3 URL |
| role | VARCHAR(20) | NOT NULL | USER / ADMIN / SUPER_ADMIN |
| created_at | TIMESTAMP | NOT NULL | BaseEntity |
| updated_at | TIMESTAMP | NOT NULL | BaseEntity |

### posts

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | |
| title | VARCHAR(100) | NOT NULL | |
| content | TEXT | NOT NULL | |
| author_id | BIGINT | FK → users.id, NOT NULL | |
| category_id | BIGINT | FK → categories.id, NULL | 보통 소분류 |
| team_id | BIGINT | NULL | 작성 시점 author.team_id 스냅샷 |
| view_count | BIGINT | NOT NULL DEFAULT 0 | |
| like_count | BIGINT | NOT NULL DEFAULT 0 | |
| deleted | BOOLEAN | NOT NULL DEFAULT FALSE | soft delete |
| created_at / updated_at | TIMESTAMP | | |

**인덱스**: `(author_id)`, `(category_id)`, `(team_id)`

### post_tags (조인 테이블)
| post_id | tag_id | PK 복합 |

### post_likes
| id PK | user_id FK | post_id FK | UNIQUE(user_id, post_id) |

### asset_files

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | |
| post_id | BIGINT | NOT NULL | (FK 무관 정책 — 분리 준비) |
| original_name | VARCHAR(200) | NOT NULL | |
| stored_path | VARCHAR(500) | NOT NULL | 로컬 또는 S3 key |
| extension | VARCHAR(30) | NOT NULL | |
| size_bytes | BIGINT | NOT NULL | |
| thumbnail | BOOLEAN | NOT NULL | true면 대표 이미지 |
| created_at / updated_at | | | |

**왜 post_id에 FK를 안 거나?** → 향후 파일 도메인을 분리 서비스로 떼어내기 쉽도록 외래키 제약 대신 ID 참조만. 정합성은 서비스 레이어에서 보장.

### download_logs

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id PK | | |
| user_id | NULL 허용 | 비로그인도 다운로드 가능 |
| post_id, file_id | NOT NULL | FK X (분리 준비) |
| original_name | VARCHAR(200) | 사후 분석용 스냅샷 |
| ip_address | VARCHAR(50) | |
| user_agent | VARCHAR(512) | truncate |
| created_at | | 다운로드 시점 |

### categories

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| id PK | | | |
| name | VARCHAR(50) | NOT NULL | |
| parent_id | BIGINT | FK → categories.id, NULL | 셀프 참조 |
| depth | INT | NOT NULL | 1=대 / 2=중 / 3=소 |
| sort_order | INT | NOT NULL | 같은 depth 내 정렬 |

### tags
| id PK | name VARCHAR(30) UNIQUE NOT NULL |

### comments

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id PK | | |
| post_id | FK NOT NULL | |
| author_id | FK NOT NULL | |
| parent_id | FK NULL | 대댓글 셀프 참조 |
| content | VARCHAR(2000) NOT NULL | |
| deleted | BOOLEAN | soft delete |

### request_posts

| 컬럼 | 타입 | 비고 |
|---|---|---|
| id PK | | |
| title | VARCHAR(100) | |
| content | TEXT | |
| asset_type, preferred_style, engine | VARCHAR(60) | 요청 메타 |
| deadline | DATE | |
| status | VARCHAR(20) | REQUESTED/IN_REVIEW/IN_PROGRESS/COMPLETED/REJECTED |
| requester_id | FK → users.id NOT NULL | |
| assignee_id | FK → users.id NULL | TA |
| linked_post_id | FK → posts.id NULL | 완료 시 결과물 연결 |
| team_id | BIGINT | 작성자 스냅샷 |
| deleted | BOOLEAN | |

**상태 전이 (강제)**:
```
REQUESTED ─▶ IN_REVIEW ─▶ IN_PROGRESS ─▶ COMPLETED
        │                              │
        └──────────▶ REJECTED ◀────────┘
```
역방향 전이는 ADMIN만, 일반 USER는 단방향만 (RequestStatusService에서 검증).

### request_comments
구조는 `comments` 와 동일. (단, post_id 자리에 request_id)

### messages
| id PK | sender_id FK | receiver_id FK | content TEXT | read BOOLEAN | created_at |
인덱스: `(sender_id)`, `(receiver_id)`.

### feedbacks
| id PK | title | content | user_id NULL | user_nickname VARCHAR | status (NEW/READ) |

---

## 설계 결정 사유 정리

| 결정 | 사유 |
|---|---|
| **카테고리 셀프 참조 트리** | 화이트보드의 "단계별 드릴다운"과 1:1. 트리를 한 번에 안 내려준다. |
| **`Post.teamId` 스냅샷** | 사용자가 팀 옮긴 뒤에도 과거 글의 소속 추적. 회의록 결정. |
| **`AssetFile`/`DownloadLog`에 FK 미설정** | 파일 도메인 분리 준비. 정합성은 서비스 레이어에서. |
| **soft delete (`deleted` 플래그)** | 댓글/포스트는 신고/분쟁 흔적 보존. 어드민이 복구 가능. |
| **Tag 별도 테이블 + M:N** | 한 글에 여러 태그, 한 태그에 여러 글. findOrCreate 패턴으로 무한 증식 방지. |
| **request_post.linked_post_id (Post FK)** | 완료된 요청은 결과 게시글과 연결. 회의록 결정. |

---

## "한 사이클" 데이터 흐름 예시

> 시나리오: TA-김씨가 "캐주얼 의자 모델 필요" 요청 → ADMIN이 TA-박씨에 배정 → 박씨가 의자 만들어 게시글 작성 → 요청 완료 처리.

```
1. INSERT request_posts (requester=김씨, status='REQUESTED', team_id=김씨.team_id)
2. UPDATE request_posts SET assignee_id=박씨, status='IN_PROGRESS' WHERE id=1
   └─ MessageService.send(system→김씨, "요청이 박씨에게 배정되었습니다")
3. INSERT posts (author=박씨, ...), INSERT asset_files (postId=새포스트.id)
4. UPDATE request_posts SET linked_post_id=새포스트.id, status='COMPLETED' WHERE id=1
   └─ MessageService.send(system→김씨, "요청이 완료되었습니다 → /posts/{id}")
```

요청과 결과 게시글은 서로 다른 테이블이지만 `linked_post_id` 로 1:1 연결.

---

## 마이그레이션 정책

- **M0 (5/22)**: 위 스키마를 v1로 동결. 그 후 변경은 모두 PR + Infra 리뷰 필수.
- **dev 프로파일**: `ddl-auto: create-drop` 로 매번 재생성. 시드는 `CategorySeeder` + `AdminBootstrapRunner` 가 담당.
- **prod 전환 시 (M2 후반, 6/8)**: `validate` 로 잠금. 이후 변경은 v1.1 백로그로.
- **Flyway / Liquibase 도입은 v1.1.** 이번 학기엔 도입 비용이 학습 효과보다 큼.
