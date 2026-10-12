package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * M11 FeeCalculator.startOf — 환산 7위 (41.8, 잠정, 복잡도 confidence 0.00).
 * 정산 마감 경계(BR-STL-002)의 기준 시각이라 명세 근거로 검증한다.
 * 기대 시각은 Asia/Seoul 월 1일 00:00을 사람이 UTC로 환산해 적었다.
 */
@Tag(Basis.SPEC)
@Tag(Basis.OPEN_PREMISE)
@DisplayName("M11 FeeCalculator.startOf")
class FeeCalculatorStartOfTest {

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @DisplayName("월 시작 = 해당 월 1일 00:00:00 Asia/Seoul (BR-TIM-002)")
    @CsvSource({
            "2026-07, 2026-06-30T15:00:00Z",
            "2026-08, 2026-07-31T15:00:00Z",
            "2025-01, 2024-12-31T15:00:00Z",
            "2028-03, 2028-02-29T15:00:00Z",
            "2028-02, 2028-01-31T15:00:00Z",
            "2026-07, 2026-07-01T00:00:00+09:00"
    })
    void startOfMonth(String month, String expectedInstant) {
        assertThat(FeeCalculator.startOf(YearMonth.parse(month))).isEqualTo(at(expectedInstant));
    }
}
