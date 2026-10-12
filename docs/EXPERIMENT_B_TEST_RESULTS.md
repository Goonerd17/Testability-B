# 실험 B 테스트 결과 — EXP-001 (Phase 5)

- 작성일: 2026-10-12 · Phase 5 · 브랜치 `exp-001-b`
- 대상 코드: `src/main/java/com/testability/lab/billing/` (커밋 `061ce73`, **변경 없음**)
- 입력: `docs/JEV_RISK_ASSESSMENT.md` (Phase 4, **변경 없음**) 및 명세 5종
- 테스트 코드: `src/test/java/com/testability/lab/billing/` (이번 Phase에서 새로 작성)
- 첫 실행 결과 원본: [`docs/experiment-b-runs/first-run/`](experiment-b-runs/first-run/)
- 실험 A의 디렉터리·브랜치·결과는 열람하지 않았고, 이 문서는 실험 A와 비교하지 않는다. PIT는 실행하지 않았다.

---

## 1. 테스트 작성 전략

1. **작성 순서는 Phase 4 환산 순위를 그대로 따랐다.** `calculate`(1위) → `cumulativeUsage` → `monthlyUsage` → `attributionMonth` → `calculateFee` → `Grade.of`(6위) → `startOf` → `Payment` → `rateBasisPoints` → 생성자 → `minimumAmount`. `jev_rank` 보조 순위는 참고만 했고 순서에 쓰지 않았다.
2. **범위는 순위로 자르지 않았다.** 11개 메서드 모두 테스트했다. 등급 경계(`Grade.of`), 요율(`rateBasisPoints`), 반올림(`calculateFee`), 정산 마감 기준(`startOf`)은 Jev 점수와 관계없이 명세 필수 규칙으로 보고 검증했다.
3. **기대값은 구현과 독립적으로 정했다.**
   - 대부분은 `TEST_CASE_MATRIX.md`에 사람이 미리 계산해 둔 값을 리터럴로 옮겼다.
   - 매트릭스에 없는 값(예: `Long.MAX_VALUE × bp`, 0.5 경계 근처 값, 오프셋 환산)은 분수 계산 또는 손 계산으로 구해 테스트 주석에 과정을 남겼다.
   - 프로덕션 코드의 계산식(`BigDecimal … HALF_UP`, `atZone(ZONE)` 등)을 테스트 안에서 다시 실행해 기대값을 만들지 않았다. 시각 입력은 오프셋이 붙은 ISO 문자열을 `OffsetDateTime.parse`로만 바꿨다.
4. **테스트 더블.** Mockito를 쓰지 않았다. 현재 시각은 JDK `Clock.fixed`로 주입했고, 시계 자체의 시간대는 UTC로 두어 구현이 시계의 시간대에 의존하지 않는지도 함께 확인했다.
5. **근거 추적.** 모든 테스트에 JUnit `@Tag`로 근거를 붙였다(§3.1). 태그별 개수는 태그 필터 실행으로 직접 셌다.
6. **OPEN 항목.** 승인 대기 중인 OPEN-01(시간대), OPEN-03(금액 상한), OPEN-04(HALF_UP), OPEN-15(고정 요율)은 `EXPERIMENT_SCOPE.md` §5에 적힌 **실험용 임시 전제**로만 사용했다. 이 전제에 기대값이 의존하는 테스트에는 `open-premise` 태그를 붙여, 전제가 바뀌면 다시 검토할 대상임을 표시했다. 명세가 정하지 않은 부분(예: null 입력의 예외 종류)은 특정 예외 타입을 정답으로 고정하지 않고 "계산하지 않고 예외가 난다"만 확인했다.

## 2. Jev 평가 활용 방식

| 활용한 것 | 어떻게 |
|-----------|--------|
| 환산 순위 (§7 of JEV_RISK_ASSESSMENT) | 메서드별 테스트 작성 순서. 테스트 클래스 Javadoc에 해당 순위·점수·잠정 여부를 적었다 |
| 상위 6개 목록 | `basis-jev` 태그 대상 (M06, M07, M08, M10, M09, M01). 이 메서드들은 직접 호출 단위 테스트와 `calculate` 경유 테스트를 둘 다 작성했다 |
| §8 우선 시나리오 (Claude 작성) | 시나리오 후보 목록으로 사용. 이 절은 Phase 4에서도 Jev 출력이 아니라고 표시돼 있으므로, 여기서도 Jev 추천으로 기록하지 않는다 |
| review / confidence | 바꾸지 않았다. 잠정 점수에 따라 테스트를 빼거나 줄이지 않았다 (§7) |

