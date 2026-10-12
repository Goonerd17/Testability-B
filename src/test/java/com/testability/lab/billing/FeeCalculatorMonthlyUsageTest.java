package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.defaultCalculator;
import static com.testability.lab.billing.Fixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * M08 FeeCalculator.monthlyUsage — Phase 4 환산 순위 3위 (63.9, 잠정). BR-FEE-001.
 */
@Tag(Basis.JEV)
@DisplayName("M08 FeeCalculator.monthlyUsage")
class FeeCalculatorMonthlyUsageTest {

    private static final YearMonth JULY_2026 = YearMonth.of(2026, 7);

    private final FeeCalculator calculator = defaultCalculator();

    @ParameterizedTest(name = "[{index}] {0} → {1}원 ({2})")
    @Tag(Basis.SPEC)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("월 경계: 1,000,000원 결제 한 건이 2026-07 사용금액에 들어가는지 (BR-TIM-001/002)")
    @CsvSource(delimiter = '|', value = {
            "2026-06-30T23:59:59.999999999+09:00 | 0       | 직전 월 마지막 1ns",
            "2026-07-01T00:00:00+09:00           | 1000000 | TC-PRD-04 당월 시작 정각",
            "2026-07-31T23:59:59.999999999+09:00 | 1000000 | 당월 마지막 1ns",
            "2026-08-01T00:00:00+09:00           | 0       | 다음 월 시작 정각",
            "2026-06-30T15:00:00Z                | 1000000 | TC-PRD-06(a) UTC → 7월",
            "2026-06-30T14:59:59Z                | 0       | TC-PRD-06(b) UTC → 6월",
            "2026-07-31T14:59:59Z                | 1000000 | UTC 7/31 14:59:59 = KST 7/31 23:59:59",
            "2026-07-31T15:00:00Z                | 0       | UTC 7/31 15:00 = KST 8/1 00:00"
    })
    void monthBoundary(String occurredAt, long expected, String description) {
        assertThat(calculator.monthlyUsage(List.of(payment(occurredAt, 1_000_000)), JULY_2026))
                .isEqualTo(expected);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-FEE-13/14 데이터: 같은 달 여러 건 합산 (100×3 = 300, 17×2 = 34)")
    void sumsSameMonth() {
        assertThat(calculator.monthlyUsage(List.of(
                payment("2026-07-15T12:00:00+09:00", 100),
                payment("2026-07-15T12:00:00+09:00", 100),
                payment("2026-07-15T12:00:00+09:00", 100)), JULY_2026)).isEqualTo(300);
        assertThat(calculator.monthlyUsage(List.of(
                payment("2026-07-15T12:00:00+09:00", 17),
                payment("2026-07-15T12:00:00+09:00", 17)), JULY_2026)).isEqualTo(34);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-PRD-05 데이터: 다른 달 결제가 섞여도 당월 500,000만")
    void onlyTargetMonth() {
        List<Payment> payments = List.of(
                payment("2025-07-15T12:00:00+09:00", 50_000),
                payment("2026-06-15T12:00:00+09:00", 50_000),
                payment("2026-07-15T12:00:00+09:00", 500_000),
                payment("2026-08-15T12:00:00+09:00", 70_000),
                payment("2025-07-20T12:00:00+09:00", 9_000));
        assertThat(calculator.monthlyUsage(payments, JULY_2026)).isEqualTo(500_000);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-FEE-16 데이터: 10억 × 3 = 30억 (int 초과, BR-AMT-003)")
    void exceedsInt() {
        assertThat(calculator.monthlyUsage(List.of(
                payment("2026-07-01T00:00:00+09:00", 1_000_000_000),
                payment("2026-07-15T12:00:00+09:00", 1_000_000_000),
                payment("2026-07-31T23:59:59.999999999+09:00", 1_000_000_000)), JULY_2026))
                .isEqualTo(3_000_000_000L);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-PRD-09 윤년 2028-03: 2/29 마지막 1ns 제외, 3/1 정각 100 포함")
    void leapYear() {
        assertThat(calculator.monthlyUsage(List.of(
                payment("2028-02-29T23:59:59.999999999+09:00", 1_000_000),
                payment("2028-03-01T00:00:00+09:00", 100)), YearMonth.of(2028, 3))).isEqualTo(100);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("BR-FEE-004/BR-STL-008 당월 결제가 없으면 0")
    void emptyIsZero() {
        assertThat(calculator.monthlyUsage(List.of(), JULY_2026)).isZero();
        assertThat(calculator.monthlyUsage(
                List.of(payment("2026-06-15T12:00:00+09:00", 1_000)), JULY_2026)).isZero();
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 같은 달 이름이라도 연도가 다르면 제외 (2025-07 ≠ 2026-07)")
    void differentYearSameMonthExcluded() {
        assertThat(calculator.monthlyUsage(List.of(
                payment("2025-07-15T12:00:00+09:00", 1_000),
                payment("2027-07-15T12:00:00+09:00", 2_000)), JULY_2026)).isZero();
    }
}
