package cv.oryzon.npcs.store;

import cv.oryzon.npcs.action.Action;
import cv.oryzon.npcs.action.ClickType;

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
 * Tiny dependency-free JSON store. Atomic via write-temp + rename. The parser
 * supports objects, strings, numbers and arrays of objects — enough for the
 * NPC + actions schema we control. Hand-edited files that don't conform get
 * logged and discarded, never crash the plugin.
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

    @Override public Collection<NpcRecord> loadAll() { return new ArrayList<>(records.values()); }

    @Override public synchronized void save(NpcRecord record) {
        records.put(record.id().toLowerCase(Locale.ROOT), record);
        flush();
    }

    @Override public synchronized void delete(String id) {
        if (records.remove(id.toLowerCase(Locale.ROOT)) != null) flush();
    }

    @Override public void close() {}

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
            sb.append(", \"actions\":[");
            boolean firstAction = true;
            for (Action a : r.actions()) {
                if (!firstAction) sb.append(", ");
                firstAction = false;
                sb.append("{");
                field(sb, "type", a.type().name(), true);
                field(sb, "click", a.click().name(), false);
                field(sb, "value", a.value() == null ? "" : a.value(), false);
                field(sb, "password", a.password() == null ? "" : a.password(), false);
                sb.append("}");
            }
            sb.append("]}");
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

    private static List<NpcRecord> parse(String body) {
        List<NpcRecord> out = new ArrayList<>();
        Cursor c = new Cursor(body);
        c.skipWs(); c.expect('['); c.skipWs();
        if (c.peek() == ']') { c.advance(); return out; }
        while (true) {
            c.skipWs();
            out.add(toRecord(parseObject(c)));
            c.skipWs();
            char nx = c.peek();
            if (nx == ',') { c.advance(); continue; }
            if (nx == ']') { c.advance(); break; }
            throw new IllegalStateException("Expected , or ] at index " + c.pos);
        }
        return out;
    }

    private static Map<String, Object> parseObject(Cursor c) {
        c.expect('{');
        Map<String, Object> fields = new LinkedHashMap<>();
        c.skipWs();
        if (c.peek() == '}') { c.advance(); return fields; }
        while (true) {
            c.skipWs();
            String key = parseString(c);
            c.skipWs(); c.expect(':'); c.skipWs();
            fields.put(key, parseValue(c));
            c.skipWs();
            char nx = c.peek();
            if (nx == ',') { c.advance(); continue; }
            if (nx == '}') { c.advance(); break; }
            throw new IllegalStateException("Expected , or } at index " + c.pos);
        }
        return fields;
    }

    private static Object parseValue(Cursor c) {
        char ch = c.peek();
        if (ch == '"') return parseString(c);
        if (ch == '[') return parseArray(c);
        if (ch == '{') return parseObject(c);
        return parseNumber(c);
    }

    private static List<Object> parseArray(Cursor c) {
        c.expect('[');
        List<Object> out = new ArrayList<>();
        c.skipWs();
        if (c.peek() == ']') { c.advance(); return out; }
        while (true) {
            c.skipWs();
            out.add(parseValue(c));
            c.skipWs();
            char nx = c.peek();
            if (nx == ',') { c.advance(); continue; }
            if (nx == ']') { c.advance(); break; }
            throw new IllegalStateException("Expected , or ] at index " + c.pos);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static NpcRecord toRecord(Map<String, Object> f) {
        List<Action> actions = new ArrayList<>();
        Object raw = f.get("actions");
        if (raw instanceof List<?> list) {
            for (Object element : list) {
                if (!(element instanceof Map<?, ?> obj)) continue;
                Map<String, Object> a = (Map<String, Object>) obj;
                try {
                    actions.add(new Action(
                            Action.Type.valueOf(str(a, "type").toUpperCase(Locale.ROOT)),
                            ClickType.valueOf(str(a, "click").toUpperCase(Locale.ROOT)),
                            str(a, "value"),
                            str(a, "password")));
                } catch (IllegalArgumentException ignored) {
                    // Unknown enum value (file edited or schema drift); drop the action.
                }
            }
        }
        return new NpcRecord(
                str(f, "id"), str(f, "name"), str(f, "world"),
                dbl(f, "x"), dbl(f, "y"), dbl(f, "z"),
                (float) dbl(f, "yaw"), (float) dbl(f, "pitch"),
                str(f, "skinName"), str(f, "skinValue"), str(f, "skinSignature"),
                actions);
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
                        String hex = "" + c.advance() + c.advance() + c.advance() + c.advance();
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
