# AGENTS.md

## Product Direction

이 앱은 여행 전에 여행 일정을 만들고, 친구와 공유해서 같은 계획을 함께 확인하는 Android 앱이다.

핵심 전제는 다음과 같다.

- 기본 동작은 로컬 기반이다.
- 사용자의 여행 계획 데이터는 우선 기기 안에 저장한다.
- 친구와의 공유는 서버 동기화가 아니라 파일 공유를 기본 방식으로 한다.
- 공유받은 친구는 같은 파일을 열어 동일한 여행 계획을 볼 수 있어야 한다.
- 네트워크, 계정, 로그인, 클라우드 동기화는 기본 요구사항이 아니다.

## Product Principles

- 오프라인에서도 핵심 기능이 동작해야 한다.
- 파일 하나로 여행 계획을 내보내고 가져올 수 있어야 한다.
- 공유 파일은 사람이 이해할 수 있고 앱 버전이 바뀌어도 최대한 호환 가능해야 한다.
- 친구가 공유 파일을 열었을 때 원본 작성자와 같은 일정 구조를 확인할 수 있어야 한다.
- 데이터 손실을 막기 위해 가져오기, 덮어쓰기, 병합 같은 동작은 명확하게 처리한다.

## Data And Sharing Rules

- 여행 계획은 로컬 저장소를 기준 상태로 둔다.
- 공유용 파일 포맷은 앱 내부 모델과 분리해서 설계한다.
- 공유 파일에는 여행 제목, 날짜 범위, 일자별 일정, 장소, 메모, 순서 정보가 포함되어야 한다.
- 공유 파일에는 기기 전용 경로, 임시 ID, 민감한 내부 상태를 넣지 않는다.
- 포맷 변경이 필요하면 버전 필드를 두고 이전 파일을 읽을 수 있는 방향을 우선한다.

## Architecture Rules

- UI, 도메인 모델, 저장소, 파일 import/export 책임을 분리한다.
- 파일 공유 로직은 화면 코드에 직접 넣지 않는다.
- 로컬 저장 모델과 공유 파일 DTO를 구분한다.
- 일정 정렬, 날짜 계산, 파일 변환 같은 규칙은 테스트 가능한 순수 로직으로 둔다.
- 외부 서비스 연동은 기본 기능을 해치지 않는 선택 기능으로만 고려한다.

## Android Tech Stack

- 화면 전환과 앱 내 흐름은 Navigation 3를 기준으로 설계한다.
- 로컬 영속 저장소는 RoomDB를 기준으로 한다.
- 전체 구조는 Clean Architecture를 따른다.
- presentation 계층은 MVVM 패턴을 따른다.
- 의존성 주입은 Dagger/Hilt를 기준으로 한다.
- UI 계층, domain 계층, data 계층의 책임을 명확히 분리한다.
- ViewModel은 UI 상태 관리와 use case 호출을 담당하고, 저장소나 파일 시스템 세부 구현에 직접 의존하지 않는다.
- UseCase는 앱의 주요 행동 단위를 표현하며, 화면별 편의를 위해 과도하게 얇은 래퍼로 만들지 않는다.
- Repository interface는 domain 계층에 두고, RoomDB와 파일 import/export 구현은 data 계층에 둔다.
- Room Entity, 공유 파일 DTO, domain model은 서로 분리한다.
- Mapper는 계층 간 변환 책임을 명확히 하기 위해 별도로 둔다.

## MVVM And ViewModel Rules

- Compose Screen은 UI state를 화면에 그리는 책임을 우선한다.
- Compose Screen에서 repository, DAO, 파일 시스템, Android asset을 직접 호출하지 않는다.
- ViewModel은 화면 상태를 `StateFlow` 또는 Compose에서 관찰 가능한 단일 UI state로 노출한다.
- ViewModel은 사용자 입력 이벤트를 함수로 받고, 그 결과로 UI state를 갱신한다.
- ViewModel은 domain use case에 의존하고 data 구현체에 직접 의존하지 않는다.
- ViewModel 안에 Room Entity, 공유 파일 DTO, Google Maps SDK 타입 같은 외부/저장소 세부 타입을 노출하지 않는다.
- 화면에 필요한 값은 presentation 전용 UI model 또는 UI state로 변환해서 사용한다.
- 일회성 이벤트가 필요한 경우 navigation, snackbar, dialog 표시 같은 이벤트를 상태와 분리해서 다룬다.
- 날짜 계산, 지역 검색, 파일 변환 같은 비즈니스 규칙은 ViewModel에 오래 두지 말고 use case나 순수 로직으로 분리한다.
- ViewModel 테스트는 초기 상태, 입력 이벤트 후 상태 변화, use case 성공/실패 결과 처리를 검증한다.

