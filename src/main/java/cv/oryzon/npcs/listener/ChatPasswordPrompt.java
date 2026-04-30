package cv.oryzon.npcs.listener;

import cv.oryzon.npcs.action.ActionRunner;
import cv.oryzon.npcs.ui.PendingPrompts;
import org.bukkit.entity.Player;

/**
 * Wires CONNECT_SERVER's password requirement into the existing chat-prompt
 * machinery. The runner asks the player; ChatPromptListener delivers the next
 * message to our callback; we run the success branch only on a constant-time
 * password match.
 */
public final class ChatPasswordPrompt implements ActionRunner.PasswordPrompt {

    private final PendingPrompts prompts;

    public ChatPasswordPrompt(PendingPrompts prompts) {
        this.prompts = prompts;
    }

    @Override
    public void askThen(Player viewer, String expected, Runnable onSuccess) {
        viewer.sendMessage("§eThis NPC asks for a password. Type it in chat. §7(or 'cancel')");
        prompts.expectCallback(viewer.getUniqueId(), input -> {
            if (constantTimeEquals(input, expected)) {
                onSuccess.run();
            } else {
                viewer.sendMessage("§cWrong password.");
            }
        });
    }

    private static boolean constantTimeEquals(String a, String b) {
        // Length leak is fine here — passwords on a per-NPC plugin aren't worth
        // padding, but the byte-by-byte XOR keeps us off the obvious side channel.
        if (a == null || b == null) return false;
        byte[] ab = a.getBytes();
        byte[] bb = b.getBytes();
        if (ab.length != bb.length) return false;
        int diff = 0;
        for (int i = 0; i < ab.length; i++) diff |= ab[i] ^ bb[i];
        return diff == 0;
    }
}
