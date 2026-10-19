package StatusCodecheck;

import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * For every URL in the "url" column of urls.csv, checks the HTTP status code in 3 views:
 *   1) DESKTOP : original URL with a desktop browser User-Agent
 *   2) MOBILE  : original URL with a mobile browser User-Agent
 *   3) AMP     : original URL + "/amp" with a mobile User-Agent
 *
 * Requires Java 17+ (uses records).
 * Compile : javac UrlStatusChecker.java
 * Run     : java UrlStatusChecker urls.csv results.csv
 */
public class UrlStatusChecker {

    // Domain swap: every URL in the input file that starts with OLD_DOMAIN is tested on NEW_DOMAIN.
    // Set both to the same value (or NEW_DOMAIN = "") to test URLs exactly as written.
    private static final String OLD_DOMAIN = "https://www.etnownews.com/";
    private static final String NEW_DOMAIN = "https://azpreprodnext.etnownews.com/";

    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    // Does NOT follow redirects -> real first status (200, 301, 404 ...)
    private static final HttpClient RAW_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    // Follows redirects -> final status
    private static final HttpClient FOLLOW_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    record ViewResult(String view, String requestedUrl, int initialStatus, int finalStatus,
                      String redirectTo, long millis, String error) {}

    record Row(String originalUrl, ViewResult desktop, ViewResult mobile, ViewResult amp) {}

