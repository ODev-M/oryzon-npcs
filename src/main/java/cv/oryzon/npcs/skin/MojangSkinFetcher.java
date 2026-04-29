package cv.oryzon.npcs.skin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches Mojang-signed skin textures by player name. We hit two public
 * endpoints in order:
 *
 * <ol>
 *   <li>{@code api.mojang.com/users/profiles/minecraft/<name>} → UUID</li>
 *   <li>{@code sessionserver.mojang.com/session/minecraft/profile/<uuid>?unsigned=false}
 *       → base64 textures + signature</li>
 * </ol>
 *
 * <p>Results live in an in-memory cache keyed by lowercase name. Mojang rate-limits
 * heavily (~1 req/s per IP), so callers are expected to batch and reuse the cache.
 * Persisting across restarts is M3's problem (ZeroBase).
 */
public final class MojangSkinFetcher {

    private static final URI PROFILE_URL =
            URI.create("https://api.mojang.com/users/profiles/minecraft/");
    private static final String SESSION_URL =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final Pattern UUID_PATTERN =
            Pattern.compile("\"id\"\\s*:\\s*\"([a-fA-F0-9]{32})\"");
    private static final Pattern VALUE_PATTERN =
            Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIGNATURE_PATTERN =
            Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private final Logger logger;
    private final HttpClient http;
    private final Map<String, Skin> cache = new ConcurrentHashMap<>();

    public MojangSkinFetcher(Logger logger) {
        this.logger = logger;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public CompletableFuture<Skin> fetch(String name) {
        String key = name.toLowerCase();
        Skin cached = cache.get(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return CompletableFuture.supplyAsync(() -> resolve(name))
                .thenApply(skin -> {
                    cache.put(key, skin);
                    return skin;
                });
    }

    public void invalidate(String name) {
        cache.remove(name.toLowerCase());
    }

    private Skin resolve(String name) {
        try {
            UUID uuid = lookupUuid(name);
            if (uuid == null) {
                logger.warning("Mojang has no profile for \"" + name + "\".");
                return Skin.EMPTY;
            }
            return lookupTextures(uuid);
        } catch (IOException | InterruptedException e) {
            logger.warning("Skin fetch failed for \"" + name + "\": " + e.getMessage());
            return Skin.EMPTY;
        }
    }

    private UUID lookupUuid(String name) throws IOException, InterruptedException {
        HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(PROFILE_URL.resolve(name)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        Matcher m = UUID_PATTERN.matcher(resp.body());
        if (!m.find()) return null;
        String hex = m.group(1);
        // Mojang returns the UUID without dashes; rebuild canonical form.
        String dashed = hex.substring(0, 8) + "-"
                + hex.substring(8, 12) + "-"
                + hex.substring(12, 16) + "-"
                + hex.substring(16, 20) + "-"
                + hex.substring(20, 32);
        return UUID.fromString(dashed);
    }

    private Skin lookupTextures(UUID uuid) throws IOException, InterruptedException {
        HttpResponse<String> resp = http.send(
                HttpRequest.newBuilder(URI.create(SESSION_URL + uuid + "?unsigned=false"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return Skin.EMPTY;
        Matcher v = VALUE_PATTERN.matcher(resp.body());
        Matcher s = SIGNATURE_PATTERN.matcher(resp.body());
        if (!v.find() || !s.find()) return Skin.EMPTY;
        return new Skin(v.group(1), s.group(1));
    }
}
