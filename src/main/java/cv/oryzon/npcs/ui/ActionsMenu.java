package cv.oryzon.npcs.ui;

import cv.oryzon.npcs.action.Action;
import cv.oryzon.npcs.action.ClickType;
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
 * Per-NPC action editor. Top three rows list existing actions; the bottom row
 * is action-creation buttons + back navigation.
 */
public final class ActionsMenu {

    public static final int LIST_CAPACITY = 27;
    public static final int SLOT_ADD_MESSAGE = 28;
    public static final int SLOT_ADD_COMMAND = 30;
    public static final int SLOT_ADD_CONNECT = 32;
    public static final int SLOT_BACK        = 35;

    public static final String TITLE_PREFIX = ChatColor.DARK_GRAY + "Actions · ";

    private ActionsMenu() {}

    public static String titleFor(Npc npc) {
        String trimmed = npc.id();
        if (trimmed.length() > 20) trimmed = trimmed.substring(0, 20);
        return TITLE_PREFIX + ChatColor.WHITE + trimmed;
    }

    public static void open(Player viewer, Npc npc) {
        Inventory inv = Bukkit.createInventory(new ActionsHolder(npc.id()), 36, titleFor(npc));

        int slot = 0;
        for (Action action : npc.actions()) {
            if (slot >= LIST_CAPACITY) break;
            inv.setItem(slot++, render(action, slot - 1));
        }

        inv.setItem(SLOT_ADD_MESSAGE, button(Material.PAPER, "&aAdd MESSAGE",
                "&7Send a chat line to the clicker.",
                "&7Supports & color codes + PlaceholderAPI."));
        inv.setItem(SLOT_ADD_COMMAND, button(Material.COMMAND_BLOCK, "&aAdd RUN_COMMAND",
                "&7Run a command as the clicker.",
                "&7With or without the leading slash."));
        inv.setItem(SLOT_ADD_CONNECT, button(Material.ENDER_EYE, "&aAdd CONNECT_SERVER",
                "&7Send to a backend via BungeeCord/Velocity.",
                "&7Format: &fserverName &7or &fserverName|password"));
        inv.setItem(SLOT_BACK, button(Material.ARROW, "&7Back",
                "&7Return to the NPC menu."));

        viewer.openInventory(inv);
    }

    public static ClickType nextClick(ClickType current) {
        return switch (current) {
            case RIGHT -> ClickType.LEFT;
            case LEFT -> ClickType.SHIFT_RIGHT;
            case SHIFT_RIGHT -> ClickType.RIGHT;
        };
    }

    private static ItemStack render(Action action, int index) {
        Material material = switch (action.type()) {
            case MESSAGE -> Material.PAPER;
            case RUN_COMMAND -> Material.COMMAND_BLOCK;
            case CONNECT_SERVER -> Material.ENDER_EYE;
        };
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Trigger: " + ChatColor.WHITE + action.click().name());
        lore.add(ChatColor.GRAY + "Value: " + ChatColor.WHITE + truncate(action.value()));
        if (action.type() == Action.Type.CONNECT_SERVER) {
            lore.add(ChatColor.GRAY + "Password: " + ChatColor.WHITE
                    + (action.requiresPassword() ? "yes" : "none"));
        }
        lore.add("");
        lore.add(ChatColor.YELLOW + "Left-click " + ChatColor.GRAY + "to cycle trigger.");
        lore.add(ChatColor.RED + "Shift-click " + ChatColor.GRAY + "to remove.");

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.WHITE + "#" + (index + 1) + " · "
                    + ChatColor.AQUA + action.type().name());
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static ItemStack button(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> rendered = new ArrayList<>(lore.length);
            for (String line : lore) rendered.add(ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(rendered);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static String truncate(String value) {
        if (value == null) return "";
        return value.length() > 40 ? value.substring(0, 37) + "…" : value;
    }
}
