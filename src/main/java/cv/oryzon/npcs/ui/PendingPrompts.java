package cv.oryzon.npcs.ui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks players who've been asked to type something in chat as part of an
 * edit-menu flow. The next chat message they send is consumed here instead of
 * being broadcast.
 */
public final class PendingPrompts {

    public enum Kind {
        RENAME,
        SKIN,
        ADD_MESSAGE_ACTION,
        ADD_COMMAND_ACTION,
        ADD_CONNECT_ACTION,
        /** Free-form: when a Consumer-based prompt is registered (e.g. CONNECT password). */
        CALLBACK
    }

    public record Prompt(Kind kind, String npcId, Consumer<String> callback) {
        public Prompt(Kind kind, String npcId) { this(kind, npcId, null); }
    }

    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public void expect(UUID player, Kind kind, String npcId) {
        prompts.put(player, new Prompt(kind, npcId));
    }

    public void expectCallback(UUID player, Consumer<String> callback) {
        prompts.put(player, new Prompt(Kind.CALLBACK, "", callback));
    }

    public Prompt consume(UUID player) {
        return prompts.remove(player);
    }

    public void cancel(UUID player) {
        prompts.remove(player);
    }
}
