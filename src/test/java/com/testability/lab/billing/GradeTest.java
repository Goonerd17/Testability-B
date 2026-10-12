package com.testability.lab.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * M01 Grade.of (환산 6위, 47.3, 잠정 — 영향도 confidence 0.00, 양극 분포),
 * M03 Grade.rateBasisPoints (9위), M02 Grade.minimumAmount (11위).
 * 등급 경계와 요율은 BR-GRD-001 필수 규칙이라 Jev 점수와 무관하게 모두 검증한다.
 * 기대값은 BUSINESS_RULES §7 등급표에서 옮겼다.
 */
@DisplayName("Grade")
class GradeTest {

    @Nested
    @Tag(Basis.JEV)
    @Tag(Basis.SPEC)
    @DisplayName("M01 Grade.of")
    class Of {

        @ParameterizedTest(name = "[{index}] GB {0} → {1}")
        @DisplayName("등급 구간 경계: 하한 이상 포함, 상한 미만 (TC-GRD-01 ~ 08, TEST_CASE_MATRIX §1.1)")
        @CsvSource({
                "0,                   BASIC",
                "1,                   BASIC",
                "500000,              BASIC",
                "999999,              BASIC",
                "1000000,             SILVER",
                "1000001,             SILVER",
                "3000000,             SILVER",
                "4999999,             SILVER",
                "5000000,             GOLD",
                "5000001,             GOLD",
                "7000000,             GOLD",
                "9999999,             GOLD",
                "10000000,            VIP",
                "10000001,            VIP",
                "2147483647,          VIP",
                "2147483648,          VIP",
                "3000000000,          VIP",
                "9223372036854775807, VIP"
        })
        void boundaries(long gradeBasisAmount, Grade expected) {
            assertThat(Grade.of(gradeBasisAmount)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "[{index}] GB {0}")
        @ValueSource(longs = {-1L, -1_000_000L, Long.MIN_VALUE})
        @DisplayName("음수 등급 실적은 거부 (DOMAIN_MODEL §4 GradePolicy: GB < 0 불허)")
        void negativeRejected(long gradeBasisAmount) {
            assertThatThrownBy(() -> Grade.of(gradeBasisAmount))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @Tag(Basis.SPEC)
    @Tag(Basis.OPEN_PREMISE)
    @DisplayName("M03 Grade.rateBasisPoints")
    class RateBasisPoints {

        @ParameterizedTest(name = "[{index}] {0} → {1}bp")
        @DisplayName("등급별 수수료율 bp: 3.0% / 2.0% / 1.0% / 0.5% (BR-GRD-001, OPEN-15 고정 요율)")
        @CsvSource({"BASIC, 300", "SILVER, 200", "GOLD, 100", "VIP, 50"})
        void rates(Grade grade, int expectedBasisPoints) {
            assertThat(grade.rateBasisPoints()).isEqualTo(expectedBasisPoints);
        }
    }

    @Nested
    @Tag(Basis.SPEC)
    @DisplayName("M02 Grade.minimumAmount")
    class MinimumAmount {

        @ParameterizedTest(name = "[{index}] {0} → {1}")
        @DisplayName("등급별 하한 (BR-GRD-001)")
        @CsvSource({"BASIC, 0", "SILVER, 1000000", "GOLD, 5000000", "VIP, 10000000"})
        void minimums(Grade grade, long expectedMinimum) {
            assertThat(grade.minimumAmount()).isEqualTo(expectedMinimum);
        }

        @Test
        @Tag(Basis.CODE)
        @DisplayName("[코드 분석] 각 등급의 하한을 Grade.of에 넣으면 그 등급, 하한-1이면 아래 등급")
        void minimumIsConsistentWithOf() {
            assertThat(Grade.of(Grade.BASIC.minimumAmount())).isEqualTo(Grade.BASIC);
            assertThat(Grade.of(Grade.SILVER.minimumAmount())).isEqualTo(Grade.SILVER);
            assertThat(Grade.of(Grade.SILVER.minimumAmount() - 1)).isEqualTo(Grade.BASIC);
            assertThat(Grade.of(Grade.GOLD.minimumAmount())).isEqualTo(Grade.GOLD);
            assertThat(Grade.of(Grade.GOLD.minimumAmount() - 1)).isEqualTo(Grade.SILVER);
            assertThat(Grade.of(Grade.VIP.minimumAmount())).isEqualTo(Grade.VIP);
            assertThat(Grade.of(Grade.VIP.minimumAmount() - 1)).isEqualTo(Grade.GOLD);
        }
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("등급은 BASIC, SILVER, GOLD, VIP 네 개뿐 (BR-GRD-001)")
    void exactlyFourGrades() {
        assertThat(Grade.values()).containsExactly(Grade.BASIC, Grade.SILVER, Grade.GOLD, Grade.VIP);
    }
}
