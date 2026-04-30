package cv.oryzon.npcs.store;

/**
 * Storage shape for an NPC. Decoupled from {@link cv.oryzon.npcs.npc.Npc} on
 * purpose: the runtime entity holds Bukkit references (World, Location) that
 * shouldn't bleed into JSON.
 */
public record NpcRecord(
        String id,
        String name,
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        String skinName,
        String skinValue,
        String skinSignature
) {}
