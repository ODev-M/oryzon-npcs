package cv.oryzon.npcs.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiny dependency-free JSON store. Atomic via write-temp + rename. Schema is
 * stable enough that swapping the backing implementation for ZeroBase in M3b
 * doesn't change the public surface or call sites.
 *
 * <p>We accept that JSON parsing here is hand-rolled — values written by the
 * plugin are always controlled (regex-validated ids, world names, base64 skin
 * payloads). If a user hand-edits the file and breaks it, we log + start fresh
 * rather than crashing the plugin.
 */
public final class JsonFileStore implements NpcStore {

    private final Path file;
    private final Logger logger;
    private final Map<String, NpcRecord> records = new LinkedHashMap<>();

    public JsonFileStore(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
        load();
    }

    @Override
    public Collection<NpcRecord> loadAll() {
        return new ArrayList<>(records.values());
    }

    @Override
    public synchronized void save(NpcRecord record) {
        records.put(record.id().toLowerCase(Locale.ROOT), record);
        flush();
    }

    @Override
    public synchronized void delete(String id) {
        if (records.remove(id.toLowerCase(Locale.ROOT)) != null) {
            flush();
        }
    }

    @Override
    public void close() {
        // Nothing to release; flush() is synchronous on every mutation.
    }

    // ----------------------------------------------------------------- IO

    private void load() {
        if (!Files.exists(file)) return;
        try {
            String body = Files.readString(file).trim();
            if (body.isEmpty() || body.equals("[]")) return;
            for (NpcRecord record : parse(body)) {
                records.put(record.id().toLowerCase(Locale.ROOT), record);
            }
        } catch (IOException | RuntimeException e) {
            logger.log(Level.WARNING, "Could not read " + file
                    + " — starting with an empty NPC store. Reason: " + e.getMessage());
        }
    }

    private void flush() {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, render(records.values()));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to persist NPCs to " + file, e);
        }
    }

    // ----------------------------------------------------------------- write

    private static String render(Collection<NpcRecord> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        boolean first = true;
        for (NpcRecord r : records) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  {");
            field(sb, "id", r.id(), true);
            field(sb, "name", r.name(), false);
            field(sb, "world", r.world(), false);
            number(sb, "x", r.x());
            number(sb, "y", r.y());
            number(sb, "z", r.z());
            number(sb, "yaw", r.yaw());
            number(sb, "pitch", r.pitch());
            field(sb, "skinName", r.skinName(), false);
            field(sb, "skinValue", r.skinValue(), false);
            field(sb, "skinSignature", r.skinSignature(), false);
            sb.append("}");
        }
        sb.append("\n]\n");
        return sb.toString();
    }

    private static void field(StringBuilder sb, String key, String value, boolean first) {
        if (!first) sb.append(", ");
        sb.append('"').append(key).append("\":\"").append(escape(value == null ? "" : value)).append('"');
    }

    private static void number(StringBuilder sb, String key, double value) {
        sb.append(", \"").append(key).append("\":").append(value);
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 4);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    // ----------------------------------------------------------------- read

    /** Minimal JSON-array-of-objects parser. Tolerant of whitespace; strict on shape. */
    private static List<NpcRecord> parse(String body) {
        List<NpcRecord> out = new ArrayList<>();
        Cursor c = new Cursor(body);
        c.skipWs();
        c.expect('[');
        c.skipWs();
        if (c.peek() == ']') { c.advance(); return out; }
        while (true) {
            c.skipWs();
            out.add(parseObject(c));
            c.skipWs();
            char nx = c.peek();
            if (nx == ',') { c.advance(); continue; }
            if (nx == ']') { c.advance(); break; }
            throw new IllegalStateException("Expected , or ] at index " + c.pos);
        }
        return out;
    }

    private static NpcRecord parseObject(Cursor c) {
        c.expect('{');
        Map<String, Object> fields = new LinkedHashMap<>();
        c.skipWs();
        if (c.peek() == '}') { c.advance(); return toRecord(fields); }
        while (true) {
            c.skipWs();
            String key = parseString(c);
            c.skipWs();
            c.expect(':');
            c.skipWs();
            Object value = c.peek() == '"' ? parseString(c) : parseNumber(c);
            fields.put(key, value);
            c.skipWs();
            char nx = c.peek();
            if (nx == ',') { c.advance(); continue; }
            if (nx == '}') { c.advance(); break; }
            throw new IllegalStateException("Expected , or } at index " + c.pos);
        }
        return toRecord(fields);
    }

    private static NpcRecord toRecord(Map<String, Object> f) {
        return new NpcRecord(
                str(f, "id"),
                str(f, "name"),
                str(f, "world"),
                dbl(f, "x"),
                dbl(f, "y"),
                dbl(f, "z"),
                (float) dbl(f, "yaw"),
                (float) dbl(f, "pitch"),
                str(f, "skinName"),
                str(f, "skinValue"),
                str(f, "skinSignature"));
    }

    private static String str(Map<String, Object> f, String key) {
        Object v = f.get(key);
        return v == null ? "" : v.toString();
    }

    private static double dbl(Map<String, Object> f, String key) {
        Object v = f.get(key);
        if (v == null) return 0d;
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
    }

    private static String parseString(Cursor c) {
        c.expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            char ch = c.advance();
            if (ch == '"') return out.toString();
            if (ch == '\\') {
                char esc = c.advance();
                switch (esc) {
                    case '"'  -> out.append('"');
                    case '\\' -> out.append('\\');
                    case '/'  -> out.append('/');
                    case 'n'  -> out.append('\n');
                    case 'r'  -> out.append('\r');
                    case 't'  -> out.append('\t');
                    case 'u'  -> {
                        String hex = c.advance() + "" + c.advance() + c.advance() + c.advance();
                        out.append((char) Integer.parseInt(hex, 16));
                    }
                    default -> throw new IllegalStateException("Bad escape \\" + esc);
                }
            } else {
                out.append(ch);
            }
        }
    }

    private static Number parseNumber(Cursor c) {
        StringBuilder sb = new StringBuilder();
        while (c.pos < c.body.length()) {
            char ch = c.peek();
            if (Character.isDigit(ch) || ch == '-' || ch == '+' || ch == '.' || ch == 'e' || ch == 'E') {
                sb.append(ch);
                c.advance();
            } else break;
        }
        return Double.parseDouble(sb.toString());
    }

    private static final class Cursor {
        final String body;
        int pos;

        Cursor(String body) { this.body = body; }

        char peek() { return pos < body.length() ? body.charAt(pos) : '\0'; }
        char advance() { return body.charAt(pos++); }
        void expect(char c) {
            if (peek() != c) throw new IllegalStateException(
                    "Expected '" + c + "' at index " + pos + ", got '" + peek() + "'");
            pos++;
        }
        void skipWs() {
            while (pos < body.length() && Character.isWhitespace(body.charAt(pos))) pos++;
        }
    }
}