**Jev가 하지 않은 것.** Jev는 메서드 단위 우선순위만 줬고 개별 테스트 케이스를 추천하지 않았다. 테스트 시나리오와 기대값은 모두 명세(매트릭스)와 Claude의 코드 분석에서 나왔다. 따라서 이 문서에서 "Jev 근거"는 **"Jev 상위 메서드를 겨냥했다"는 뜻**이며, "Jev가 이 케이스를 제안했다"는 뜻이 아니다.

## 3. 테스트 목록과 근거

### 3.1 근거 태그

| 태그 | 의미 |
|------|------|
| `basis-jev` | 1. Jev 위험도 평가 — 환산 상위 6개 메서드를 직접 겨냥 |
| `basis-spec` | 2. 비즈니스 명세의 필수 규칙 — BUSINESS_RULES / TEST_CASE_MATRIX의 Rule·TC 근거 |
| `basis-code` | 3. Claude가 코드 분석 중 발견한 추가 위험 — 매트릭스에 없는 케이스. DisplayName 앞에 `[코드 분석]` 표시 |
| `open-premise` | 기대값이 승인 대기 OPEN 항목(실험용 전제)에 의존 |

JUnit 태그는 클래스·중첩 클래스에서 상속된다. 그래서 `[코드 분석]` 테스트 5개가 바깥 클래스의 `basis-spec`도 함께 갖게 됐다. 집계에서는 **`basis-code`가 있으면 주 근거를 "코드 분석"으로** 본다.

### 3.2 근거별 테스트 수 (실행 단위, 태그 필터 실행으로 확인)

| 태그 필터 | 테스트 수 |
|-----------|---------:|
| `basis-jev` | 158 |
| `basis-spec` | 159 |
| `basis-code` | 33 |
| `open-premise` | 104 |
| 근거 태그 없음 | 0 |
| 전체 | 187 |

주 근거 기준 분류 (합계 187):

| | 주 근거: 명세 | 주 근거: 코드 분석 | 계 |
|---|---:|---:|---:|
| Jev 상위 6개 메서드 대상 | 128 | 30 | 158 |
| 그 밖의 메서드 대상 | 26 | 3 | 29 |
| 계 | 154 | 33 | 187 |

### 3.3 메서드별 테스트

"테스트 수"는 파라미터화 테스트의 각 실행을 1개로 센 JUnit 보고 기준이다.

| 환산 순위 | 메서드 | 테스트 클래스 | 테스트 수 | 근거 | 주요 TC / 내용 |
|---------:|--------|---------------|---------:|------|----------------|
| 1 | M06 `calculate` | `FeeCalculatorCalculateTest` (중첩 6개) | 47 | jev + spec (+ code 7) | TC-GRD-01 ~ 09, TC-AMT-08, TC-PRD-01 ~ 10, TC-FEE-01 ~ 17, TC-STL-01(대응 항목), TC-STL-04/05, TC-TIM-07, 등급 하락·상승, 등급 × 당월 기준액 결합 |
| 2 | M07 `cumulativeUsage` | `FeeCalculatorCumulativeUsageTest` | 18 | jev + spec (+ code 4) | TC-PRD-01 ~ 06 경계, TC-PRD-05, TC-GRD-09, TC-AMT-08, BR-PRF-004 |
| 3 | M08 `monthlyUsage` | `FeeCalculatorMonthlyUsageTest` | 14 | jev + spec (+ code 1) | 월 경계, UTC 입력, TC-PRD-09 윤년, TC-FEE-13/14/16 데이터 |
| 4 | M10 `attributionMonth` | `FeeCalculatorAttributionMonthTest` | 15 | jev + spec | TC-PRD-06/07/09, 음수·큰 양수 오프셋 |
| 5 | M09 `calculateFee` | `FeeCalculatorCalculateFeeTest` | 43 | jev + spec (+ code 18) | TC-FEE-01 ~ 17 값, 등급별 1,000,000, 기준액 0·1원, 0.5 경계, `Long.MAX_VALUE`, 음수 거부 |
| 6 | M01 `Grade.of` | `GradeTest$Of` | 21 | jev + spec | 네 등급 하한·상한 경계, int 경계, `Long.MAX_VALUE`, 음수 거부 |
| 7 | M11 `startOf` | `FeeCalculatorStartOfTest` | 6 | spec | 월 시작 = 1일 00:00 KST (연초·윤년 포함) |
| 8 | M04 `Payment.<init>` | `PaymentTest` | 11 | spec (+ code 1) | TC-AMT-01 ~ 06, null 시각 |
| 9 | M03 `rateBasisPoints` | `GradeTest$RateBasisPoints` | 4 | spec | 300 / 200 / 100 / 50 bp |
| 10 | M05 `FeeCalculator.<init>` | `FeeCalculatorConstructorTest` | 2 | spec, code | null Clock, 주입 Clock의 시간대 무관 |
| 11 | M02 `minimumAmount` | `GradeTest$MinimumAmount` | 5 | spec (+ code 1) | 등급별 하한, `Grade.of`와의 일관성 |
| — | `Grade` enum 전체 | `GradeTest` | 1 | spec | 등급이 정확히 4개 |

