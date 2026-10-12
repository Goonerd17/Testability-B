package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.defaultCalculator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * M09 FeeCalculator.calculateFee — Phase 4 환산 순위 5위 (53.4, 잠정, 합계 범위 33.8 ~ 78.8).
 * 반올림 정책(BR-FEE-002, OPEN-04 실험용 전제 HALF_UP)은 Jev 점수와 무관하게 필수 검증 대상이다.
 * 기대값은 TEST_CASE_MATRIX §1.5, §5의 사람 계산값과, 주석에 적은 손 계산값이다.
 */
@Tag(Basis.JEV)
@DisplayName("M09 FeeCalculator.calculateFee")
class FeeCalculatorCalculateFeeTest {

    private final FeeCalculator calculator = defaultCalculator();

    @ParameterizedTest(name = "[{index}] FB {0} × {1} → {2}원 (정확한 값 {3})")
    @Tag(Basis.SPEC)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("HALF_UP 원 단위 반올림 (TC-FEE-01 ~ 11, TC-FEE-15 ~ 17, BR-FEE-002 예시)")
    @CsvSource({
            "16,         BASIC,  0,          0.48",
            "17,         BASIC,  1,          0.51",
            "50,         BASIC,  2,          1.5",
            "24,         SILVER, 0,          0.48",
            "25,         SILVER, 1,          0.50",
            "49,         GOLD,   0,          0.49",
            "50,         GOLD,   1,          0.50",
            "250,        GOLD,   3,          2.5 (HALF_EVEN이면 2)",
            "899,        VIP,    4,          4.495",
            "900,        VIP,    5,          4.5 (HALF_EVEN·절사면 4)",
            "999,        VIP,    5,          4.995",
            "300,        VIP,    2,          1.5 (TC-FEE-13 월 합계)",
            "34,         BASIC,  1,          1.02 (TC-FEE-14 월 합계)",
            "10000000,   BASIC,  300000,     300000 (중간값 30억)",
            "3000000000, BASIC,  90000000,   90000000",
            "3000000000, VIP,    15000000,   15000000",
            "1000000,    BASIC,  30000,      30000 (TC-GRD-01)",
            "1000000,    SILVER, 20000,      20000 (TC-GRD-03)",
            "1000000,    GOLD,   10000,      10000 (TC-GRD-05)",
            "1000000,    VIP,    5000,       5000 (TC-GRD-07)"
    })
    void halfUpRounding(long feeBase, Grade grade, long expectedFee, String exact) {
        assertThat(calculator.calculateFee(feeBase, grade)).isEqualTo(expectedFee);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(Grade.class)
    @Tag(Basis.SPEC)
    @DisplayName("BR-FEE-004 기준액 0이면 모든 등급에서 0원")
    void zeroBaseIsZeroFee(Grade grade) {
        assertThat(calculator.calculateFee(0, grade)).isZero();
    }

    @ParameterizedTest(name = "[{index}] FB 1 × {0} → 0원")
    @EnumSource(Grade.class)
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 기준액 1원은 모든 등급에서 0.5 미만이라 0원 (최저 수수료 없음, BR-FEE-004)")
    void oneWonBase(Grade grade) {
        // 1 × 300 / 10,000 = 0.03 ... 1 × 50 / 10,000 = 0.005
        assertThat(calculator.calculateFee(1, grade)).isZero();
    }

    @ParameterizedTest(name = "[{index}] FB {0} × {1} → {2}원")
    @Tag(Basis.CODE)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("[코드 분석] 0.5 경계 바로 아래/정확히/바로 위 (등급별 bp가 섞이지 않는지)")
    @CsvSource({
            // BASIC 300bp: 0.5원 = 16.67원 기준액 → 16 (0.48) / 17 (0.51)
            "33,  BASIC,  1",   // 0.99
            "183, BASIC,  5",   // 5.49
            "184, BASIC,  6",   // 5.52
            // SILVER 200bp: 75 → 1.5, 74 → 1.48
            "74,  SILVER, 1",
            "75,  SILVER, 2",
            // GOLD 100bp: 149 → 1.49, 150 → 1.5
            "149, GOLD,   1",
            "150, GOLD,   2",
            // VIP 50bp: 2,899 → 14.495, 2,900 → 14.5
            "2899, VIP,   14",
            "2900, VIP,   15"
    })
    void halfBoundaries(long feeBase, Grade grade, long expectedFee) {
        assertThat(calculator.calculateFee(feeBase, grade)).isEqualTo(expectedFee);
    }

    @ParameterizedTest(name = "[{index}] Long.MAX_VALUE × {0} → {1}")
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] long 최댓값 기준액에서도 중간 계산이 넘치지 않음 (BR-AMT-003)")
    @CsvSource({
            // 9,223,372,036,854,775,807 × bp / 10,000 을 분수로 계산해 HALF_UP 반올림한 값
            "BASIC,  276701161105643274",
            "SILVER, 184467440737095516",
            "GOLD,   92233720368547758",
            "VIP,    46116860184273879"
    })
    void longMaxBase(Grade grade, long expectedFee) {
        assertThat(calculator.calculateFee(Long.MAX_VALUE, grade)).isEqualTo(expectedFee);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("BR-FEE-005 음수 기준액은 거부 (IllegalArgumentException)")
    void negativeBaseRejected() {
        assertThatThrownBy(() -> calculator.calculateFee(-1, Grade.BASIC))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> calculator.calculateFee(Long.MIN_VALUE, Grade.VIP))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 등급이 null이면 계산하지 않고 예외")
    void nullGradeRejected() {
        assertThatThrownBy(() -> calculator.calculateFee(1_000, null))
                .isInstanceOf(RuntimeException.class);
    }
}
