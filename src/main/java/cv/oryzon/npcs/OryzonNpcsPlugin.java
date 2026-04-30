package cv.oryzon.npcs;

import com.github.retrooper.packetevents.PacketEvents;
import cv.oryzon.npcs.command.NpcCommand;
import cv.oryzon.npcs.listener.ChatPromptListener;
import cv.oryzon.npcs.listener.EditMenuListener;
import cv.oryzon.npcs.listener.JoinListener;
import cv.oryzon.npcs.listener.NpcInteractListener;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.skin.MojangSkinFetcher;
import cv.oryzon.npcs.store.JsonFileStore;
import cv.oryzon.npcs.store.NpcStore;
import cv.oryzon.npcs.ui.PendingPrompts;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * OryzonNPCs entry point.
 *
 * <p>M4: PacketEvents + NpcManager + JSON persistence + OP shift+right-click
 * edit menu. M5 wires the action system and ships v0.1.0 to SpigotMC.
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
        PendingPrompts prompts = new PendingPrompts();

        getServer().getPluginManager().registerEvents(new JoinListener(npcManager), this);
        getServer().getPluginManager().registerEvents(new EditMenuListener(npcManager, prompts), this);
        getServer().getPluginManager().registerEvents(
                new ChatPromptListener(this, npcManager, skins, prompts), this);

        PacketEvents.getAPI().getEventManager().registerListener(
                new NpcInteractListener(this, npcManager));
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
