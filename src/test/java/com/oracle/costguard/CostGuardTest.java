package com.oracle.costguard;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CostGuardTest {
    private static CostGuard.DailyCost cost(int day, long cents, boolean tagged) {
        return new CostGuard.DailyCost(LocalDate.of(2026, 11, day), "Compute", cents, tagged);
    }

    private static final List<CostGuard.DailyCost> COSTS = List.of(
            cost(1, 100, true), cost(2, 110, true), cost(3, 90, true),
            cost(4, 105, true), cost(5, 95, true), cost(6, 400, false));

    @Test void calculatesMean() { assertEquals(100, CostGuard.mean(List.of(80.0, 100.0, 120.0)), 0.001); }
    @Test void calculatesStandardDeviation() { assertEquals(16.33, CostGuard.standardDeviation(List.of(80.0, 100.0, 120.0)), 0.01); }
    @Test void returnsZeroMeanForEmpty() { assertEquals(0, CostGuard.mean(List.of())); }
    @Test void returnsZeroDeviationForEmpty() { assertEquals(0, CostGuard.standardDeviation(List.of())); }
    @Test void calculatesPositiveZ() { assertEquals(2, CostGuard.zScore(120, 100, 10), 0.001); }
    @Test void calculatesNegativeZ() { assertEquals(-2, CostGuard.zScore(80, 100, 10), 0.001); }
    @Test void returnsZeroZForZeroDeviation() { assertEquals(0, CostGuard.zScore(100, 100, 0)); }
    @Test void flagsAnomalyAtTwo() { assertTrue(CostGuard.isAnomaly(2.0)); }
    @Test void flagsAnomalyBelowNegativeTwo() { assertTrue(CostGuard.isAnomaly(-2.1)); }
    @Test void doesNotFlagNormalValue() { assertFalse(CostGuard.isAnomaly(1.99)); }
    @Test void calculatesTagCoverage() { assertEquals(75.0, CostGuard.tagCoveragePercent(75, 100), 0.001); }
    @Test void returnsZeroTagCoverageForZeroSpend() { assertEquals(0, CostGuard.tagCoveragePercent(0, 0)); }
    @Test void forecastsSpend() { assertEquals(300.0, CostGuard.forecastCents(100, 3), 0.001); }
    @Test void forecastNeverNegative() { assertEquals(0, CostGuard.forecastCents(100, -1)); }
    @Test void calculatesBurnRate() { assertEquals(80.0, CostGuard.burnRate(80, 100), 0.001); }
    @Test void returnsZeroBurnForZeroBudget() { assertEquals(0, CostGuard.burnRate(80, 0)); }
    @Test void marksHighRiskForOverspend() { assertEquals("high", CostGuard.riskLevel(101, false, 95)); }
    @Test void marksHighRiskForAnomaly() { assertEquals("high", CostGuard.riskLevel(50, true, 95)); }
    @Test void marksHighRiskForLowTagCoverage() { assertEquals("high", CostGuard.riskLevel(50, false, 69)); }
    @Test void marksMediumRisk() { assertEquals("medium", CostGuard.riskLevel(85, false, 95)); }
    @Test void marksLowRisk() { assertEquals("low", CostGuard.riskLevel(50, false, 95)); }
    @Test void buildsGuardResult() {
        var result = CostGuard.guard(COSTS, 1200, 6, 30, 24);
        assertEquals(100, result.averageCents(), 0.001);
        assertEquals(400, result.latestCents());
        assertTrue(result.anomaly());
        assertEquals("high", result.risk());
    }
    @Test void calculatesTagCoverageInGuard() {
        var result = CostGuard.guard(COSTS, 1200, 6, 30, 24);
        assertEquals(55.56, result.tagCoveragePercent(), 0.01);
    }
    @Test void calculatesForecastInGuard() {
        var result = CostGuard.guard(COSTS, 1200, 6, 30, 24);
        assertEquals(2400, result.forecastCents(), 0.01);
    }
    @Test void calculatesBurnInGuard() {
        var result = CostGuard.guard(COSTS, 1200, 6, 30, 24);
        assertEquals(75, result.burnRate(), 0.01);
    }
    @Test void recommendsSpikeReview() {
        var result = CostGuard.guard(COSTS, 1200, 6, 30, 24);
        assertTrue(result.recommendations().stream().anyMatch(item -> item.contains("spike")));
    }
    @Test void recommendsTagEnforcement() {
        var result = CostGuard.guard(COSTS, 1200, 6, 30, 24);
        assertTrue(result.recommendations().stream().anyMatch(item -> item.contains("tags")));
    }
    @Test void handlesEmptyCosts() {
        var result = CostGuard.guard(List.of(), 1200, 6, 30, 24);
        assertEquals("low", result.risk());
        assertEquals(0, result.averageCents());
        assertFalse(result.anomaly());
    }
    @Test void formatsCents() { assertEquals("$12.34", CostGuard.formatCents(1234)); }
}
