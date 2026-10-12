package com.testability.lab.billing;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 테스트 데이터 도우미. 시각은 오프셋을 포함한 ISO-8601 문자열로 받아
 * 프로덕션 코드의 시간대 변환을 거치지 않고 Instant로 만든다.
 */
final class Fixtures {

    /** TEST_CASE_MATRIX §0 기본 정산 요청 시각 */
    static final String DEFAULT_REQUEST_AT = "2026-08-10T10:00:00+09:00";

    private Fixtures() {
    }

    static Instant at(String isoOffsetDateTime) {
        return OffsetDateTime.parse(isoOffsetDateTime).toInstant();
    }

    static Payment payment(String isoOffsetDateTime, long amount) {
        return new Payment(amount, at(isoOffsetDateTime));
    }

    /** 고정 시계. 시계 자체의 시간대는 UTC로 두어 프로덕션 코드가 시계 시간대에 의존하지 않는지도 함께 확인한다. */
    static Clock clockAt(String isoOffsetDateTime) {
        return Clock.fixed(at(isoOffsetDateTime), ZoneOffset.UTC);
    }

    static FeeCalculator calculatorAt(String isoOffsetDateTime) {
        return new FeeCalculator(clockAt(isoOffsetDateTime));
    }

    static FeeCalculator defaultCalculator() {
        return calculatorAt(DEFAULT_REQUEST_AT);
    }
}
