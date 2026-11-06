package com.oracle.costguard;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public final class App {
    private App() {}

    public static void main(String[] args) throws IOException {
        List<CostGuard.DailyCost> costs = List.of(
                new CostGuard.DailyCost(LocalDate.of(2026, 11, 1), "Compute", 42_000, true),
                new CostGuard.DailyCost(LocalDate.of(2026, 11, 2), "Compute", 43_800, true),
                new CostGuard.DailyCost(LocalDate.of(2026, 11, 3), "Storage", 39_200, true),
                new CostGuard.DailyCost(LocalDate.of(2026, 11, 4), "Compute", 45_100, true),
                new CostGuard.DailyCost(LocalDate.of(2026, 11, 5), "Database", 47_000, false),
                new CostGuard.DailyCost(LocalDate.of(2026, 11, 6), "Compute", 98_000, false));
        CostGuard.GuardResult result = CostGuard.guard(costs, 1_200_000, 6, 30, 24);

        HttpServer server = HttpServer.create(new InetSocketAddress(5181), 0);
        server.createContext("/api/cost-guard", exchange -> {
            String json = """
                    {"averageCents":%.2f,"standardDeviationCents":%.2f,"latestCents":%.2f,"zScore":%.2f,"anomaly":%s,"tagCoveragePercent":%.2f,"forecastCents":%.2f,"burnRate":%.2f,"risk":"%s"}
                    """.formatted(result.averageCents(), result.standardDeviationCents(), result.latestCents(), result.zScore(), result.anomaly(), result.tagCoveragePercent(), result.forecastCents(), result.burnRate(), result.risk());
            respond(exchange, "application/json", json);
        });
        server.createContext("/", exchange -> respond(exchange, "text/html", dashboard(result, costs)));
        server.start();
        System.out.println("OCI Cost Guard on http://127.0.0.1:5181");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) { stream.write(bytes); }
    }

    private static String dashboard(CostGuard.GuardResult r, List<CostGuard.DailyCost> costs) {
        String rows = costs.stream().map(cost -> """
                <tr><td>%s</td><td>%s</td><td class="num">%s</td><td><span class="badge %s">%s</span></td></tr>
                """.formatted(cost.date(), cost.service(), CostGuard.formatCents(cost.cents()), cost.tagged() ? "ok" : "warn", cost.tagged() ? "tagged" : "untagged")).reduce("", String::concat);
        String recs = r.recommendations().stream().map(item -> "<li>" + item + "</li>").reduce("", String::concat);
        return """
                <!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>OCI Cost Guard</title>
                <style>
                :root{--red:#C74634;--ink:#161513;--surface:#EFEFEF;--muted:#5E5A57;--line:#D8D2CC;--white:#fff;--radius:4px}
                *{box-sizing:border-box}body{margin:0;background:var(--white);color:var(--ink);font-family:OracleSans,ui-sans-serif,Arial,sans-serif;line-height:1.5}
                .shell{max-width:1180px;margin:0 auto;padding:40px 28px}.masthead{display:flex;justify-content:space-between;gap:32px;padding-bottom:24px;border-bottom:4px solid var(--red)}
                .eyebrow{margin:0 0 8px;color:var(--red);font-size:11px;font-weight:800;letter-spacing:.14em;text-transform:uppercase}h1,h2{margin:0}h1{font-family:Georgia,serif;font-size:clamp(34px,4.4vw,58px);line-height:.98;letter-spacing:-.04em}h2{font-size:18px}
                .lede{max-width:670px;margin:14px 0 0;color:var(--muted);font-size:16px}.mast-meta{display:flex;flex-direction:column;align-items:flex-end;gap:6px;color:var(--muted);font-size:11px;font-weight:700;letter-spacing:.12em;text-transform:uppercase}
                .metrics{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin:24px 0}.metric{padding:16px;background:var(--surface);border-radius:var(--radius);border-top:3px solid var(--ink)}.metric span{display:block;color:var(--muted);font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.06em}.metric strong{display:block;margin-top:8px;font-size:22px}.metric.red{border-top-color:var(--red)}.metric.red strong{color:var(--red)}
                .grid{display:grid;grid-template-columns:1.5fr 1fr;gap:16px}.panel{border:1px solid var(--line);border-radius:var(--radius);background:var(--white);overflow:hidden}.panel-head{display:flex;justify-content:space-between;align-items:baseline;gap:12px;padding:15px 18px;border-bottom:1px solid var(--line);background:#FAF9F7}.panel-head span{color:var(--muted);font-size:10px;font-weight:700;text-transform:uppercase;letter-spacing:.06em}
                table{width:100%%;border-collapse:collapse;text-align:left}th{padding:10px 14px;color:var(--muted);font-size:10px;text-transform:uppercase;letter-spacing:.06em;border-bottom:1px solid var(--line)}td{padding:12px 14px;border-bottom:1px solid #E8E2DD}tr:last-child td{border-bottom:0}.num{text-align:right;font-variant-numeric:tabular-nums}.badge{display:inline-block;padding:3px 7px;border-radius:999px;font-size:9px;font-weight:800;text-transform:uppercase}.badge.ok{background:#E7F5EF;color:#087A2C}.badge.warn{background:#FFF1D6;color:#A66A00}
                .side .risk{padding:18px}.risk strong{display:block;font-family:Georgia,serif;font-size:38px;color:var(--red)}.risk p{color:var(--muted);font-size:12px}.side ul{list-style:none;margin:0;padding:0}.side li{padding:11px 18px;border-top:1px solid var(--line);font-size:12px}.callout{margin:14px 18px 18px;padding:12px;background:#FBEDEA;border-left:4px solid var(--red)}.callout strong{display:block;color:var(--red);font-size:10px;text-transform:uppercase;letter-spacing:.05em}.callout p{margin:5px 0 0;color:#4A3F3A;font-size:12px}
                footer{display:flex;justify-content:space-between;gap:16px;margin-top:24px;padding-top:16px;border-top:1px solid var(--line);color:var(--muted);font-size:12px}
                @media(max-width:980px){.metrics{grid-template-columns:repeat(3,1fr)}.grid{grid-template-columns:1fr}.masthead{flex-direction:column}.mast-meta{flex-direction:row;align-items:flex-start;gap:16px}}
                </style></head><body><div class="shell"><header class="masthead"><div><p class="eyebrow">Oracle · OCI cost guard</p><h1>Cloud spend, before it surprises you.</h1><p class="lede">Daily cost anomalies, tag coverage, forecast and budget burn for OCI services.</p></div><div class="mast-meta"><span>OCI</span><span>FinOps</span><span>Java 17</span></div></header>
                <section class="metrics"><div class="metric"><span>Average/day</span><strong>%s</strong></div><div class="metric"><span>Latest/day</span><strong>%s</strong></div><div class="metric red"><span>Z-score</span><strong>%.2f</strong></div><div class="metric"><span>Tag coverage</span><strong>%.1f%%</strong></div><div class="metric red"><span>Burn rate</span><strong>%.1f%%</strong></div></section>
                <main class="grid"><section class="panel"><div class="panel-head"><h2>Daily spend</h2><span>%d entries</span></div><table><thead><tr><th>Date</th><th>Service</th><th class="num">Cost</th><th>Tag</th></tr></thead><tbody>%s</tbody></table></section>
                <aside class="panel side"><div class="panel-head"><h2>Guard result</h2><span>Java 17</span></div><div class="risk"><strong>%s risk</strong><p>Forecast %s · burn %.1f%% · anomaly %s</p></div><ul>%s</ul><div class="callout"><strong>Intervention</strong><p>Investigate spikes, enforce cost tags and review forecast before the next billing cycle.</p></div></aside></main>
                <footer><span>Java 17 · HttpServer · JUnit 5</span><span>Reference implementation for Oracle OCI cost governance</span></footer></div></body></html>
                """.formatted(CostGuard.formatCents(r.averageCents()), CostGuard.formatCents(r.latestCents()), r.zScore(), r.tagCoveragePercent(), r.burnRate(), costs.size(), rows, r.risk(), CostGuard.formatCents(r.forecastCents()), r.burnRate(), r.anomaly(), recs);
    }
}
