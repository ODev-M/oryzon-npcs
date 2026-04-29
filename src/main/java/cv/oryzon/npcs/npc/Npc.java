package cv.oryzon.npcs.npc;

import cv.oryzon.npcs.skin.Skin;
import org.bukkit.Location;

import java.util.UUID;

/**
 * Server-side representation of an NPC. The actual rendered entity is purely
 * client-side packets — PacketEvents builds them, no real Bukkit entity exists.
 *
 * <p>Each NPC owns a stable {@code id} (used for /npc remove + future ZeroBase
 * persistence) and a {@code profileId} (used as the GameProfile UUID the client
 * sees, must be unique per online server to avoid tab list collisions).
 */
public final class Npc {

    private final String id;
    private final UUID profileId;
    private final int entityId;
    private String name;
    private Location location;
    private Skin skin;

    public Npc(String id, UUID profileId, int entityId, String name, Location location, Skin skin) {
        this.id = id;
        this.profileId = profileId;
        this.entityId = entityId;
        this.name = name;
        this.location = location;
        this.skin = skin;
    }

    public String id() { return id; }
    public UUID profileId() { return profileId; }
    public int entityId() { return entityId; }
    public String name() { return name; }
    public Location location() { return location; }
    public Skin skin() { return skin; }

    public void setName(String name) { this.name = name; }
    public void setLocation(Location location) { this.location = location; }
    public void setSkin(Skin skin) { this.skin = skin; }
}
