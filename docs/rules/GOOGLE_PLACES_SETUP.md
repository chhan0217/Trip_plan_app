# Google Places 설정 및 보안

기준일: 2026-11-19

## 적용 범위

- Places SDK for Android와 Places API (New)를 사용한다.
- 여행 국가 코드로 검색 범위를 제한하고 도시 자동완성 결과만 요청한다.
- API 키, 네트워크 또는 할당량 문제가 있어도 로컬 추천 도시와 직접 입력은 계속 동작한다.
- 현재 Kotlin 2.0 및 Room 컴파일러와의 호환성을 위해 Places SDK `4.4.1`을 고정한다.

## 로컬 키 설정

저장소 루트에 Git에서 제외되는 `secrets.properties` 파일을 만들고 다음 값을 넣는다.

```properties
PLACES_API_KEY=발급받은_API_키
```

실제 키는 소스 코드, `local.defaults.properties`, 문서, 이슈 또는 채팅에 기록하지 않는다. 키가 없을 때는 `local.defaults.properties`의 `DEFAULT_API_KEY`가 사용되며 Google Places 호출은 비활성화된다.

## Google Cloud 제한

1. 결제가 연결된 전용 Google Cloud 프로젝트를 사용한다.
2. `Places API (New)`만 활성화한다.
3. API 키의 애플리케이션 제한을 `Android 앱`으로 설정한다.
4. 개발 앱을 다음 값으로 등록한다.
   - 패키지명: `com.example.tripplan`
   - Debug SHA-1: `68:D3:98:C7:8F:30:8F:1B:E5:78:A3:DA:C2:59:43:52:44:FD:0A:52`
5. API 제한은 `Places API (New)`만 선택한다.
6. Places API 일일 요청 할당량을 우선 `250`으로 낮춘다.
7. 결제 예산 알림을 `$1`, `$5`, `$10`에 설정한다.

예산 알림은 사용을 중단하지 않는다. 실제 요청 차단은 API 일일 할당량으로 처리한다.

## 배포 키

Debug SHA-1은 현재 개발 컴퓨터의 Debug 인증서에만 유효하다. 공개 배포 시에는 다음 중 사용하는 서명의 SHA-1을 별도로 등록한다.

- 로컬 Release 서명 인증서
- Google Play App Signing의 앱 서명 인증서

개발용과 배포용 API 키는 분리하는 것을 권장한다. 배포용 키에는 Debug SHA-1을 넣지 않고, 개발용 키에는 배포 SHA-1을 넣지 않는다.

현재 인증서 지문은 다음 명령으로 다시 확인한다.

```shell
./gradlew signingReport
```

## 운영 원칙

- 입력 두 글자부터 검색하고 입력 변경 시 기존 검색 작업을 취소한다.
- 국가 코드로 결과를 제한하고 도시 타입만 요청한다.
- API 오류는 저장 및 여행 생성 실패로 전파하지 않는다.
- 제한에 도달하면 로컬 추천 도시와 직접 입력으로 대체한다.
- 키가 노출된 것으로 의심되면 기존 키를 즉시 폐기하고 새 제한 키를 발급한다.