## Package Direction

- `presentation` 또는 `ui`: 화면, Navigation, ViewModel, UI state
- `domain`: domain model, repository interface, use case
- `data`: RoomDB, DAO, repository implementation, file import/export implementation
- `di`: Hilt module과 dependency binding
- 기능 단위 패키지를 만들 때도 Clean Architecture 계층 경계가 흐려지지 않게 한다.

## Testing Rules

- 개발은 기본적으로 UnitTest를 함께 작성하면서 진행한다.
- 새 use case, mapper, repository 구현, 파일 import/export 로직에는 우선적으로 단위 테스트를 둔다.
- 일정 정렬, 날짜 계산, 파일 포맷 변환, Room entity 변환처럼 데이터 정확도에 영향을 주는 로직은 테스트 대상이다.
- ViewModel은 UI 상태 변화와 use case 호출 결과 처리를 검증한다.
- 테스트하기 어려운 Android framework 의존성은 interface로 분리하거나 wrapper를 둔다.
- 단위 테스트에서 실제 파일, 실제 DB, Android context에 직접 의존하는 범위는 최소화한다.
- RoomDB 동작 검증이 필요한 경우에는 in-memory database 기반 테스트를 우선한다.
- 버그를 수정할 때는 가능한 한 재발 방지 테스트를 먼저 추가하거나 함께 추가한다.

## Compose Preview Rules

- Compose로 View 또는 Screen을 만들 때는 Preview를 반드시 함께 추가한다.
- Preview는 기본 상태, 빈 상태, 주요 데이터 상태처럼 화면 판단에 필요한 대표 상태를 포함한다.
- Preview 함수는 실제 앱 실행 로직, ViewModel, Repository, 파일 시스템, DB에 직접 의존하지 않는다.
- Preview에서 필요한 데이터는 샘플 UI state나 샘플 domain model을 사용한다.
- Preview가 많아질 경우 화면 파일 하단에 모으거나 별도 preview 파일로 분리한다.

## UX Rules

- 사용자는 먼저 여행을 만들고, 날짜별 일정을 편집하고, 파일로 공유할 수 있어야 한다.
- 공유받은 사용자는 파일을 열어 여행 계획을 가져올 수 있어야 한다.
- 가져오기 시 기존 계획에 추가할지, 새 계획으로 열지, 덮어쓸지 명확히 보여준다.
- 친구와 실시간 공동 편집을 기대하게 만드는 표현은 피한다.
- 앱의 기본 메시지는 "같은 계획 파일을 함께 본다"는 사용성을 기준으로 한다.

## Non-Goals For Now

- 실시간 동시 편집
- 서버 기반 계정 시스템
- 클라우드 자동 동기화
- Google Maps 연동과 오프라인 지도 다운로드
- 위치 기반 실시간 추적
- 채팅 기능

## Development Notes For Agents

- 새 기능을 구현할 때 이 문서의 로컬 우선, 파일 공유 우선 원칙을 먼저 확인한다.
- Android 구현은 Navigation 3, RoomDB, Clean Architecture, Dagger/Hilt를 기본값으로 둔다.
- 기능 개발과 리팩터링은 관련 UnitTest를 함께 작성하거나 갱신하는 것을 기본으로 한다.
- Compose 화면을 추가하거나 수정할 때는 Preview도 함께 추가하거나 갱신한다.
- 서버, 로그인, 실시간 동기화를 전제로 하는 설계는 사용자가 명시적으로 요청하기 전까지 피한다.
- 공유 파일 호환성에 영향을 주는 변경은 별도 섹션이나 작업 문서에 기록한다.
- 사용자가 추가로 정하는 규칙은 이 파일에 누적한다.
