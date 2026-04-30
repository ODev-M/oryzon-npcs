package cv.oryzon.npcs.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.ui.NpcEditMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Routes client {@code Interact Entity} packets back to NPCs.
 *
 * <p>Behaviour:
 * <ul>
 *   <li><b>Shift + right-click</b> on an NPC by an op (permission
 *       {@code oryzonnpcs.admin}) opens the edit menu (M4).</li>
 *   <li>Plain right-click and left-click are surfaced to the action system in
 *       M5; until then they only print an ack.</li>
 * </ul>
 */
public final class NpcInteractListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final NpcManager manager;

    public NpcInteractListener(Plugin plugin, NpcManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;
        WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
        int entityId = wrapper.getEntityId();

        Npc target = null;
        for (Npc candidate : manager.all()) {
            if (candidate.entityId() == entityId) { target = candidate; break; }
        }
        if (target == null) return;
        Npc npc = target;

        Player player = (Player) event.getPlayer();
        event.setCancelled(true);

        // PE only fires INTERACT_AT once per right-click, INTERACT also fires;
        // we use INTERACT_AT to avoid double-handling.
        if (wrapper.getAction() != WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT) {
            return;
        }

        boolean sneaking = wrapper.isSneaking().orElse(player.isSneaking());
        boolean canEdit = player.hasPermission("oryzonnpcs.admin");

        // Bukkit inventory APIs are main-thread only.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (sneaking && canEdit) {
                NpcEditMenu.open(player, npc);
            } else {
                player.sendMessage("§7[OryzonNPCs] §fInteracted with §a" + npc.id() + "§f.");
            }
        });
    }
}
