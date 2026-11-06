# Oracle OCI Cost Guard

Java 17 reference implementation for OCI cost governance: daily spend anomalies,
tag coverage, forecast, budget burn and risk recommendations.

## Why

OCI cost surprises usually have two causes: untagged resources that cannot be
attributed to a team, and usage spikes that are invisible until the invoice.
This service turns daily service spend into a guard result before the billing
cycle closes.

## Stack

- Java 17 (`com.sun.net.httpserver`)
- Gradle 8
- JUnit 5
- Oracle-inspired UI tokens: `OracleSans`, Georgia headings, `#161513`, `#C74634`,
  `#EFEFEF`, 4px radius

## Run

```bash
./gradlew test
./gradlew run
# http://127.0.0.1:5181
```

JSON:

```bash
curl http://127.0.0.1:5181/api/cost-guard
```

## Guard logic

- Population mean and standard deviation of prior daily totals.
- Latest-day z-score; `|z| >= 2` is an anomaly.
- Tagged spend / total spend.
- Forecast = average daily spend × remaining days.
- Burn rate = month-to-date spend / monthly budget.
- Risk = high when burn > 100%, anomaly, or tag coverage < 70%; medium above
  80% burn or below 90% coverage.
- Recommendations for spike review, tag enforcement, budget freeze, forecast
  review.

## Reference fixtures

The included six-day fixture produces a 42.43 z-score, 55.56% tag coverage,
75% burn and a high-risk result, which exercises the intervention path.

## Scope

This is a reference implementation, not a production OCI tenancy integration.
It does not call OCI APIs or claim Oracle production usage.
