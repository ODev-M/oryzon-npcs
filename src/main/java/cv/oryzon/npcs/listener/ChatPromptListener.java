package cv.oryzon.npcs.listener;

import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.skin.MojangSkinFetcher;
import cv.oryzon.npcs.skin.Skin;
import cv.oryzon.npcs.ui.PendingPrompts;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Captures the next chat message from a player who's in the middle of an
 * edit-menu prompt. The chat event is async; we cancel the broadcast and bounce
 * the actual mutation to the main thread.
 */
public final class ChatPromptListener implements Listener {

    private final Plugin plugin;
    private final NpcManager manager;
    private final MojangSkinFetcher skins;
    private final PendingPrompts prompts;

    public ChatPromptListener(Plugin plugin, NpcManager manager,
                              MojangSkinFetcher skins, PendingPrompts prompts) {
        this.plugin = plugin;
        this.manager = manager;
        this.skins = skins;
        this.prompts = prompts;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingPrompts.Prompt prompt = prompts.consume(player.getUniqueId());
        if (prompt == null) return;

        event.setCancelled(true);
        String input = event.getMessage().trim();
        if (input.equalsIgnoreCase("cancel")) {
            player.sendMessage("§7Edit cancelled.");
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> apply(player, prompt, input));
    }

    private void apply(Player player, PendingPrompts.Prompt prompt, String input) {
        Optional<Npc> ref = manager.get(prompt.npcId());
        if (ref.isEmpty()) {
            player.sendMessage("§cThat NPC no longer exists.");
            return;
        }
        Npc npc = ref.get();

        switch (prompt.kind()) {
            case RENAME -> {
                if (input.length() > 32) {
                    player.sendMessage("§cName too long (max 32 characters).");
                    return;
                }
                manager.update(npc, null, input, null, null);
                player.sendMessage("§aRenamed §f" + npc.id() + " §ato §f" + input + "§a.");
            }
            case SKIN -> {
                String name = input;
                player.sendMessage("§7Resolving skin for §f" + name + "§7…");
                skins.fetch(name).whenComplete((skin, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
                    if (error != null || skin == null || skin == Skin.EMPTY) {
                        player.sendMessage("§cMojang did not return a skin for \"" + name + "\".");
                        return;
                    }
                    Optional<Npc> still = manager.get(prompt.npcId());
                    if (still.isEmpty()) return;
                    manager.update(still.get(), null, null, skin, name);
                    player.sendMessage("§aSkin updated to §f" + name + "§a.");
                }));
            }
        }
    }
}
