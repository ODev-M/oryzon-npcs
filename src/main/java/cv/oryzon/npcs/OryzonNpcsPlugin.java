package cv.oryzon.npcs;

import com.github.retrooper.packetevents.PacketEvents;
import cv.oryzon.npcs.command.NpcCommand;
import cv.oryzon.npcs.listener.JoinListener;
import cv.oryzon.npcs.listener.NpcInteractListener;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.skin.MojangSkinFetcher;
import cv.oryzon.npcs.store.JsonFileStore;
import cv.oryzon.npcs.store.NpcStore;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * OryzonNPCs entry point.
 *
 * <p>M2: registers PacketEvents, the NpcManager, listeners and the /npc command.
 * No persistence yet — that's M3 (ZeroBase). M4 wires the OP shift+right-click
 * editor; M5 wires actions and the Free release goes out.
 */
public final class OryzonNpcsPlugin extends JavaPlugin {

    private NpcManager npcManager;

    @Override
    public void onLoad() {
        // PacketEvents must be loaded (not just enabled) before any other plugin
        // tries to talk to it during world load.
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(false)
                .checkForUpdates(false);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        Path dataFile = getDataFolder().toPath().resolve("npcs.json");
        NpcStore store = new JsonFileStore(dataFile, getLogger());
        npcManager = new NpcManager(this, store);
        npcManager.loadFromStore();

        MojangSkinFetcher skins = new MojangSkinFetcher(getLogger());

        getServer().getPluginManager().registerEvents(new JoinListener(npcManager), this);

        PacketEvents.getAPI().getEventManager().registerListener(new NpcInteractListener(npcManager));
        PacketEvents.getAPI().init();

        PluginCommand npc = getCommand("npc");
        if (npc != null) {
            NpcCommand executor = new NpcCommand(npcManager, skins);
            npc.setExecutor(executor);
            npc.setTabCompleter(executor);
        }

        getLogger().info("OryzonNPCs v" + getPluginMeta().getVersion() + " ready.");
    }

    @Override
    public void onDisable() {
        if (npcManager != null) {
            npcManager.shutdown();
        }
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
        getLogger().info("OryzonNPCs shutting down.");
    }
}
