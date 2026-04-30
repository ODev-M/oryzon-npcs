package cv.oryzon.npcs.ui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class ActionsHolder implements InventoryHolder {

    private final String npcId;

    public ActionsHolder(String npcId) { this.npcId = npcId; }

    public String npcId() { return npcId; }

    @Override
    public Inventory getInventory() {
        return Bukkit.createInventory(this, 36);
    }
}
