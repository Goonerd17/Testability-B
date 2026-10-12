package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.defaultCalculator;
import static com.testability.lab.billing.Fixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * M07 FeeCalculator.cumulativeUsage — Phase 4 환산 순위 2위 (69.4, 잠정).
 * 정산월 2026-07 → 산정 기간 2025-07 ~ 2026-06 (BR-PRF-002, BR-PRF-003).
 */
@Tag(Basis.JEV)
@DisplayName("M07 FeeCalculator.cumulativeUsage")
class FeeCalculatorCumulativeUsageTest {

    private static final YearMonth JULY_2026 = YearMonth.of(2026, 7);

    private final FeeCalculator calculator = defaultCalculator();

    @ParameterizedTest(name = "[{index}] {0} → {1}원 ({2})")
    @Tag(Basis.SPEC)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("산정 기간 경계: 1,000,000원 결제 한 건의 포함 여부 (TC-PRD-01 ~ 04, 06)")
    @CsvSource(delimiter = '|', value = {
            "2025-06-30T23:59:59.999999999+09:00 | 0         | TC-PRD-02 M-13 마지막 1ns 제외",
            "2025-07-01T00:00:00+09:00           | 1000000   | TC-PRD-01 M-12 시작 정각 포함",
            "2026-06-30T23:59:59.999999999+09:00 | 1000000   | TC-PRD-03 M-1 마지막 1ns 포함",
            "2026-07-01T00:00:00+09:00           | 0         | TC-PRD-04 당월 시작 정각 제외",
            "2026-07-31T23:59:59.999999999+09:00 | 0         | 당월 마지막 1ns 제외",
            "2025-06-30T15:00:00Z                | 1000000   | UTC 입력이 Asia/Seoul로 2025-07-01 00:00",
            "2025-06-30T14:59:59Z                | 0         | UTC 입력이 Asia/Seoul로 2025-06-30 23:59:59",
            "2026-06-30T14:59:59Z                | 1000000   | TC-PRD-06(b) 6월 귀속",
            "2026-06-30T15:00:00Z                | 0         | TC-PRD-06(a) 7월 귀속"
    })
    void periodBoundary(String occurredAt, long expected, String description) {
        assertThat(calculator.cumulativeUsage(List.of(payment(occurredAt, 1_000_000)), JULY_2026))
                .isEqualTo(expected);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-PRD-05 13개월 함정: M-13(500,000)과 당월(500,000)을 빼고 12개월 × 50,000 = 600,000")
    void thirteenMonthTrap() {
        List<Payment> payments = new ArrayList<>();
        payments.add(payment("2025-06-15T12:00:00+09:00", 500_000));
        payments.add(payment("2025-07-15T12:00:00+09:00", 50_000));
        payments.add(payment("2025-08-15T12:00:00+09:00", 50_000));
        payments.add(payment("2025-09-15T12:00:00+09:00", 50_000));
        payments.add(payment("2025-10-15T12:00:00+09:00", 50_000));
        payments.add(payment("2025-11-15T12:00:00+09:00", 50_000));
        payments.add(payment("2025-12-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-01-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-02-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-03-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-04-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-05-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-06-15T12:00:00+09:00", 50_000));
        payments.add(payment("2026-07-15T12:00:00+09:00", 500_000));

        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isEqualTo(600_000);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-GRD-09 여러 달 분산 합산 300,000 + 300,000 + 400,000 = 1,000,000")
    void sumsAcrossMonths() {
        List<Payment> payments = List.of(
                payment("2025-08-15T12:00:00+09:00", 300_000),
                payment("2025-11-15T12:00:00+09:00", 300_000),
                payment("2026-04-15T12:00:00+09:00", 400_000));
        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isEqualTo(1_000_000);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("같은 달 여러 건도 모두 합산")
    void sumsWithinSameMonth() {
        List<Payment> payments = List.of(
                payment("2026-03-01T00:00:00+09:00", 1),
                payment("2026-03-15T12:00:00+09:00", 20),
                payment("2026-03-31T23:59:59.999999999+09:00", 300));
        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isEqualTo(321);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("BR-PRF-004 결제가 없으면 0")
    void emptyIsZero() {
        assertThat(calculator.cumulativeUsage(List.of(), JULY_2026)).isZero();
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("TC-AMT-08 int 최댓값 2,147,483,647과 그 +1 이 순환 없이 합산 (BR-AMT-003)")
    void intBoundarySum() {
        List<Payment> payments = new ArrayList<>(List.of(
                payment("2025-09-15T12:00:00+09:00", 1_000_000_000),
                payment("2025-12-15T12:00:00+09:00", 1_000_000_000),
                payment("2026-03-15T12:00:00+09:00", 147_483_647)));
        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isEqualTo(2_147_483_647L);

        payments.add(payment("2026-04-15T12:00:00+09:00", 1));
        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isEqualTo(2_147_483_648L);
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 12개월 각각 상한 금액 10억 → 120억 (long 합산)")
    void maximumWindowSum() {
        List<Payment> payments = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            payments.add(payment(YearMonth.of(2025, 7).plusMonths(i) + "-15T12:00:00+09:00", 1_000_000_000));
        }
        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isEqualTo(12_000_000_000L);
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 정산월이 1월이면 산정 기간이 전년도 1월 ~ 12월 (연도 넘김)")
    void januarySettlementSpansPreviousYear() {
        YearMonth january2026 = YearMonth.of(2026, 1);
        List<Payment> payments = List.of(
                payment("2024-12-31T23:59:59.999999999+09:00", 1),
                payment("2025-01-01T00:00:00+09:00", 10),
                payment("2025-12-31T23:59:59.999999999+09:00", 100),
                payment("2026-01-01T00:00:00+09:00", 1_000));
        assertThat(calculator.cumulativeUsage(payments, january2026)).isEqualTo(110);
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 정산월 이후 월의 결제는 제외")
    void futureMonthsExcluded() {
        List<Payment> payments = List.of(
                payment("2026-08-15T12:00:00+09:00", 1_000_000),
                payment("2027-06-15T12:00:00+09:00", 1_000_000));
        assertThat(calculator.cumulativeUsage(payments, JULY_2026)).isZero();
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 결과가 Clock(현재 시각)에 의존하지 않음")
    void independentOfClock() {
        List<Payment> payments = List.of(payment("2026-01-15T12:00:00+09:00", 777));
        long early = Fixtures.calculatorAt("2000-01-01T00:00:00+09:00").cumulativeUsage(payments, JULY_2026);
        long late = Fixtures.calculatorAt("2100-01-01T00:00:00+09:00").cumulativeUsage(payments, JULY_2026);
        assertThat(early).isEqualTo(777);
        assertThat(late).isEqualTo(777);
    }
}
