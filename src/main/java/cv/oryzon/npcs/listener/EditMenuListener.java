package cv.oryzon.npcs.listener;

import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.ui.NpcEditHolder;
import cv.oryzon.npcs.ui.NpcEditMenu;
import cv.oryzon.npcs.ui.PendingPrompts;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Optional;

/**
 * Handles clicks inside the OP-only NPC edit menu. Prompts (rename / skin) get
 * registered here; ChatPromptListener finishes them.
 */
public final class EditMenuListener implements Listener {

    private final NpcManager manager;
    private final PendingPrompts prompts;

    public EditMenuListener(NpcManager manager, PendingPrompts prompts) {
        this.manager = manager;
        this.prompts = prompts;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof NpcEditHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("oryzonnpcs.admin")) return;

        Optional<Npc> ref = manager.get(holder.npcId());
        if (ref.isEmpty()) {
            player.sendMessage("§cThat NPC no longer exists.");
            player.closeInventory();
            return;
        }
        Npc npc = ref.get();

        switch (event.getRawSlot()) {
            case NpcEditMenu.SLOT_RENAME -> {
                prompts.expect(player.getUniqueId(), PendingPrompts.Kind.RENAME, npc.id());
                player.closeInventory();
                player.sendMessage("§eType the new name in chat. §7(or 'cancel')");
            }
            case NpcEditMenu.SLOT_SKIN -> {
                prompts.expect(player.getUniqueId(), PendingPrompts.Kind.SKIN, npc.id());
                player.closeInventory();
                player.sendMessage("§eType a Mojang username for the skin. §7(or 'cancel')");
            }
            case NpcEditMenu.SLOT_MOVE -> {
                manager.update(npc, player.getLocation(), null, null, null);
                player.sendMessage("§aMoved §f" + npc.id() + " §ato your position.");
                player.closeInventory();
            }
            case NpcEditMenu.SLOT_TP -> {
                player.teleport(npc.location());
                player.closeInventory();
            }
            case NpcEditMenu.SLOT_DELETE -> {
                if (!event.isShiftClick()) {
                    player.sendMessage("§7Shift-click to confirm deletion.");
                    return;
                }
                manager.remove(npc.id());
                player.sendMessage("§cDeleted §f" + npc.id() + "§c.");
                player.closeInventory();
            }
            default -> { /* decoration / coming-soon */ }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        // Nothing to release; PendingPrompts is keyed by player and survives close
        // intentionally — the prompt is the whole point of closing the menu.
    }
}
