package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * M10 FeeCalculator.attributionMonth — Phase 4 환산 순위 4위 (58.1, 잠정). BR-TIM-001, BR-TIM-002.
 * 기대 귀속월은 입력 시각을 사람이 Asia/Seoul(+09:00)로 환산해 적었다.
 */
@Tag(Basis.JEV)
@Tag(Basis.SPEC)
@Tag(Basis.OPEN_PREMISE)
@DisplayName("M10 FeeCalculator.attributionMonth")
class FeeCalculatorAttributionMonthTest {

    @ParameterizedTest(name = "[{index}] {0} → {1} ({2})")
    @DisplayName("Asia/Seoul 기준 귀속월")
    @CsvSource(delimiter = '|', value = {
            "2026-07-15T12:00:00+09:00           | 2026-07 | 월 중간",
            "2026-07-01T00:00:00+09:00           | 2026-07 | 월 시작 정각",
            "2026-06-30T23:59:59.999999999+09:00 | 2026-06 | 월말 마지막 1ns",
            "2026-06-30T15:00:00Z                | 2026-07 | TC-PRD-06(a) UTC 15:00 = KST 다음날 00:00",
            "2026-06-30T14:59:59Z                | 2026-06 | TC-PRD-06(b) UTC 14:59:59 = KST 23:59:59",
            "2026-06-30T14:59:59.999999999Z      | 2026-06 | UTC 기준 KST 월말 마지막 1ns",
            "2024-12-31T23:59:59.999999999+09:00 | 2024-12 | TC-PRD-07(a) 연말",
            "2025-01-01T00:00:00+09:00           | 2025-01 | TC-PRD-07(b) 연초",
            "2024-12-31T15:00:00Z                | 2025-01 | UTC 연말 15:00 = KST 새해",
            "2028-02-29T23:59:59.999999999+09:00 | 2028-02 | TC-PRD-09 윤년 2/29",
            "2028-03-01T00:00:00+09:00           | 2028-03 | TC-PRD-09 윤년 다음날",
            "2026-07-31T10:00:00-05:00           | 2026-08 | 음수 오프셋: 7/31 10:00 -05:00 = 8/1 00:00 KST",
            "2026-07-31T09:59:59-05:00           | 2026-07 | 음수 오프셋: 7/31 09:59:59 -05:00 = 7/31 23:59:59 KST",
            "2026-08-01T05:00:00+14:00           | 2026-08 | 큰 양수 오프셋: 8/1 05:00 +14:00 = 8/1 00:00 KST",
            "2026-08-01T04:59:59+14:00           | 2026-07 | 큰 양수 오프셋: 8/1 04:59:59 +14:00 = 7/31 23:59:59 KST"
    })
    void attributesInSeoul(String occurredAt, String expectedMonth, String description) {
        assertThat(FeeCalculator.attributionMonth(payment(occurredAt, 1)))
                .isEqualTo(YearMonth.parse(expectedMonth));
    }
}