    static ViewResult check(String view, String url, String userAgent) {
        long start = System.currentTimeMillis();
        try {
            HttpResponse<Void> first = RAW_CLIENT.send(
                    buildRequest(url, userAgent), HttpResponse.BodyHandlers.discarding());
            String location = first.headers().firstValue("location").orElse("");

            int finalStatus = first.statusCode();
            if (finalStatus >= 300 && finalStatus < 400) {
                HttpResponse<Void> last = FOLLOW_CLIENT.send(
                        buildRequest(url, userAgent), HttpResponse.BodyHandlers.discarding());
                finalStatus = last.statusCode();
            }
            return new ViewResult(view, url, first.statusCode(), finalStatus, location,
                    System.currentTimeMillis() - start, "");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ViewResult(view, url, -1, -1, "", 0, "Interrupted");
        } catch (Exception e) {
            return new ViewResult(view, url, -1, -1, "", System.currentTimeMillis() - start,
                    e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static HttpRequest buildRequest(String url, String userAgent) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .header("Accept", "text/html,application/xhtml+xml,*/*;q=0.8")
                .GET() // GET (body discarded) because some servers reject HEAD
                .build();
    }

    static String ampUrl(String url) {
        String u = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        return u + "/amp";
    }

    static Row checkAllViews(String url) {
        ViewResult desktop = check("DESKTOP", url, DESKTOP_UA);
        ViewResult mobile  = check("MOBILE",  url, MOBILE_UA);
        ViewResult amp     = check("AMP",     ampUrl(url), MOBILE_UA);
        return new Row(url, desktop, mobile, amp);
    }

    // ---------- CSV helpers ----------
    static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    static String csvPart(ViewResult r) {
        return String.join(",",
                String.valueOf(r.initialStatus()), String.valueOf(r.finalStatus()),
                q(r.redirectTo()), String.valueOf(r.millis()), q(r.error()));
    }

    static String csvRow(Row row) {
        return String.join(",",
                q(row.originalUrl()),
                csvPart(row.desktop()),
                csvPart(row.mobile()),
                q(row.amp().requestedUrl()),
                csvPart(row.amp()));
    }

    static final String CSV_HEADER = String.join(",",
            "url",
            "desktop_initial", "desktop_final", "desktop_redirect", "desktop_ms", "desktop_error",
            "mobile_initial", "mobile_final", "mobile_redirect", "mobile_ms", "mobile_error",
            "amp_url",
            "amp_initial", "amp_final", "amp_redirect", "amp_ms", "amp_error");

    static String label(ViewResult r) {
        return r.finalStatus() < 0 ? "ERR" : r.initialStatus() + (r.initialStatus() != r.finalStatus() ? ">" + r.finalStatus() : "");
    }

    // ---------- File reading (disk, then classpath, then search the project folder) ----------
    /**
     * Finds and reads a file. Order:
     *  1) the path exactly as given (absolute, or relative to the working directory)
     *  2) the classpath (target/classes, target/test-classes ...)
     *  3) a search of the project folder (skips target/.git/.idea), so it works even if the
     *     file sits in src/main/resources or src/test/resources and was never copied to target.
     */
    static List<String> readAllLinesFlexible(String name) throws java.io.IOException {
        Path p = Path.of(name);
        if (Files.exists(p)) {
            System.out.println("Input file: " + p.toAbsolutePath());
            return Files.readAllLines(p, java.nio.charset.StandardCharsets.UTF_8);
        }

        try (java.io.InputStream in = UrlStatusChecker.class.getClassLoader().getResourceAsStream(name)) {
            if (in != null) {
                System.out.println("Input file: found on classpath -> " + name);
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).lines().toList();
            }
        }

        Path root = Path.of("").toAbsolutePath();
        String fileName = p.getFileName().toString();
        try (java.util.stream.Stream<Path> walk = Files.walk(root, 8)) {
            java.util.Optional<Path> found = walk
                    .filter(f -> f.getFileName() != null && f.getFileName().toString().equals(fileName))
                    .filter(f -> {
                        String path = f.toString().replace('\\', '/');
                        return !path.contains("/target/") && !path.contains("/.git/") && !path.contains("/.idea/");
                    })
                    .filter(Files::isRegularFile)
                    .findFirst();
            if (found.isPresent()) {
                System.out.println("Input file: found by searching project -> " + found.get());
                return Files.readAllLines(found.get(), java.nio.charset.StandardCharsets.UTF_8);
            }
        }

        throw new java.io.FileNotFoundException("Could not find '" + name + "'. Looked in: "
                + p.toAbsolutePath() + ", the classpath, and under " + root
                + ". Pass the full path of the CSV as the first program argument.");
    }

    // ---------- CSV input ----------
    /** Reads the "url" column of a CSV file (falls back to the first column if there is no "url" header). */
    static List<String> readUrlsFromCsv(Path file) throws java.io.IOException {
        List<String> lines = readAllLinesFlexible(file.toString());
        List<String> urls = new ArrayList<>();
        if (lines.isEmpty()) return urls;

        // strip UTF-8 BOM (Excel adds it)
        String headerLine = lines.get(0).replace("\uFEFF", "");
        List<String> header = parseCsvLine(headerLine);
        int urlCol = 0;
        boolean hasHeader = false;
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).trim().equalsIgnoreCase("url")) { urlCol = i; hasHeader = true; break; }
        }

        for (int i = hasHeader ? 1 : 0; i < lines.size(); i++) {
            String line = i == 0 ? headerLine : lines.get(i);
            if (line.isBlank()) continue;
            List<String> cols = parseCsvLine(line);
            if (urlCol < cols.size()) urls.add(cols.get(urlCol));
        }
        return urls;
    }

    /** Minimal CSV line parser: handles commas, quoted fields and "" escapes. */
    static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') { sb.append('"'); i++; }
                else if (c == '"') inQuotes = false;
                else sb.append(c);
            } else if (c == '"') inQuotes = true;
            else if (c == ',') { out.add(sb.toString()); sb.setLength(0); }
            else sb.append(c);
        }
        out.add(sb.toString());
        return out;
    }

    // ---------- main ----------
    public static void main(String[] args) throws Exception {
        Path input = Path.of(args.length > 0 ? args[0] : "urls.csv");
        Path output = Path.of(args.length > 1 ? args[1] : "results.csv");

        List<String> urls = readUrlsFromCsv(input).stream()
                .map(String::trim)
                .filter(s -> s.startsWith("http"))
                .map(s -> NEW_DOMAIN.isEmpty() ? s : s.replace(OLD_DOMAIN, NEW_DOMAIN))
                .distinct()
                .toList();

        System.out.println("Checking " + urls.size() + " URLs x 3 views (desktop, mobile, amp)...\n");

        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Row>> futures = new ArrayList<>();
        for (String u : urls) {
            futures.add(pool.submit(() -> checkAllViews(u)));
        }

        int[] bad = {0, 0, 0}; // non-2xx counts for desktop, mobile, amp
        try (PrintWriter csv = new PrintWriter(output.toFile(), "UTF-8")) {
            csv.println(CSV_HEADER);

            System.out.printf("%-8s %-8s %-8s  %s%n", "DESKTOP", "MOBILE", "AMP", "URL");
            for (Future<Row> f : futures) {
                Row r = f.get();
                csv.println(csvRow(r));

                System.out.printf("%-8s %-8s %-8s  %s%n",
                        label(r.desktop()), label(r.mobile()), label(r.amp()), r.originalUrl());

                if (!is2xx(r.desktop())) bad[0]++;
                if (!is2xx(r.mobile()))  bad[1]++;
                if (!is2xx(r.amp()))     bad[2]++;
            }
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("\n===== SUMMARY (final status not 2xx) =====");
        System.out.println("Desktop : " + bad[0] + " / " + urls.size());
        System.out.println("Mobile  : " + bad[1] + " / " + urls.size());
        System.out.println("AMP     : " + bad[2] + " / " + urls.size());
        System.out.println("\nCSV saved to: " + output.toAbsolutePath());
    }

    static boolean is2xx(ViewResult r) {
        return r.finalStatus() >= 200 && r.finalStatus() < 300;
    }
}