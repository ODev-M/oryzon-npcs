package cv.oryzon.npcs.npc;

import cv.oryzon.npcs.action.Action;
import cv.oryzon.npcs.skin.Skin;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private String skinName;
    private final List<Action> actions = new ArrayList<>();

    public Npc(String id, UUID profileId, int entityId, String name, Location location,
               Skin skin, String skinName) {
        this.id = id;
        this.profileId = profileId;
        this.entityId = entityId;
        this.name = name;
        this.location = location;
        this.skin = skin;
        this.skinName = skinName;
    }

    public List<Action> actions() {
        return Collections.unmodifiableList(actions);
    }

    public void addAction(Action action) { actions.add(action); }
    public boolean removeAction(int index) {
        if (index < 0 || index >= actions.size()) return false;
        actions.remove(index);
        return true;
    }
    public void replaceActions(List<Action> next) {
        actions.clear();
        actions.addAll(next);
    }

    public String id() { return id; }
    public UUID profileId() { return profileId; }
    public int entityId() { return entityId; }
    public String name() { return name; }
    public Location location() { return location; }
    public Skin skin() { return skin; }
    public String skinName() { return skinName; }

    public void setName(String name) { this.name = name; }
    public void setLocation(Location location) { this.location = location; }
    public void setSkin(Skin skin, String skinName) { this.skin = skin; this.skinName = skinName; }
}