### 3.4 코드 분석으로 추가한 위험 (`basis-code`, 33개)

| 위험 | 테스트 |
|------|--------|
| 마감 판정이 시계의 시간대나 UTC 표기에 흔들림 | `SettlementClosing.closingBoundaryUsesSeoulEvenForUtcClock`, `FeeCalculatorConstructorTest.usesInjectedClockInstant`(spec) |
| 마감 전 요청이 입력 데이터에 따라 통과 | `SettlementClosing.rejectsRegardlessOfPayments` |
| 정산월 이후 월 결제가 섞여 들어감 | `Interactions.laterMonthPaymentsIgnored`, `CumulativeUsage.futureMonthsExcluded` |
| 결제 목록 순서 의존 | `Interactions.orderIndependent` |
| 1월 정산의 연도 넘김 계산 | `CumulativeUsage.januarySettlementSpansPreviousYear` |
| 같은 월 이름·다른 연도 혼동 (`getMonth()` 비교 같은 실수) | `MonthlyUsage.differentYearSameMonthExcluded` |
| 최대 합계(12 × 10억)와 `Long.MAX_VALUE` 기준액의 중간값 오버플로 | `CumulativeUsage.maximumWindowSum`, `CalculateFee.longMaxBase` (4) |
| 등급별 bp가 뒤섞임 (0.5 경계 근처를 등급마다 확인) | `CalculateFee.halfBoundaries` (9), `CalculateFee.oneWonBase` (4) |
| 누적 계산이 Clock에 의존 | `CumulativeUsage.independentOfClock` |
| null 입력 (payments, settlementMonth, grade, occurredAt, clock) | 각 클래스의 null 테스트 5개 |
| `minimumAmount`와 `Grade.of` 기준 불일치 | `MinimumAmount.minimumIsConsistentWithOf` |

## 4. 테스트 실행 결과

### 4.1 첫 실행 (보존본)

- 명령: `./gradlew test --continue` (JDK temurin 21.0.12.1, Gradle 8.14.3)
- 원본: `docs/experiment-b-runs/first-run/test-results/*.xml`, `jacocoTestReport.xml`, `jacocoTestReport.csv`

| 항목 | 값 |
|------|---:|
| 전체 테스트 | 187 |
| 성공 | 187 |
| 실패 | 0 |
| 오류 | 0 |
| 스킵 | 0 |

테스트 코드의 기대값 오류 1건은 **첫 실행 전에** 발견해 고쳤다(§8.2). 그래서 첫 실행 보존본은 고친 뒤의 코드로 실행한 결과다.

### 4.2 최종 측정 실행

`./gradlew test --rerun-tasks`로 다시 실행했다. 187 / 성공 187 / 실패 0 / 오류 0 / 스킵 0으로 첫 실행과 같고, 커버리지도 같다. 첫 실행 이후 테스트 코드는 바꾸지 않았다.

### 4.3 실패 분류

실패가 없어 분류할 항목이 없다.

| 분류 | 건수 |
|------|----:|
| 테스트 코드 오류 | 0 (첫 실행 전 수정 1건은 §8.2) |
| 명세 해석 오류 | 0 |
| 프로덕션 코드의 잠재적 버그 | 0 |
| 명세 미확정으로 인한 판단 불가 | 0 (실패는 없지만 판단 불가 항목은 §6.2) |

## 5. JaCoCo 커버리지

JaCoCo 0.8.15, 최종 측정 실행 기준(첫 실행과 같음).

| 범위 | Line | Branch | Instruction | Method |
|------|------|--------|-------------|--------|
| **`billing` 패키지 (실험 대상)** | **67 / 67 (100%)** | **26 / 26 (100%)** | 316 / 316 (100%) | 15 / 15 (100%) |
| 프로젝트 전체 | 67 / 70 (95.7%) | 26 / 26 (100%) | 316 / 324 (97.5%) | 15 / 17 (88.2%) |

