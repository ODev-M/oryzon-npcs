package cv.oryzon.npcs.ui;

import cv.oryzon.npcs.npc.Npc;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-page edit menu rendered as a 27-slot inventory. Slot layout is fixed
 * here so the click listener can switch on slot index without reflection or
 * NBT lookups.
 */
public final class NpcEditMenu {

    public static final int SLOT_RENAME   = 10;
    public static final int SLOT_SKIN     = 12;
    public static final int SLOT_MOVE     = 14;
    public static final int SLOT_TP       = 16;
    public static final int SLOT_ACTIONS  = 22;
    public static final int SLOT_DELETE   = 26;

    public static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "OryzonNPCs · ";

    private NpcEditMenu() {}

    public static String titleFor(Npc npc) {
        String trimmed = npc.id();
        if (trimmed.length() > 20) trimmed = trimmed.substring(0, 20);
        return TITLE_PREFIX + ChatColor.WHITE + trimmed;
    }

    public static void open(Player viewer, Npc npc) {
        Inventory inv = Bukkit.createInventory(new NpcEditHolder(npc.id()), 27, titleFor(npc));

        inv.setItem(SLOT_RENAME, item(Material.NAME_TAG, "&aRename",
                "&7Current: &f" + npc.name(),
                "",
                "&eClick &7and type the new name in chat."));
        inv.setItem(SLOT_SKIN, item(Material.PLAYER_HEAD, "&aChange skin",
                "&7Current: &f" + (npc.skinName() == null || npc.skinName().isEmpty()
                        ? "(none)" : npc.skinName()),
                "",
                "&eClick &7and type a Mojang name in chat."));
        inv.setItem(SLOT_MOVE, item(Material.ENDER_PEARL, "&aMove here",
                "&7Snap the NPC to your current position.",
                "",
                "&eClick &7to confirm."));
        inv.setItem(SLOT_TP, item(Material.COMPASS, "&aTeleport to NPC",
                "&7Sends you to where the NPC stands.",
                "",
                "&eClick &7to confirm."));
        inv.setItem(SLOT_ACTIONS, item(Material.COMMAND_BLOCK, "&7Actions",
                "&8Coming in M5",
                "",
                "&8Server-switch, run command, message, …"));
        inv.setItem(SLOT_DELETE, item(Material.BARRIER, "&cDelete NPC",
                "&7Removes &f" + npc.id() + " &7for good.",
                "",
                "&cShift-click &7to confirm."));

        viewer.openInventory(inv);
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> rendered = new ArrayList<>(lore.length);
            for (String line : lore) {
                rendered.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(rendered);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
