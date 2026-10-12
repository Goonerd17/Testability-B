package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.payment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * M05 FeeCalculator 생성자 — 환산 10위 (18.0, 잠정). BR-TIM-004 (Clock 주입).
 */
@DisplayName("M05 FeeCalculator.<init>")
class FeeCalculatorConstructorTest {

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] Clock이 없으면 생성 거부")
    void rejectsNullClock() {
        assertThatThrownBy(() -> new FeeCalculator(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @Tag(Basis.SPEC)
    @DisplayName("BR-TIM-004 주입된 Clock이 '현재 시각'을 결정: 시계의 시간대가 달라도 같은 순간이면 같은 판정")
    void usesInjectedClockInstant() {
        Instant justBeforeClose = Instant.parse("2026-07-31T14:59:59Z");
        List<Clock> clocks = List.of(
                Clock.fixed(justBeforeClose, ZoneOffset.UTC),
                Clock.fixed(justBeforeClose, ZoneId.of("Asia/Seoul")),
                Clock.fixed(justBeforeClose, ZoneId.of("America/New_York")));
        for (Clock clock : clocks) {
            FeeCalculator calculator = new FeeCalculator(clock);
            assertThatThrownBy(() -> calculator.calculate(List.of(), YearMonth.of(2026, 7)))
                    .isInstanceOf(IllegalStateException.class);
        }

        FeeResult result = new FeeCalculator(Clock.fixed(Instant.parse("2026-07-31T15:00:00Z"), ZoneId.of("America/New_York")))
                .calculate(List.of(payment("2026-07-15T12:00:00+09:00", 100)), YearMonth.of(2026, 7));
        assertThat(result.fee()).isEqualTo(3); // 100 × 300 / 10,000 = 3
    }
}
