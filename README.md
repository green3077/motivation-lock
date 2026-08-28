# 동기부여 (motivation-lock)

화면을 켤 때 잠금화면 위에 목표 사진(왼쪽)과 목표 문구(오른쪽, 손글씨 타이핑 애니메이션)를 보여주는 안드로이드 네이티브 앱입니다. 지금은 큰 틀(스켈레톤)만 잡은 단계입니다.

## 동작 방식

1. `MainActivity`에서 목표 사진 1장 + 목표 문구 1개를 설정합니다.
2. "화면 켤 때 잠금화면에 자동으로 뜨게 하기"를 켜면 `LockScreenTriggerService`(포그라운드 서비스)가 실행되면서 화면 켜짐(`ACTION_SCREEN_ON`)을 감지합니다.
3. 화면이 켜지고 기기가 잠긴 상태면, 진짜 시스템 잠금화면(PIN/패턴/지문)보다 먼저 `LockScreenActivity`가 뜹니다(`setShowWhenLocked` + `setTurnScreenOn`). 화면을 켤 때마다 손글씨 애니메이션이 처음부터 다시 재생됩니다.
4. 화면 아무 곳이나 탭하면 닫히고, 원래 있던 진짜 잠금화면(또는 잠금이 없으면 홈 화면)으로 이어집니다.

## 파일 구조

| 파일 | 역할 |
|---|---|
| `MainActivity.kt` | 목표 사진/문구 설정 화면, 잠금화면 미리보기, 자동 실행 스위치 |
| `LockScreenActivity.kt` | 잠금화면 위에 뜨는 실제 화면 (좌: 사진, 우: 손글씨 애니메이션) |
| `LockScreenTriggerService.kt` | 화면 켜짐을 감지해 `LockScreenActivity`를 띄우는 포그라운드 서비스 |
| `BootReceiver.kt` | 재부팅 후 자동 실행이 켜져 있었다면 서비스 재시작 |
| `GoalRepository.kt` | 목표 사진/문구 저장·조회 (사진은 `filesDir`로 복사해서 보관) |
| `HandwritingTextView.kt` | 글자를 한 글자씩 드러내는 타이핑 애니메이션 커스텀 뷰 |

## 알려진 한계

- 일부 제조사(삼성/샤오미 등)의 강한 절전 기능은 배터리 최적화 예외를 줘도 백그라운드 서비스를 종료시킬 수 있습니다 - 100% 항상 뜨는 걸 보장하지는 못합니다. 안 뜨면 기기 설정에서 "자동 실행/보호된 앱" 허용이 추가로 필요할 수 있습니다.
- 손글씨 표현은 v1에서는 나눔손글씨 펜 폰트 + 한 글자씩 나타나는 타이핑 효과입니다. 실제 붓/펜 획이 곡선을 따라 그어지는 진짜 스트로크 애니메이션(글자마다 벡터 경로가 필요)은 다음 단계 개선 과제입니다.
- 목표는 지금 1개만 지원합니다. 여러 개(랜덤/순서대로 표시)로 확장하려면 `GoalRepository`를 리스트 기반으로 바꾸면 됩니다.

## 빌드

```
./gradlew assembleDebug
```

이 PC에서는 JDK 경로를 따로 지정할 필요 없이 바로 빌드됩니다(다른 프로젝트인 sobang1004처럼 `gradle.properties`에 특정 PC의 JDK 경로를 박아두지 않았습니다).

release 서명이 필요해지면 `keystore.properties.example`을 참고해 `keystore.properties`를 만드세요.
