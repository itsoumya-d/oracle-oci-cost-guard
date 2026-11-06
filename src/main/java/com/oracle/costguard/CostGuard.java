package com.oracle.costguard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class CostGuard {
    public record DailyCost(LocalDate date, String service, long cents, boolean tagged) {}
    public record GuardResult(
            double averageCents,
            double standardDeviationCents,
            double latestCents,
            double zScore,
            boolean anomaly,
            double tagCoveragePercent,
            double forecastCents,
            double burnRate,
            String risk,
            List<String> recommendations) {}

    private CostGuard() {}

    public static double mean(List<Double> values) {
        if (values.isEmpty()) return 0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    public static double standardDeviation(List<Double> values) {
        if (values.isEmpty()) return 0;
        double mean = mean(values);
        double variance = values.stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    public static double zScore(double value, double mean, double standardDeviation) {
        if (standardDeviation <= 0) return 0;
        return (value - mean) / standardDeviation;
    }

    public static boolean isAnomaly(double zScore) {
        return Math.abs(zScore) >= 2.0;
    }

    public static double tagCoveragePercent(long taggedCents, long totalCents) {
        if (totalCents <= 0) return 0;
        return Math.round((taggedCents * 10000.0 / totalCents)) / 100.0;
    }

    public static double forecastCents(double averageCents, int remainingDays) {
        return Math.round(averageCents * Math.max(0, remainingDays) * 100.0) / 100.0;
    }

    public static double burnRate(long monthToDateCents, long budgetCents) {
        if (budgetCents <= 0) return 0;
        return Math.round((monthToDateCents * 10000.0 / budgetCents)) / 100.0;
    }

    public static String riskLevel(double burnRate, boolean anomaly, double tagCoveragePercent) {
        if (burnRate > 100 || anomaly || tagCoveragePercent < 70) return "high";
        if (burnRate > 80 || tagCoveragePercent < 90) return "medium";
        return "low";
    }

    public static GuardResult guard(
            List<DailyCost> costs,
            long budgetCents,
            int dayOfMonth,
            int daysInMonth,
            int remainingDays) {
        if (costs.isEmpty()) {
            return new GuardResult(0, 0, 0, 0, false, 0, 0, 0, "low",
                    List.of("No cost data for this period."));
        }
        List<DailyCost> sorted = costs.stream()
                .sorted(Comparator.comparing(DailyCost::date))
                .toList();
        DailyCost latest = sorted.get(sorted.size() - 1);
        List<DailyCost> history = sorted.subList(0, sorted.size() - 1);
        List<Double> dailyTotals = new ArrayList<>();
        for (DailyCost cost : history) dailyTotals.add((double) cost.cents());
        double average = mean(dailyTotals);
        double deviation = standardDeviation(dailyTotals);
        double z = zScore(latest.cents(), average, deviation);
        boolean anomaly = isAnomaly(z);
        long tagged = sorted.stream().filter(DailyCost::tagged).mapToLong(DailyCost::cents).sum();
        long total = sorted.stream().mapToLong(DailyCost::cents).sum();
        double coverage = tagCoveragePercent(tagged, total);
        double forecast = forecastCents(average, remainingDays);
        double burn = burnRate(total, budgetCents);
        String risk = riskLevel(burn, anomaly, coverage);

        List<String> recommendations = new ArrayList<>();
        if (anomaly) recommendations.add("Investigate the latest daily cost spike against service-level usage.");
        if (coverage < 90) recommendations.add("Enforce cost-tracking tags before the next billing cycle.");
        if (burn > 100) recommendations.add("Freeze non-critical spend; the monthly budget is exceeded.");
        else if (burn > 80) recommendations.add("Review the forecast and reduce non-critical spend.");
        if (forecast > budgetCents && budgetCents > 0) recommendations.add("Forecast exceeds budget; open a FinOps review.");
        if (recommendations.isEmpty()) recommendations.add("No action required; spend is within guardrails.");

        return new GuardResult(
                Math.round(average * 100.0) / 100.0,
                Math.round(deviation * 100.0) / 100.0,
                latest.cents(),
                Math.round(z * 100.0) / 100.0,
                anomaly,
                coverage,
                forecast,
                burn,
                risk,
                recommendations);
    }

    public static String formatCents(double cents) {
        return String.format(Locale.US, "$%,.2f", cents / 100.0);
    }
}
