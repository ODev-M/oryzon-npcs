package cv.oryzon.npcs.store;

import cv.oryzon.npcs.action.Action;

import java.util.List;

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
        String skinSignature,
        List<Action> actions
) {
    public NpcRecord {
        if (actions == null) actions = List.of();
    }
}