전체에서 빠진 3줄은 `TestabilityLabApplication`(Spring Boot 진입점, 실험 범위 밖)이다. 이 클래스의 테스트는 작성하지 않았다.

메서드별 (`billing`):

| 메서드 | Line | Branch |
|--------|------|--------|
| `Grade.of` | 9 / 9 | 8 / 8 |
| `Grade.minimumAmount`, `rateBasisPoints`, 생성자, static 초기화 | 11 / 11 | — |
| `FeeCalculator.<init>` | 3 / 3 | — |
| `FeeCalculator.calculate` | 13 / 13 | 2 / 2 |
| `FeeCalculator.cumulativeUsage` | 9 / 9 | 6 / 6 |
| `FeeCalculator.monthlyUsage` | 6 / 6 | 4 / 4 |
| `FeeCalculator.calculateFee` | 6 / 6 | 2 / 2 |
| `FeeCalculator.attributionMonth`, `startOf`, static 초기화 | 4 / 4 | — |
| `Payment.<init>` | 5 / 5 | 4 / 4 |
| `FeeResult.<init>` | 1 / 1 | — |

커버리지 100%는 모든 줄과 분기가 **실행됐다**는 뜻이다. 결함을 찾아낼 수 있는지(뮤테이션 탐지율)는 이 Phase에서 측정하지 않았고 예측하지 않는다.

## 6. 발견한 버그 후보

### 6.1 버그 후보

**0건.** 187개 테스트가 모두 명세의 기대값(매트릭스 사람 계산값과 독립 손 계산값)과 일치했다. 코드 분석 중에도 명세 범위 안에서 결과 값이 틀리는 경로는 찾지 못했다.

### 6.2 명세상 판단할 수 없거나 테스트로 검증할 수 없는 항목

| ID | 항목 | 관찰 | 판단할 수 없는 이유 |
|----|------|------|---------------------|
| U-1 | 합산 오버플로의 오류 형태 (BR-AMT-003 `AMOUNT_OVERFLOW`) | `Math.addExact`의 `ArithmeticException`이 코드 문자열 없이 전파된다(`EXPERIMENT_SCOPE.md` §6에 이미 기록됨) | (a) 결제 1건 상한이 10억이라 `long`이 넘치려면 약 92억 건이 필요해 정상 `List`로는 재현할 수 없다(C-06). (b) 이 형태를 결함으로 볼지 실험 범위상 허용으로 볼지 명세가 정하지 않았다. 테스트를 작성하지 않았다 |
| U-2 | null 입력의 오류 형태 | `Objects.requireNonNull` 등으로 JDK 예외가 난다 | 명세의 `INVALID_REQUEST`는 API 수준 규칙이고, 이번 범위(도메인 클래스)의 예외 종류는 정해져 있지 않다. 테스트는 "계산하지 않고 예외"만 확인한다 |
| U-3 | 결제 목록 안의 null 원소 | `attributionMonth`에서 NPE가 난다 | U-2와 같다. 테스트를 작성하지 않았다 |
| U-4 | OPEN-01 / 03 / 04 / 15 전제 | 104개 테스트(`open-premise`)의 기대값이 이 전제에 의존한다 | 승인 대기 상태. 전제가 바뀌면 해당 테스트의 기대값을 다시 계산해야 한다. 특히 OPEN-04가 HALF_EVEN이나 절사로 바뀌면 TC-FEE-08/10 대응 테스트가 달라진다 |
| U-5 | 정산월 이후 시각의 결제나 미래 시각 결제 | 거부하지 않고 집계에서만 빠진다 | 거래 시각 검증(BR-TIM-003)은 이번 범위에서 제외됐다. "빠진다"만 테스트했다 |

## 7. Jev 판단의 불확실성이 미친 영향

Phase 4의 원시 결과, 환산 점수, confidence, review 상태는 그대로 두었다(`JEV_RISK_ASSESSMENT.md` 변경 없음).

