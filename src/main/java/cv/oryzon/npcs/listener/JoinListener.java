package cv.oryzon.npcs.listener;

import cv.oryzon.npcs.npc.NpcManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinListener implements Listener {

    private final NpcManager manager;

    public JoinListener(NpcManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.sendAllTo(event.getPlayer());
    }
}
