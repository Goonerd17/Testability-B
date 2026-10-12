package com.testability.lab.billing;

/**
 * 테스트 작성 근거 태그 (docs/EXPERIMENT_B_TEST_RESULTS.md §3).
 *
 * <ul>
 *   <li>{@link #JEV} — Phase 4 Jev 환산 순위 상위 6개 메서드(M06, M07, M08, M10, M09, M01)를 직접 겨냥한 테스트.
 *       Jev는 메서드 우선순위만 제공했고 개별 테스트 케이스를 추천하지 않았다.</li>
 *   <li>{@link #SPEC} — BUSINESS_RULES / TEST_CASE_MATRIX의 필수 규칙·TC를 근거로 한 테스트.</li>
 *   <li>{@link #CODE} — 매트릭스에 없고 Claude가 코드 분석 중 찾은 추가 위험을 겨냥한 테스트.</li>
 *   <li>{@link #OPEN_PREMISE} — 승인 대기 중인 OPEN 항목(EXPERIMENT_SCOPE §5의 실험용 임시 전제)에 기대값이 의존하는 테스트.</li>
 * </ul>
 */
final class Basis {

    static final String JEV = "basis-jev";
    static final String SPEC = "basis-spec";
    static final String CODE = "basis-code";
    static final String OPEN_PREMISE = "open-premise";

    private Basis() {
    }
}
