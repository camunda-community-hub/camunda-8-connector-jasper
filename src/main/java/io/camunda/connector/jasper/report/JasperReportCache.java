package io.camunda.connector.jasper.report;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe cache of compiled JasperReport instances, keyed by the SHA-256 hash
 * of the JRXML content (content-addressed, not filename).
 *
 * <p>Each entry records when it was compiled. Entries older than MAX_AGE are
 * evicted by purgeCache(), which is called automatically on every getJasperReport()
 * call.
 *
 * <p>Thread-safety notes:
 * <ul>
 *   <li>purgeCache() uses ConcurrentHashMap.entrySet().removeIf(), which is
 *       thread-safe by the ConcurrentHashMap contract — no extra lock is needed.</li>
 *   <li>getJasperReport() uses double-checked locking (synchronized on the map
 *       object) to ensure a given JRXML is compiled at most once even under
 *       concurrent access.</li>
 * </ul>
 */
public class JasperReportCache {

    private static final Logger logger = LoggerFactory.getLogger(JasperReportCache.class);
    private static final Duration MAX_AGE = Duration.ofHours(1);

    private record CacheEntry(JasperReport report, Instant compiledAt) {}

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Returns a compiled JasperReport for the given JRXML bytes, compiling on cache miss.
     * Triggers purgeCache() before every lookup.
     *
     * @param reportName used only for log messages
     * @param jrxmlBytes raw JRXML file content
     */
    public JasperReport getJasperReport(String reportName, byte[] jrxmlBytes) throws JRException {
        purgeCache();

        String cacheKey = contentHash(jrxmlBytes);

        CacheEntry entry = cache.get(cacheKey);
        if (entry != null) {
            logger.info("[{}] using cached compiled report (hash={})", reportName, cacheKey.substring(0, 8));
            return entry.report();
        }

        // Double-checked locking: only one thread compiles a given JRXML at a time.
        synchronized (cache) {
            entry = cache.get(cacheKey);
            if (entry != null) {
                return entry.report();
            }
            logger.info("[{}] cache miss (hash={}), compiling...", reportName, cacheKey.substring(0, 8));
            JasperReport report = JasperCompileManager.compileReport(new ByteArrayInputStream(jrxmlBytes));
            cache.put(cacheKey, new CacheEntry(report, Instant.now()));
            return report;
        }
    }

    /**
     * Removes all cached entries whose compiledAt timestamp is older than MAX_AGE (1 hour).
     * ConcurrentHashMap.entrySet().removeIf() is thread-safe; no additional lock is required.
     */
    public void purgeCache() {
        Instant cutoff = Instant.now().minus(MAX_AGE);
        AtomicInteger removed = new AtomicInteger(0);
        cache.entrySet().removeIf(e -> {
            if (e.getValue().compiledAt().isBefore(cutoff)) {
                removed.incrementAndGet();
                return true;
            }
            return false;
        });
        if (removed.get() > 0) {
            logger.info("JasperReportCache: purged {} expired compiled report(s) (older than 1 hour)", removed.get());
        }
    }

    /** SHA-256 content hash used as the cache key. */
    private static String contentHash(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
