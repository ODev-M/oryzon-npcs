package cv.oryzon.npcs.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import cv.oryzon.npcs.action.ActionRunner;
import cv.oryzon.npcs.action.ClickType;
import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.ui.NpcEditMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Routes client {@code Interact Entity} packets back to NPCs.
 *
 * <p>Behaviour:
 * <ul>
 *   <li><b>Shift + right-click</b> by an op ({@code oryzonnpcs.admin}) opens
 *       the edit menu (M4); for non-admins it falls through as a SHIFT_RIGHT
 *       action trigger.</li>
 *   <li>Plain right-click → RIGHT actions; left-click → LEFT actions.</li>
 * </ul>
 */
public final class NpcInteractListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final NpcManager manager;
    private final ActionRunner actions;
    private final ActionRunner.PasswordPrompt passwordPrompt;

    public NpcInteractListener(Plugin plugin, NpcManager manager, ActionRunner actions,
                               ActionRunner.PasswordPrompt passwordPrompt) {
        this.plugin = plugin;
        this.manager = manager;
        this.actions = actions;
        this.passwordPrompt = passwordPrompt;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);

        Npc target = null;
        for (Npc candidate : manager.all()) {
            if (candidate.entityId() == wrapper.getEntityId()) { target = candidate; break; }
        }
        if (target == null) return;
        Npc npc = target;

        Player player = (Player) event.getPlayer();
        event.setCancelled(true);

        WrapperPlayClientInteractEntity.InteractAction action = wrapper.getAction();
        boolean sneaking = wrapper.isSneaking().orElse(player.isSneaking());

        ClickType click;
        if (action == WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
            click = ClickType.LEFT;
        } else if (action == WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            click = sneaking ? ClickType.SHIFT_RIGHT : ClickType.RIGHT;
        } else {
            return; // INTERACT fires alongside INTERACT_AT; ignore the duplicate.
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (click == ClickType.SHIFT_RIGHT && player.hasPermission("oryzonnpcs.admin")) {
                NpcEditMenu.open(player, npc);
                return;
            }
            actions.run(player, npc.actions(), click, passwordPrompt);
        });
    }
}