1. **테스트 범위에는 영향이 없었다.** 상위 6개는 어느 순위(환산·보조)를 따르든 같은 집합이고, 11개 메서드를 모두 테스트했다. 그래서 순위 불확실성이 "어떤 메서드를 테스트하느냐"를 바꾸지 않았다.
2. **작성 순서에는 영향이 있을 수 있었다.** 순서는 잠정 점수 기준이다. M06 ~ M10의 합계 범위가 겹쳐서, 다른 환산 방식을 썼다면 순서가 달랐을 수 있다. 이번 Phase는 모든 대상을 끝까지 작성했으므로 순서 차이가 최종 테스트 집합에는 나타나지 않는다.
3. **불확실한 항목은 명세 근거로 보완했다.** 영향도 판단이 양극으로 갈린 `Grade.of`(confidence 0.00)와 `rateBasisPoints`(0.00), 범위가 가장 넓은 `calculateFee`(33.8 ~ 78.8), 복잡도가 갈린 `startOf`(0.00)는 Jev 점수와 무관하게 명세 필수 규칙(BR-GRD-001, BR-FEE-002, BR-STL-002)으로 검증했다. 이 테스트들은 `basis-spec`으로 기록했고 Jev 추천으로 기록하지 않았다.
4. **메서드별 테스트 수는 Jev 순위와 비례하지 않는다.** 5위 `calculateFee`(43개)가 2 ~ 4위보다 많다. 반올림 경계 케이스가 명세에 많기 때문이다. 테스트 수는 Jev 순위보다 명세의 경계값 개수에 더 크게 좌우됐다.
5. **이번 결과로는 Jev의 도움 정도를 판단할 수 없다.** 버그 후보가 0건이고 실패도 없어서, Jev 우선순위가 결함 발견에 도움이 됐는지를 이 Phase의 데이터로는 보여줄 수 없다. 탐지 능력 비교는 계획된 다음 단계(PIT)의 대상이다.

## 8. 테스트 작성 과정에서 발생한 문제

| # | 문제 | 처리 |
|---|------|------|
| 1 | `JAVA_HOME`이 설정되지 않아 `gradlew`가 실행되지 않음 | 시스템 설정은 바꾸지 않고, 명령마다 `JAVA_HOME=C:/Users/PC/.jdks/temurin-21.0.12.1`을 지정해 실행 |
| 2 | **테스트 코드 기대값 오류 (첫 실행 전 발견)** — `attributionMonth`의 `2026-08-01T05:00:00+14:00` 기대값을 처음에 `2026-07`로 적음. 실제로는 UTC 7/31 15:00 = KST 8/1 00:00이라 `2026-08`이 맞음 | 분류: **테스트 코드 오류**. 코드 리뷰 중 손 계산으로 다시 확인해 기대값을 `2026-08`로 고쳤다. 원래 의도한 "7월로 남는 큰 양수 오프셋" 케이스로 `04:59:59+14:00 → 2026-07`을 추가했다. 구현 결과를 보고 바꾼 것이 아니다(첫 실행 전) |
| 3 | 태그별 개수를 세려고 Gradle init 스크립트를 쓸 때, `finalizedBy` 초기화 구문 오류로 첫 시도가 모두 0건 | 해당 구문을 빼고 `-x jacocoTestReport`로 다시 실행. init 스크립트는 저장소 밖 임시 폴더에 두었고 `build.gradle`은 바꾸지 않았다 |
| 4 | JUnit 태그 상속으로 `[코드 분석]` 테스트 일부가 `basis-spec`도 갖게 됨 | §3.1처럼 "코드 분석 태그가 있으면 주 근거는 코드 분석"으로 집계 규칙을 정해 기록 |
| 5 | 매트릭스의 null·예외 기대값이 API 수준(`INVALID_REQUEST`)으로만 정의됨 | 특정 예외 타입을 정답으로 고정하지 않고 `RuntimeException`(계산 거부)만 확인 (U-2) |

## 9. 변경 파일

새로 만든 파일만 있다. 프로덕션 코드, `build.gradle`, 명세, `JEV_RISK_ASSESSMENT.md`는 바꾸지 않았다.

- `src/test/java/com/testability/lab/billing/` — `Basis.java`, `Fixtures.java`, `FeeCalculatorCalculateTest.java`, `FeeCalculatorCumulativeUsageTest.java`, `FeeCalculatorMonthlyUsageTest.java`, `FeeCalculatorAttributionMonthTest.java`, `FeeCalculatorCalculateFeeTest.java`, `FeeCalculatorStartOfTest.java`, `FeeCalculatorConstructorTest.java`, `GradeTest.java`, `PaymentTest.java`
- `docs/EXPERIMENT_B_TEST_RESULTS.md` (이 문서)
- `docs/experiment-b-runs/first-run/` — 첫 실행의 JUnit XML, JaCoCo XML·CSV
