package cv.oryzon.npcs;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * OryzonNPCs entry point.
 *
 * <p>v0.1 is a scaffold: lifecycle hooks only. M2 wires the NPC core, M3
 * embeds ZeroBase, M4 the in-game UI, M5 the action system + Free release.
 */
public final class OryzonNpcsPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("OryzonNPCs v" + getPluginMeta().getVersion() + " starting up.");
        // TODO M2: register PacketEvents, NPC manager, /npc command.
        // TODO M3: spin up the embedded zerobased subprocess and connect via
        //         the Java client we'll add in this milestone.
    }

    @Override
    public void onDisable() {
        getLogger().info("OryzonNPCs shutting down.");
        // TODO M3: graceful zerobased shutdown + flush.
    }
}
