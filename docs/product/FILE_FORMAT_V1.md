# Trip Plan 공유 파일 형식 v1

문서 버전: 1.1
기준일: 2026-11-09

## 목적

여행 계획을 서버나 계정 없이 하나의 JSON 파일로 전달하기 위한 형식이다. 파일 이름은 `.tripplan.json`으로 끝나며 UTF-8로 저장한다.

## 호환성 규칙

- 최상위 `formatVersion`의 현재 값은 `1`이다.
- 앱은 알 수 없는 필드를 무시해 같은 버전 안에서 선택 필드가 추가되어도 기존 파일을 최대한 읽는다.
- 지원하지 않는 버전은 임의 변환하지 않고 명확한 오류를 표시한다.
- 로컬 Room ID, 기기 경로, 생성 시각과 내부 상태는 공유하지 않는다.
- 가져올 때 여행과 모든 일정에 새 로컬 ID를 부여한다.
- 같은 파일을 다시 가져오면 기존 여행을 덮어쓰지 않고 별도의 새 여행으로 추가한다.
- 파일 전체를 검증한 뒤 하나의 Room 트랜잭션으로 저장한다.
- v1에는 국가와 대표 통화가 없으므로 새 앱에서 대한민국(`KR`)과 원화(`KRW`) 기본값으로 가져온다.

## JSON 구조

```json
{
  "formatVersion": 1,
  "trip": {
    "title": "제주 여행",
    "destination": "제주",
    "startDate": "2026-11-09",
    "endDate": "2026-11-11",
    "description": "친구와 여행"
  },
  "itineraryItems": [
    {
      "date": "2026-11-09",
      "title": "성산일출봉",
      "place": "제주",
      "startTime": "09:00",
      "endTime": "11:00",
      "note": "입장권 확인",
      "order": 0
    }
  ]
}
```

## 필드와 검증

| 경로 | 형식 | 규칙 |
|---|---|---|
| `formatVersion` | 정수 | `1`만 지원 |
| `trip.title` | 문자열 | 공백만 있는 값 불가 |
| `trip.destination` | 문자열 | 빈 값 허용 |
| `trip.startDate`, `trip.endDate` | `YYYY-MM-DD` | 종료일은 시작일보다 빠를 수 없음 |
| `trip.description` | 문자열 | 빈 값 허용 |
| `itineraryItems[].date` | `YYYY-MM-DD` | 여행 기간 안의 날짜 |
| `itineraryItems[].title` | 문자열 | 공백만 있는 값 불가 |
| `itineraryItems[].place`, `note` | 문자열 | 빈 값 허용 |
| `itineraryItems[].startTime`, `endTime` | `HH:mm` 또는 `null` | 둘 다 있으면 종료 시간이 시작 시간보다 빠를 수 없음 |
| `itineraryItems[].order` | 0 이상의 정수 | 같은 날짜 안의 표시 순서 |

## Android 파일 흐름

- 저장: Storage Access Framework의 문서 생성 화면을 사용한다.
- 공유: `FileProvider`의 읽기 전용 URI와 Android 공유 시트를 사용한다.
- 가져오기: 문서 선택 또는 외부 앱의 `ACTION_VIEW`로 파일을 연다.
- 저장 전 미리보기에서 제목, 목적지, 날짜 범위, 일수와 일정 개수를 표시한다.

## 검증 완료 범위

- 내보내기 → FileProvider 공유 URI → 다시 읽기 → 새 여행 가져오기 왕복 후 여행과 일정 구조가 동일함을 확인했다.
- 일정 저장 도중 실패하면 여행과 이미 저장한 일정까지 모두 롤백됨을 확인했다.
- 같은 파일을 두 번 가져오면 서로 다른 로컬 ID의 독립된 여행 두 개가 생성됨을 확인했다.
- 500개 일정, 윤년 날짜와 여행 시작·종료 경계 검증을 통과했다.
- 손상 JSON, 미지원 버전과 여행 범위 밖 일정은 로컬 저장 전에 거부한다.
