package com.testability.lab.billing;

import static com.testability.lab.billing.Fixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * M04 Payment 생성자 — 환산 8위 (34.9, 잠정). BR-AMT-002 (OPEN-03 실험용 전제: 1 ~ 1,000,000,000원).
 */
@Tag(Basis.SPEC)
@Tag(Basis.OPEN_PREMISE)
@DisplayName("M04 Payment.<init>")
class PaymentTest {

    private static final Instant OCCURRED_AT = at("2026-07-15T12:00:00+09:00");

    @ParameterizedTest(name = "[{index}] {0}원 허용")
    @ValueSource(longs = {1L, 2L, 999_999_999L, 1_000_000_000L})
    @DisplayName("TC-AMT-01, 04 하한 1원·상한 10억 포함")
    void acceptsAmountsInRange(long amount) {
        Payment payment = new Payment(amount, OCCURRED_AT);
        assertThat(payment.amount()).isEqualTo(amount);
        assertThat(payment.occurredAt()).isEqualTo(OCCURRED_AT);
    }

    @ParameterizedTest(name = "[{index}] {0}원 거부")
    @ValueSource(longs = {0L, -1L, 1_000_000_001L, 2_147_483_648L, Long.MAX_VALUE, Long.MIN_VALUE})
    @DisplayName("TC-AMT-02, 03, 05, 06 0·음수·상한 초과·int 초과는 INVALID_AMOUNT")
    void rejectsAmountsOutOfRange(long amount) {
        assertThatThrownBy(() -> new Payment(amount, OCCURRED_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INVALID_AMOUNT");
    }

    @Test
    @Tag(Basis.CODE)
    @DisplayName("[코드 분석] 발생 시각이 없으면 거부")
    void rejectsNullOccurredAt() {
        assertThatThrownBy(() -> new Payment(1_000, null))
                .isInstanceOf(RuntimeException.class);
    }
}
