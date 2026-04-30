package cv.oryzon.npcs.action;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Dispatches a list of actions in order. CONNECT_SERVER goes out through the
 * BungeeCord plugin channel — the proxy (BungeeCord/Velocity) intercepts the
 * "Connect" subchannel and switches the player; works with both proxies on
 * 1.20.6+.
 */
public final class ActionRunner {

    private static final String CHANNEL = "BungeeCord";

    private final Plugin plugin;

    public ActionRunner(Plugin plugin) {
        this.plugin = plugin;
        Messenger messenger = Bukkit.getMessenger();
        if (!messenger.isOutgoingChannelRegistered(plugin, CHANNEL)) {
            messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        }
    }

    public void run(Player viewer, List<Action> actions, ClickType click, PasswordPrompt passwords) {
        for (Action action : actions) {
            if (action.click() != click) continue;
            execute(viewer, action, passwords);
        }
    }

    private void execute(Player viewer, Action action, PasswordPrompt passwords) {
        String value = Placeholders.apply(viewer, action.value());
        switch (action.type()) {
            case MESSAGE -> viewer.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', value));
            case RUN_COMMAND -> Bukkit.dispatchCommand(viewer,
                    value.startsWith("/") ? value.substring(1) : value);
            case CONNECT_SERVER -> {
                if (action.requiresPassword()) {
                    passwords.askThen(viewer, action.password(),
                            () -> sendConnect(viewer, value));
                } else {
                    sendConnect(viewer, value);
                }
            }
        }
    }

    private void sendConnect(Player viewer, String server) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            out.writeUTF("Connect");
            out.writeUTF(server);
            viewer.sendPluginMessage(plugin, CHANNEL, buf.toByteArray());
        } catch (IOException e) {
            viewer.sendMessage("§cCould not request server switch: " + e.getMessage());
        }
    }

    /** Strategy hook: the password capture lives in a chat listener (M5). */
    public interface PasswordPrompt {
        void askThen(Player viewer, String expected, Runnable onSuccess);
    }
}
