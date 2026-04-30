package cv.oryzon.npcs.ui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players who've been asked to type something in chat as part of an
 * edit-menu flow. The next chat message they send is consumed here instead of
 * being broadcast.
 */
public final class PendingPrompts {

    public enum Kind { RENAME, SKIN }

    public record Prompt(Kind kind, String npcId) {}

    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public void expect(UUID player, Kind kind, String npcId) {
        prompts.put(player, new Prompt(kind, npcId));
    }

    public Prompt consume(UUID player) {
        return prompts.remove(player);
    }

    public void cancel(UUID player) {
        prompts.remove(player);
    }
}
