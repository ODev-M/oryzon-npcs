package cv.oryzon.npcs.action;

/**
 * One thing that happens when a player clicks an NPC. The on-disk shape is
 * dead simple: {@code type}, {@code click}, {@code value} and (only for
 * CONNECT) an optional {@code password} that has to match what the player
 * just typed in chat.
 */
public record Action(Type type, ClickType click, String value, String password) {

    public enum Type {
        /** Send a chat message to the clicker. PlaceholderAPI applied if present. */
        MESSAGE,
        /** Execute a command as the clicker. {@code value} is the command without the slash. */
        RUN_COMMAND,
        /** Connect the player to a backend server via BungeeCord plugin messaging. */
        CONNECT_SERVER
    }

    public Action(Type type, ClickType click, String value) {
        this(type, click, value, "");
    }

    public boolean requiresPassword() {
        return type == Type.CONNECT_SERVER && password != null && !password.isEmpty();
    }
}
