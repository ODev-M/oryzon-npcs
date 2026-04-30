package cv.oryzon.npcs.ui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marker holder so InventoryClickEvent can identify our menu in O(1) without
 * relying on title parsing.
 */
public final class NpcEditHolder implements InventoryHolder {

    private final String npcId;

    public NpcEditHolder(String npcId) {
        this.npcId = npcId;
    }

    public String npcId() { return npcId; }

    @Override
    public Inventory getInventory() {
        // Holder is created before the inventory; Bukkit re-binds it via
        // createInventory(holder, ...). We don't expose a backing reference.
        return Bukkit.createInventory(this, 27);
    }
}
