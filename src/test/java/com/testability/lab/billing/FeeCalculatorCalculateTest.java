package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.calculatorAt;
import static com.testability.lab.billing.Fixtures.defaultCalculator;
import static com.testability.lab.billing.Fixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * M06 FeeCalculator.calculate — Phase 4 환산 순위 1위 (75.2, 잠정).
 * 기대값은 TEST_CASE_MATRIX에 사람이 계산해 둔 값이며, 매트릭스에 없는 값은 주석에 손 계산 과정을 남긴다.
 */
@Tag(Basis.JEV)
@DisplayName("M06 FeeCalculator.calculate")
class FeeCalculatorCalculateTest {

    private static final YearMonth JULY_2026 = YearMonth.of(2026, 7);

    /** TEST_CASE_MATRIX §2: 모든 GRD 케이스에 당월 결제 07-15 1,000,000을 넣어 FB = 1,000,000 */
    private static final String CURRENT_MONTH_PAYMENT_AT = "2026-07-15T12:00:00+09:00";

    private static void assertResult(FeeResult result, Grade grade, long gradeBasis, long feeBase, long fee) {
        assertThat(result.grade()).as("grade").isEqualTo(grade);
        assertThat(result.gradeBasisAmount()).as("GB").isEqualTo(gradeBasis);
        assertThat(result.feeBaseAmount()).as("FB").isEqualTo(feeBase);
        assertThat(result.fee()).as("fee").isEqualTo(fee);
    }

    private static List<Payment> withCurrentMonthPayment(Payment... windowPayments) {
        List<Payment> payments = new ArrayList<>(List.of(windowPayments));
        payments.add(payment(CURRENT_MONTH_PAYMENT_AT, 1_000_000));
        return payments;
    }

    @Nested
    @Tag(Basis.SPEC)
    @DisplayName("등급 경계 (TC-GRD, BR-GRD-001)")
    class GradeBoundaries {

        @Test
        @DisplayName("TC-GRD-01 이력 없음 → BASIC / 0 / 1,000,000 / 30,000")
        void noHistory() {
            FeeResult result = defaultCalculator().calculate(withCurrentMonthPayment(), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 1_000_000, 30_000);
        }

