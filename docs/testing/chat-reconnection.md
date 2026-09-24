\# 채팅 연결 종료 처리 수정 및 검증



\## 검증 목적



서버가 WebSocket 연결 종료를 요청했을 때,

채팅 클라이언트가 재연결하고 기존 채팅방을 재구독하는지 확인했다.



\## 검증 환경



\- 검증일: 2026-09-24

\- 대상: StompChatClient.kt

\- 실행 환경: Windows JVM

\- 통신 환경: 로컬 MockWebServer와 실제 OkHttp WebSocket 클라이언트

\- 방식: 동일한 테스트에서 onClosing 처리의 활성화 여부를 바꿔 비교



\## 문제 현상



서버가 종료 코드 1001의 Close 프레임을 보내면

4초 동안 재연결과 재구독이 진행되지 않았다.

클라이언트의 isConnected 값도 true로 남아 있었다.



반면 강제 소켓 단절을 통한 통신 오류 상황에서는 정상적으로 복구됐다.



\## 원인



기존 코드에는 onFailure와 onClosed 처리가 있었지만,

서버의 종료 요청에 응답하는 onClosing 처리가 없었다.



onClosing은 상대의 종료 요청을 받았을 때 호출되고,

onClosed는 양쪽의 종료 절차가 완료된 뒤 호출된다.



종료 요청에 응답하지 않아 종료 완료 후의 재연결 처리로 이어지지 않았다.



\## 수정 내용



WebSocketListener에 다음 처리를 추가했다.



```kotlin

override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {

&#x20;   Log.d(TAG, "WebSocket 종료 요청 수신: code=$code, reason=$reason")

&#x20;   \_connected.value = false

&#x20;   webSocket.close(code, reason)

}

```



종료 요청을 받으면 연결 상태를 false로 바꾸고 close로 응답한다.

이후 기존 onClosed에서 연결을 정리하고 재연결을 예약한다.



재연결을 중복 예약하지 않도록 onClosing에서는 별도로 예약하지 않았다.



\## 검증 방법과 결과



onClosing을 주석 처리한 상태에서 실패를 재현한 뒤,

주석을 해제하고 테스트 코드와 성공 조건을 바꾸지 않고 다시 실행했다.



| 시나리오 | 검증 내용 | 수정 전 | 수정 후 |

|---|---|---|---|

| 강제 단절 및 복구 | 소켓 강제 단절을 3회 반복하고 재구독 및 새 메시지 수신 확인 | 통과 | 통과 |

| 접속 실패 지속 | HTTP 503 응답 조건에서 1, 2, 4, 8, 16초 대기 후 최대 5회 재시도 확인 | 통과 | 통과 |

| 구독자 수 유지 | 같은 방의 구독자가 2명일 때 중복 구독 방지와 마지막 구독자의 해제 처리 확인 | 통과 | 통과 |

| 명시적 종료 | 재시도 대기 중 disconnect 호출 후 예약 취소 확인 | 통과 | 통과 |

| 서버 종료 요청 | Close 프레임 수신 후 재연결 및 재구독 확인 | 실패 | 통과 |



\### 수정 전



```text

TEST\_FAIL server\_close\_frame\_reconnects

No recovery within 4000ms: requests=1, isConnected=true

SUMMARY passed=4 total=5

```



\### 수정 후



```text

TEST\_PASS server\_close\_frame\_reconnects

server Close frame followed by new connection and SUBSCRIBE

SUMMARY passed=5 total=5

```



수정 후에는 서버 종료 요청 수신, 종료 완료, 재연결 예약,

STOMP 연결 완료, 채팅방 재구독 순서로 진행됐다.



\## 관찰 범위



접속 실패 지속 테스트에서는 최초 접속과 재시도 5회를 합쳐

총 6회의 접속 요청 및 중단 로그를 확인했다.

이후 1.5초 동안 추가 요청이 없음을 확인했다.



명시적 종료 테스트에서는 종료 후 1.8초 동안 추가 접속이 없음을 확인했다.



강제 단절은 실제 클라이언트 소켓의 cancel을 호출해 재현했다.

복구 후 보낸 테스트 메시지 3개를 각각 한 번 수신했다.



\## 검증하지 않은 범위



\- 실제 휴대폰의 Wi-Fi 및 모바일 데이터 전환

\- 운영 서버의 인증과 DB 연동

\- 앱 화면의 메시지 표시

\- 단절 중 발생한 메시지의 전체 복원

\- 운영 환경의 장애 감소율



이번 결과는 로컬 테스트 환경에서 종료 처리 수정 전후를 비교한 결과다.



\## 배운 점



통신 오류, 종료 요청, 종료 완료는 서로 다른 처리 경로다.

재연결 로직이 있어도 해당 경로까지 진행되지 않으면 복구되지 않는다.



콜백의 호출 조건과 순서를 확인하고,

동일한 조건에서 수정 전후를 비교하는 과정이 필요하다는 점을 배웠다.

