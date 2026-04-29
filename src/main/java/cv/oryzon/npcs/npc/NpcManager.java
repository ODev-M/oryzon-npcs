package cv.oryzon.npcs.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.TextureProperty;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoRemove;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import cv.oryzon.npcs.skin.Skin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the lifecycle of every NPC in memory and turns those records into
 * client-visible packets. Storage is in-memory only at M2 — M3 wires ZeroBase
 * underneath without changing this surface.
 *
 * <p>Spawn protocol (Paper 1.20.6+ via PacketEvents):
 * <ol>
 *   <li>{@code PlayerInfoUpdate} ADD_PLAYER + UPDATE_LISTED so the client trusts
 *       the GameProfile (skin layers, name).</li>
 *   <li>{@code SpawnEntity} as a PLAYER entity at the target location.</li>
 *   <li>{@code EntityMetadata} with the skin-layers byte set so capes/jackets
 *       render — without this NPCs look bald.</li>
 *   <li>{@code EntityHeadLook} so the head matches body yaw on spawn.</li>
 *   <li>4 s later, {@code PlayerInfoRemove} to drop the entry from the tab list
 *       while keeping the rendered entity.</li>
 * </ol>
 */
public final class NpcManager {

    private static final long TAB_REMOVAL_DELAY_TICKS = 80L;

    private final Plugin plugin;
    private final Map<String, Npc> npcs = new HashMap<>();
    private final AtomicInteger entityIdCounter = new AtomicInteger(2_000_000);

    public NpcManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public Optional<Npc> get(String id) {
        return Optional.ofNullable(npcs.get(id.toLowerCase()));
    }

    public Collection<Npc> all() {
        return Collections.unmodifiableCollection(npcs.values());
    }

    public Npc create(String id, String name, Location location, Skin skin) {
        String key = id.toLowerCase();
        if (npcs.containsKey(key)) {
            throw new IllegalArgumentException("NPC \"" + id + "\" already exists.");
        }
        Npc npc = new Npc(id, UUID.randomUUID(), entityIdCounter.incrementAndGet(),
                name, location.clone(), skin);
        npcs.put(key, npc);
        for (Player viewer : location.getWorld().getPlayers()) {
            spawnFor(viewer, npc);
        }
        return npc;
    }

    public boolean remove(String id) {
        Npc npc = npcs.remove(id.toLowerCase());
        if (npc == null) return false;
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            despawnFor(viewer, npc);
        }
        return true;
    }

    /** Push every NPC in {@code viewer}'s world to that viewer. Used on join. */
    public void sendAllTo(Player viewer) {
        for (Npc npc : npcs.values()) {
            if (npc.location().getWorld() != null
                    && npc.location().getWorld().equals(viewer.getWorld())) {
                spawnFor(viewer, npc);
            }
        }
    }

    public void shutdown() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            for (Npc npc : npcs.values()) {
                despawnFor(viewer, npc);
            }
        }
        npcs.clear();
    }

    // ---------------------------------------------------------------- packets

    private void spawnFor(Player viewer, Npc npc) {
        UserProfile profile = new UserProfile(npc.profileId(), npc.name());
        if (!npc.skin().value().isEmpty()) {
            profile.getTextureProperties().add(new TextureProperty(
                    "textures", npc.skin().value(), npc.skin().signature()));
        }

        WrapperPlayServerPlayerInfoUpdate.PlayerInfo info =
                new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                        profile, true, 0, GameMode.SURVIVAL, null, null);
        send(viewer, new WrapperPlayServerPlayerInfoUpdate(EnumSet.of(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED), info));

        Location loc = npc.location();
        send(viewer, new WrapperPlayServerSpawnEntity(
                npc.entityId(),
                Optional.of(npc.profileId()),
                EntityTypes.PLAYER,
                new Vector3d(loc.getX(), loc.getY(), loc.getZ()),
                loc.getPitch(), loc.getYaw(), loc.getYaw(),
                0, Optional.empty()));

        // Skin layers byte at index 17 (1.20.6+). 0x7F = all layers visible.
        List<EntityData> meta = new ArrayList<>();
        meta.add(new EntityData(17, EntityDataTypes.BYTE, (byte) 0x7F));
        send(viewer, new WrapperPlayServerEntityMetadata(npc.entityId(), meta));

        send(viewer, new WrapperPlayServerEntityHeadLook(npc.entityId(), loc.getYaw()));

        new BukkitRunnable() {
            @Override public void run() {
                if (!viewer.isOnline()) return;
                send(viewer, new WrapperPlayServerPlayerInfoRemove(
                        Collections.singletonList(npc.profileId())));
            }
        }.runTaskLater(plugin, TAB_REMOVAL_DELAY_TICKS);
    }

    private void despawnFor(Player viewer, Npc npc) {
        send(viewer, new WrapperPlayServerDestroyEntities(npc.entityId()));
        send(viewer, new WrapperPlayServerPlayerInfoRemove(
                Collections.singletonList(npc.profileId())));
    }

    private void send(Player viewer, Object packet) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
    }
}