        @Test
        @DisplayName("TC-GRD-02 999,999 → BASIC / 30,000")
        void justBelowSilver() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 999_999)), JULY_2026);
            assertResult(result, Grade.BASIC, 999_999, 1_000_000, 30_000);
        }

        @Test
        @DisplayName("TC-GRD-03 1,000,000 → SILVER / 20,000")
        void silverLowerBound() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 1_000_000)), JULY_2026);
            assertResult(result, Grade.SILVER, 1_000_000, 1_000_000, 20_000);
        }

        @Test
        @DisplayName("TC-GRD-04 4,999,999 → SILVER / 20,000")
        void justBelowGold() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 4_999_999)), JULY_2026);
            assertResult(result, Grade.SILVER, 4_999_999, 1_000_000, 20_000);
        }

        @Test
        @DisplayName("TC-GRD-05 5,000,000 → GOLD / 10,000")
        void goldLowerBound() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 5_000_000)), JULY_2026);
            assertResult(result, Grade.GOLD, 5_000_000, 1_000_000, 10_000);
        }

        @Test
        @DisplayName("TC-GRD-06 9,999,999 → GOLD / 10,000")
        void justBelowVip() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 9_999_999)), JULY_2026);
            assertResult(result, Grade.GOLD, 9_999_999, 1_000_000, 10_000);
        }

        @Test
        @DisplayName("TC-GRD-07 10,000,000 → VIP / 5,000")
        void vipLowerBound() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 10_000_000)), JULY_2026);
            assertResult(result, Grade.VIP, 10_000_000, 1_000_000, 5_000);
        }

        @Test
        @DisplayName("TC-GRD-08 30억 → VIP / 3,000,000,000 / 5,000")
        void threeBillion() {
            FeeResult result = defaultCalculator().calculate(withCurrentMonthPayment(
                    payment("2025-09-15T12:00:00+09:00", 1_000_000_000),
                    payment("2025-12-15T12:00:00+09:00", 1_000_000_000),
                    payment("2026-03-15T12:00:00+09:00", 1_000_000_000)), JULY_2026);
            assertResult(result, Grade.VIP, 3_000_000_000L, 1_000_000, 5_000);
        }

        @Test
        @DisplayName("TC-GRD-09 여러 달 분산 합계 1,000,000 → SILVER / 20,000")
        void spreadOverMonths() {
            FeeResult result = defaultCalculator().calculate(withCurrentMonthPayment(
                    payment("2025-08-15T12:00:00+09:00", 300_000),
                    payment("2025-11-15T12:00:00+09:00", 300_000),
                    payment("2026-04-15T12:00:00+09:00", 400_000)), JULY_2026);
            assertResult(result, Grade.SILVER, 1_000_000, 1_000_000, 20_000);
        }

        @Test
        @DisplayName("TC-AMT-08 GB = int 최댓값 / 최댓값+1 이 순환 없이 VIP")
        void intBoundaryGradeBasis() {
            List<Payment> base = List.of(
                    payment("2025-09-15T12:00:00+09:00", 1_000_000_000),
                    payment("2025-12-15T12:00:00+09:00", 1_000_000_000),
                    payment("2026-03-15T12:00:00+09:00", 147_483_647));
            FeeResult atMax = defaultCalculator().calculate(base, JULY_2026);
            assertThat(atMax.grade()).isEqualTo(Grade.VIP);
            assertThat(atMax.gradeBasisAmount()).isEqualTo(2_147_483_647L);

            List<Payment> plusOne = new ArrayList<>(base);
            plusOne.add(payment("2026-04-15T12:00:00+09:00", 1));
            FeeResult overMax = defaultCalculator().calculate(plusOne, JULY_2026);
            assertThat(overMax.grade()).isEqualTo(Grade.VIP);
            assertThat(overMax.gradeBasisAmount()).isEqualTo(2_147_483_648L);
        }
    }

    @Nested
    @Tag(Basis.SPEC)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("산정 기간·시간 경계 (TC-PRD, BR-PRF-002/003, BR-TIM-001/002)")
    class PeriodBoundaries {

        @Test
        @DisplayName("TC-PRD-01 산정 기간 시작 정각(2025-07-01 00:00) 결제는 포함 → SILVER / 1,000,000 / 0 / 0")
        void periodStartIncluded() {
            FeeResult result = defaultCalculator().calculate(
                    List.of(payment("2025-07-01T00:00:00+09:00", 1_000_000)), JULY_2026);
            assertResult(result, Grade.SILVER, 1_000_000, 0, 0);
        }

        @Test
        @DisplayName("TC-PRD-02 산정 기간 시작 1ns 전 결제는 제외 → BASIC / 0 / 0 / 0")
        void beforePeriodStartExcluded() {
            FeeResult result = defaultCalculator().calculate(
                    List.of(payment("2025-06-30T23:59:59.999999999+09:00", 1_000_000)), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 0, 0);
        }

        @Test
        @DisplayName("TC-PRD-03 직전 월 마지막 1ns 결제는 등급에 포함 → SILVER / 1,000,000 / 0 / 0")
        void lastNanoOfPreviousMonthIncluded() {
            FeeResult result = defaultCalculator().calculate(
                    List.of(payment("2026-06-30T23:59:59.999999999+09:00", 1_000_000)), JULY_2026);
            assertResult(result, Grade.SILVER, 1_000_000, 0, 0);
        }

        @Test
        @DisplayName("TC-PRD-04 당월 시작 정각 결제는 등급 제외·수수료 포함 → BASIC / 0 / 1,000,000 / 30,000")
        void currentMonthStartGoesToFee() {
            FeeResult result = defaultCalculator().calculate(
                    List.of(payment("2026-07-01T00:00:00+09:00", 1_000_000)), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 1_000_000, 30_000);
        }

        @Test
        @DisplayName("TC-PRD-05 13개월 함정: 2025-06과 당월을 빼면 BASIC / 600,000 / 500,000 / 15,000")
        void thirteenMonthTrap() {
            List<Payment> payments = new ArrayList<>();
            payments.add(payment("2025-06-15T12:00:00+09:00", 500_000));
            for (int i = 0; i < 12; i++) {
                YearMonth month = YearMonth.of(2025, 7).plusMonths(i);
                payments.add(payment(month + "-15T12:00:00+09:00", 50_000));
            }
            payments.add(payment("2026-07-15T12:00:00+09:00", 500_000));

            FeeResult result = defaultCalculator().calculate(payments, JULY_2026);
            assertResult(result, Grade.BASIC, 600_000, 500_000, 15_000);
        }

        @Test
        @DisplayName("TC-PRD-06(a) UTC 2026-06-30T15:00:00Z 는 7월 귀속 → BASIC / 0 / 1,000,000 / 30,000")
        void utcInputAttributedToJuly() {
            FeeResult result = defaultCalculator().calculate(
                    List.of(payment("2026-06-30T15:00:00Z", 1_000_000)), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 1_000_000, 30_000);
        }

        @Test
        @DisplayName("TC-PRD-06(b) UTC 2026-06-30T14:59:59Z 는 6월 귀속 → SILVER / 1,000,000 / 0 / 0")
        void utcInputAttributedToJune() {
            FeeResult result = defaultCalculator().calculate(
                    List.of(payment("2026-06-30T14:59:59Z", 1_000_000)), JULY_2026);
            assertResult(result, Grade.SILVER, 1_000_000, 0, 0);
        }

        @Test
        @DisplayName("TC-PRD-07 연도 경계: 정산 2026-01 (요청 2026-02-10)")
        void yearBoundary() {
            FeeCalculator calculator = calculatorAt("2026-02-10T10:00:00+09:00");
            YearMonth january2026 = YearMonth.of(2026, 1);

            FeeResult a = calculator.calculate(
                    List.of(payment("2024-12-31T23:59:59.999999999+09:00", 1_000_000)), january2026);
            assertResult(a, Grade.BASIC, 0, 0, 0);

            FeeResult b = calculator.calculate(
                    List.of(payment("2025-01-01T00:00:00+09:00", 1_000_000)), january2026);
            assertResult(b, Grade.SILVER, 1_000_000, 0, 0);
        }

        @Test
        @DisplayName("TC-PRD-08 12개월 합산: 83,334×12 = 1,000,008 SILVER / 83,333×12 = 999,996 BASIC")
        void twelveMonthSum() {
            List<Payment> a = new ArrayList<>();
            List<Payment> b = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                String at = YearMonth.of(2025, 7).plusMonths(i) + "-15T12:00:00+09:00";
                a.add(payment(at, 83_334));
                b.add(payment(at, 83_333));
            }
            FeeResult resultA = defaultCalculator().calculate(a, JULY_2026);
            assertThat(resultA.grade()).isEqualTo(Grade.SILVER);
            assertThat(resultA.gradeBasisAmount()).isEqualTo(1_000_008);

            FeeResult resultB = defaultCalculator().calculate(b, JULY_2026);
            assertThat(resultB.grade()).isEqualTo(Grade.BASIC);
            assertThat(resultB.gradeBasisAmount()).isEqualTo(999_996);
        }

        @Test
        @DisplayName("TC-PRD-09 윤년: 정산 2028-03 → SILVER / 1,000,000 / 100 / 2")
        void leapYear() {
            FeeResult result = calculatorAt("2028-04-10T10:00:00+09:00").calculate(List.of(
                    payment("2028-02-29T23:59:59.999999999+09:00", 1_000_000),
                    payment("2028-03-01T00:00:00+09:00", 100)), YearMonth.of(2028, 3));
            assertResult(result, Grade.SILVER, 1_000_000, 100, 2);
        }

        @Test
        @DisplayName("TC-PRD-10 결과에 산정 기간 2025-07 ~ 2026-06 기록")
        void recordsPerformancePeriod() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 1_000_000)), JULY_2026);
            assertThat(result.settlementMonth()).isEqualTo(YearMonth.of(2026, 7));
            assertThat(result.periodFrom()).isEqualTo(YearMonth.of(2025, 7));
            assertThat(result.periodTo()).isEqualTo(YearMonth.of(2026, 6));
        }
    }

    @Nested
    @Tag(Basis.SPEC)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("수수료·반올림 (TC-FEE, BR-FEE-001 ~ 005) — calculate 경유")
    class Fees {

        private static final List<Payment> SILVER_WINDOW = List.of(payment("2026-01-15T12:00:00+09:00", 1_000_000));
        private static final List<Payment> GOLD_WINDOW = List.of(payment("2026-01-15T12:00:00+09:00", 5_000_000));
        private static final List<Payment> VIP_WINDOW = List.of(payment("2026-01-15T12:00:00+09:00", 10_000_000));
        private static final List<Payment> EMPTY_WINDOW = List.of();

        private long feeFor(List<Payment> window, long... currentMonthAmounts) {
            List<Payment> payments = new ArrayList<>(window);
            for (long amount : currentMonthAmounts) {
                payments.add(payment("2026-07-15T12:00:00+09:00", amount));
            }
            return defaultCalculator().calculate(payments, JULY_2026).fee();
        }

        @Test
        @DisplayName("TC-FEE-01 ~ 03 BASIC 300bp: 16→0, 17→1, 50→2")
        void basicRounding() {
            assertThat(feeFor(EMPTY_WINDOW, 16)).isEqualTo(0);
            assertThat(feeFor(EMPTY_WINDOW, 17)).isEqualTo(1);
            assertThat(feeFor(EMPTY_WINDOW, 50)).isEqualTo(2);
        }

        @Test
        @DisplayName("TC-FEE-04, 05 SILVER 200bp: 24→0, 25→1")
        void silverRounding() {
            assertThat(feeFor(SILVER_WINDOW, 24)).isEqualTo(0);
            assertThat(feeFor(SILVER_WINDOW, 25)).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-FEE-06 ~ 08 GOLD 100bp: 49→0, 50→1, 250→3 (HALF_EVEN이면 2)")
        void goldRounding() {
            assertThat(feeFor(GOLD_WINDOW, 49)).isEqualTo(0);
            assertThat(feeFor(GOLD_WINDOW, 50)).isEqualTo(1);
            assertThat(feeFor(GOLD_WINDOW, 250)).isEqualTo(3);
        }

        @Test
        @DisplayName("TC-FEE-09 ~ 11 VIP 50bp: 899→4, 900→5, 999→5")
        void vipRounding() {
            assertThat(feeFor(VIP_WINDOW, 899)).isEqualTo(4);
            assertThat(feeFor(VIP_WINDOW, 900)).isEqualTo(5);
            assertThat(feeFor(VIP_WINDOW, 999)).isEqualTo(5);
        }

        @Test
        @DisplayName("TC-FEE-12 VIP, 당월 거래 없음 → VIP / 10,000,000 / 0 / 0")
        void noCurrentMonthPayment() {
            FeeResult result = defaultCalculator().calculate(VIP_WINDOW, JULY_2026);
            assertResult(result, Grade.VIP, 10_000_000, 0, 0);
        }

        @Test
        @DisplayName("TC-FEE-13 VIP 100×3 → 월 합계 1회 반올림 2 (거래별이면 3)")
        void roundOncePerMonthVip() {
            assertThat(feeFor(VIP_WINDOW, 100, 100, 100)).isEqualTo(2);
        }

        @Test
        @DisplayName("TC-FEE-14 BASIC 17×2 → 월 합계 1회 반올림 1 (거래별이면 2)")
        void roundOncePerMonthBasic() {
            assertThat(feeFor(EMPTY_WINDOW, 17, 17)).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-FEE-15 BASIC 10,000,000 → 300,000 (중간값 30억이 int 초과)")
        void intermediateExceedsInt() {
            assertThat(feeFor(EMPTY_WINDOW, 10_000_000)).isEqualTo(300_000);
        }

        @Test
        @DisplayName("TC-FEE-16 BASIC 1,000,000,000×3 → 90,000,000")
        void hugeBaseBasic() {
            FeeResult result = defaultCalculator().calculate(List.of(
                    payment("2026-07-15T12:00:00+09:00", 1_000_000_000),
                    payment("2026-07-15T12:00:00+09:00", 1_000_000_000),
                    payment("2026-07-15T12:00:00+09:00", 1_000_000_000)), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 3_000_000_000L, 90_000_000);
        }

        @Test
        @DisplayName("TC-FEE-17 VIP 1,000,000,000×3 → 15,000,000")
        void hugeBaseVip() {
            assertThat(feeFor(VIP_WINDOW, 1_000_000_000, 1_000_000_000, 1_000_000_000)).isEqualTo(15_000_000);
        }

        @Test
        @DisplayName("등급별 적용 bp 기록: BASIC 300 / SILVER 200 / GOLD 100 / VIP 50 (BR-GRD-001, BR-STL-011)")
        void recordsRateBasisPoints() {
            assertThat(defaultCalculator().calculate(EMPTY_WINDOW, JULY_2026).rateBasisPoints()).isEqualTo(300);
            assertThat(defaultCalculator().calculate(SILVER_WINDOW, JULY_2026).rateBasisPoints()).isEqualTo(200);
            assertThat(defaultCalculator().calculate(GOLD_WINDOW, JULY_2026).rateBasisPoints()).isEqualTo(100);
            assertThat(defaultCalculator().calculate(VIP_WINDOW, JULY_2026).rateBasisPoints()).isEqualTo(50);
        }
    }

    @Nested
    @Tag(Basis.SPEC)
    @DisplayName("정산 마감 검증 (TC-STL-04/05, BR-STL-002)")
    class SettlementClosing {

        @Test
        @DisplayName("TC-STL-04(a) 2026-07-31T23:59:59.999999999 요청 → SETTLEMENT_PERIOD_NOT_CLOSED")
        void rejectedOneNanoBeforeClose() {
            FeeCalculator calculator = calculatorAt("2026-07-31T23:59:59.999999999+09:00");
            assertThatThrownBy(() -> calculator.calculate(List.of(), JULY_2026))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SETTLEMENT_PERIOD_NOT_CLOSED");
        }

        @Test
        @DisplayName("TC-STL-04(b) 2026-08-01T00:00:00 요청 → 성공")
        void allowedAtClose() {
            FeeResult result = calculatorAt("2026-08-01T00:00:00+09:00").calculate(List.of(), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 0, 0);
        }

        @Test
        @DisplayName("TC-STL-05 2026-08-10에 진행 중인 2026-08, 미래 2026-09 정산 → 둘 다 거부")
        void rejectsOpenAndFutureMonths() {
            FeeCalculator calculator = defaultCalculator();
            assertThatThrownBy(() -> calculator.calculate(List.of(), YearMonth.of(2026, 8)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SETTLEMENT_PERIOD_NOT_CLOSED");
            assertThatThrownBy(() -> calculator.calculate(List.of(), YearMonth.of(2026, 9)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SETTLEMENT_PERIOD_NOT_CLOSED");
        }

        @Test
        @Tag(Basis.CODE)
        @DisplayName("[코드 분석] 마감 경계를 UTC 시각으로 줘도 Asia/Seoul 기준으로 판정")
        void closingBoundaryUsesSeoulEvenForUtcClock() {
            // start(2026-08) = 2026-08-01T00:00+09:00 = 2026-07-31T15:00:00Z
            FeeCalculator before = calculatorAt("2026-07-31T14:59:59.999999999Z");
            assertThatThrownBy(() -> before.calculate(List.of(), JULY_2026))
                    .isInstanceOf(IllegalStateException.class);

            FeeResult atClose = calculatorAt("2026-07-31T15:00:00Z").calculate(List.of(), JULY_2026);
            assertThat(atClose.settlementMonth()).isEqualTo(JULY_2026);
        }

        @Test
        @Tag(Basis.CODE)
        @DisplayName("[코드 분석] 마감 전 거부 시에는 결과를 만들지 않고 입력 데이터와 무관하게 거부")
        void rejectsRegardlessOfPayments() {
            FeeCalculator calculator = calculatorAt("2026-07-20T10:00:00+09:00");
            List<Payment> payments = List.of(payment("2026-07-15T12:00:00+09:00", 1_000_000));
            assertThatThrownBy(() -> calculator.calculate(payments, JULY_2026))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("SETTLEMENT_PERIOD_NOT_CLOSED");
        }
    }

    @Nested
    @DisplayName("규칙 간 상호작용과 등급 변경")
    class Interactions {

        @Test
        @Tag(Basis.SPEC)
        @DisplayName("TC-TIM-07 같은 데이터·같은 Clock이면 서로 다른 인스턴스도 같은 결과 (BR-TIM-004)")
        void deterministic() {
            List<Payment> payments = withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 1_000_000));
            FeeResult first = defaultCalculator().calculate(payments, JULY_2026);
            FeeResult second = defaultCalculator().calculate(payments, JULY_2026);
            assertThat(second).isEqualTo(first);
        }

        @Test
        @Tag(Basis.SPEC)
        @DisplayName("TC-STL-01 대응 항목: SILVER, GB 1,000,000, 기간 2025-07~2026-06, FB 1,000,000, bp 200, 수수료 20,000")
        void fullRecord() {
            FeeResult result = defaultCalculator().calculate(
                    withCurrentMonthPayment(payment("2026-01-15T12:00:00+09:00", 1_000_000)), JULY_2026);
            assertThat(result).isEqualTo(new FeeResult(
                    YearMonth.of(2026, 7), YearMonth.of(2025, 7), YearMonth.of(2026, 6),
                    1_000_000, Grade.SILVER, 1_000_000, 200, 20_000));
        }

        @Test
        @Tag(Basis.SPEC)
        @DisplayName("등급 하락: 2025-07 결제는 2026-07 정산에선 SILVER, 2026-08 정산에선 기간 밖이라 BASIC (BR-PRF-002/003)")
        void gradeDropsWhenPaymentLeavesWindow() {
            List<Payment> payments = List.of(payment("2025-07-15T12:00:00+09:00", 1_000_000));
            FeeCalculator calculator = calculatorAt("2026-09-10T10:00:00+09:00");

            assertResult(calculator.calculate(payments, JULY_2026), Grade.SILVER, 1_000_000, 0, 0);
            assertResult(calculator.calculate(payments, YearMonth.of(2026, 8)), Grade.BASIC, 0, 0, 0);
        }

        @Test
        @Tag(Basis.SPEC)
        @DisplayName("등급 상승: 7월 결제 5,000,000은 7월엔 수수료 기준액(BASIC 3% = 150,000), 8월엔 등급 실적(GOLD)")
        void sameMonthPaymentIsFeeBaseThenGradeBasis() {
            // 7월: GB 0 → BASIC, FB 5,000,000 × 300 / 10,000 = 150,000
            // 8월: GB 5,000,000 → GOLD, FB 0 → 0
            List<Payment> payments = List.of(payment("2026-07-20T12:00:00+09:00", 5_000_000));
            FeeCalculator calculator = calculatorAt("2026-09-10T10:00:00+09:00");

            assertResult(calculator.calculate(payments, JULY_2026), Grade.BASIC, 0, 5_000_000, 150_000);
            assertResult(calculator.calculate(payments, YearMonth.of(2026, 8)), Grade.GOLD, 5_000_000, 0, 0);
        }

        @Test
        @Tag(Basis.SPEC)
        @DisplayName("등급(직전 12개월)과 수수료 기준액(당월)이 독립적으로 결합: GOLD 등급 × 당월 3,000,001 → 30,000")
        void gradeAndFeeBaseCombine() {
            // GB 7,000,000 → GOLD 100bp, FB 3,000,001 × 100 / 10,000 = 30,000.01 → 30,000
            FeeResult result = defaultCalculator().calculate(List.of(
                    payment("2025-10-15T12:00:00+09:00", 3_000_000),
                    payment("2026-05-15T12:00:00+09:00", 4_000_000),
                    payment("2026-07-03T12:00:00+09:00", 3_000_001)), JULY_2026);
            assertResult(result, Grade.GOLD, 7_000_000, 3_000_001, 30_000);
        }

        @Test
        @Tag(Basis.CODE)
        @DisplayName("[코드 분석] 정산월 이후 월의 결제는 등급·기준액 어디에도 들어가지 않음")
        void laterMonthPaymentsIgnored() {
            FeeResult result = calculatorAt("2026-09-10T10:00:00+09:00").calculate(List.of(
                    payment("2026-08-01T00:00:00+09:00", 9_000_000),
                    payment("2026-08-20T12:00:00+09:00", 1_000_000)), JULY_2026);
            assertResult(result, Grade.BASIC, 0, 0, 0);
        }

        @Test
        @Tag(Basis.CODE)
        @DisplayName("[코드 분석] 입력 결제 목록 순서와 무관한 결과")
        void orderIndependent() {
            Payment a = payment("2025-09-15T12:00:00+09:00", 2_000_000);
            Payment b = payment("2026-07-15T12:00:00+09:00", 1_234);
            Payment c = payment("2026-02-15T12:00:00+09:00", 3_500_000);
            FeeResult forward = defaultCalculator().calculate(List.of(a, b, c), JULY_2026);
            FeeResult reversed = defaultCalculator().calculate(List.of(c, b, a), JULY_2026);
            // GB 5,500,000 → GOLD, FB 1,234 × 100 / 10,000 = 12.34 → 12
            assertResult(forward, Grade.GOLD, 5_500_000, 1_234, 12);
            assertThat(reversed).isEqualTo(forward);
        }
    }

    @Nested
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 입력 검증")
    class InputValidation {

        @Test
        @DisplayName("payments가 null이면 계산하지 않고 예외")
        void nullPayments() {
            assertThatThrownBy(() -> defaultCalculator().calculate(null, JULY_2026))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("settlementMonth가 null이면 계산하지 않고 예외")
        void nullSettlementMonth() {
            assertThatThrownBy(() -> defaultCalculator().calculate(List.of(), null))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @Tag(Basis.SPEC)
        @DisplayName("빈 결제 목록 → BASIC / 0 / 0 / 0 (BR-PRF-004, BR-GRD-004, BR-FEE-004)")
        void emptyPayments() {
            assertResult(defaultCalculator().calculate(List.of(), JULY_2026), Grade.BASIC, 0, 0, 0);
        }
    }
}
