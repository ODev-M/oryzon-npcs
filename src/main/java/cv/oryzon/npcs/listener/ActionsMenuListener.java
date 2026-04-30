package cv.oryzon.npcs.listener;

import cv.oryzon.npcs.action.Action;
import cv.oryzon.npcs.action.ClickType;
import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.ui.ActionsHolder;
import cv.oryzon.npcs.ui.ActionsMenu;
import cv.oryzon.npcs.ui.NpcEditMenu;
import cv.oryzon.npcs.ui.PendingPrompts;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Click handling for the per-NPC action editor.
 */
public final class ActionsMenuListener implements Listener {

    private final NpcManager manager;
    private final PendingPrompts prompts;

    public ActionsMenuListener(NpcManager manager, PendingPrompts prompts) {
        this.manager = manager;
        this.prompts = prompts;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ActionsHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!player.hasPermission("oryzonnpcs.admin")) return;

        Optional<Npc> ref = manager.get(holder.npcId());
        if (ref.isEmpty()) {
            player.closeInventory();
            return;
        }
        Npc npc = ref.get();
        int raw = event.getRawSlot();

        if (raw < ActionsMenu.LIST_CAPACITY) {
            int index = raw;
            if (index >= npc.actions().size()) return;
            if (event.isShiftClick()) {
                List<Action> next = new ArrayList<>(npc.actions());
                next.remove(index);
                npc.replaceActions(next);
                manager.persist(npc);
                ActionsMenu.open(player, npc);
            } else {
                Action existing = npc.actions().get(index);
                ClickType cycled = ActionsMenu.nextClick(existing.click());
                List<Action> next = new ArrayList<>(npc.actions());
                next.set(index, new Action(existing.type(), cycled, existing.value(), existing.password()));
                npc.replaceActions(next);
                manager.persist(npc);
                ActionsMenu.open(player, npc);
            }
            return;
        }

        switch (raw) {
            case ActionsMenu.SLOT_ADD_MESSAGE -> startAdd(player, npc, PendingPrompts.Kind.ADD_MESSAGE_ACTION,
                    "&eType the message. & color codes + PlaceholderAPI supported.");
            case ActionsMenu.SLOT_ADD_COMMAND -> startAdd(player, npc, PendingPrompts.Kind.ADD_COMMAND_ACTION,
                    "&eType the command, with or without /. PAPI supported.");
            case ActionsMenu.SLOT_ADD_CONNECT -> startAdd(player, npc, PendingPrompts.Kind.ADD_CONNECT_ACTION,
                    "&eType the server name &7(or &fname|password &7for password-locked).");
            case ActionsMenu.SLOT_BACK -> NpcEditMenu.open(player, npc);
            default -> { /* decoration */ }
        }
    }

    private void startAdd(Player player, Npc npc, PendingPrompts.Kind kind, String prompt) {
        prompts.expect(player.getUniqueId(), kind, npc.id());
        player.closeInventory();
        player.sendMessage(prompt.replace('&', '§'));
        player.sendMessage("§7(or 'cancel')");
    }
}
