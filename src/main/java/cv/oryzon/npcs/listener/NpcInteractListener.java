package cv.oryzon.npcs.listener;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import org.bukkit.entity.Player;

/**
 * Routes client {@code Interact Entity} packets back to NPCs.
 *
 * <p>M2 just logs the click; M5 wires actions and M4 the OP-only edit menu
 * (sneak + right-click).
 */
public final class NpcInteractListener extends PacketListenerAbstract {

    private final NpcManager manager;

    public NpcInteractListener(NpcManager manager) {
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

        Player player = (Player) event.getPlayer();
        // Cancel so the client doesn't yell about an unknown entity.
        event.setCancelled(true);
        // M5 will dispatch actions here. For M2 we only acknowledge.
        player.sendMessage("§7[OryzonNPCs] §fInteracted with NPC §a" + target.id() + "§f.");
    }
}
