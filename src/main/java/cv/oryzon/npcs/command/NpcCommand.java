package cv.oryzon.npcs.command;

import cv.oryzon.npcs.npc.Npc;
import cv.oryzon.npcs.npc.NpcManager;
import cv.oryzon.npcs.skin.MojangSkinFetcher;
import cv.oryzon.npcs.skin.Skin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public final class NpcCommand implements CommandExecutor, TabCompleter {

    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");
    private static final List<String> ROOT = List.of("create", "remove", "list", "tp", "help");

    private final NpcManager manager;
    private final MojangSkinFetcher skins;

    public NpcCommand(NpcManager manager, MojangSkinFetcher skins) {
        this.manager = manager;
        this.skins = skins;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list"   -> handleList(sender);
            case "tp"     -> handleTp(sender, args);
            default       -> sendHelp(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can create NPCs (need a location).");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /npc create <id> [skinName]");
            return;
        }
        String id = args[1];
        if (!ID_PATTERN.matcher(id).matches()) {
            sender.sendMessage("§cInvalid id. Use 1-32 chars: A-Z, a-z, 0-9, _ or -.");
            return;
        }
        if (manager.get(id).isPresent()) {
            sender.sendMessage("§cAn NPC with id \"" + id + "\" already exists.");
            return;
        }

        String skinName = args.length >= 3 ? args[2] : id;
        sender.sendMessage("§7Resolving skin for §f" + skinName + "§7…");

        skins.fetch(skinName).whenComplete((skin, error) -> {
            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                Skin resolved = (error != null || skin == null) ? Skin.EMPTY : skin;
                Npc npc = manager.create(id, id, player.getLocation(), resolved);
                sender.sendMessage("§aSpawned NPC §f" + npc.id()
                        + (resolved == Skin.EMPTY ? " §7(no skin)" : " §7with skin §f" + skinName));
            });
        });
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /npc remove <id>");
            return;
        }
        if (manager.remove(args[1])) {
            sender.sendMessage("§aRemoved NPC §f" + args[1] + "§a.");
        } else {
            sender.sendMessage("§cNo NPC with id \"" + args[1] + "\".");
        }
    }

    private void handleList(CommandSender sender) {
        if (manager.all().isEmpty()) {
            sender.sendMessage("§7No NPCs registered.");
            return;
        }
        sender.sendMessage("§7Registered NPCs (" + manager.all().size() + "):");
        for (Npc npc : manager.all()) {
            sender.sendMessage("§7- §f" + npc.id()
                    + " §7@ " + formatLoc(npc));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can teleport.");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /npc tp <id>");
            return;
        }
        Optional<Npc> npc = manager.get(args[1]);
        if (npc.isEmpty()) {
            sender.sendMessage("§cNo NPC with id \"" + args[1] + "\".");
            return;
        }
        player.teleport(npc.get().location());
        sender.sendMessage("§aTeleported to §f" + npc.get().id() + "§a.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§7§m                    §r §fOryzonNPCs §7§m                    ");
        sender.sendMessage("§e/npc create <id> [skin] §7– spawn at your position");
        sender.sendMessage("§e/npc remove <id>        §7– delete an NPC");
        sender.sendMessage("§e/npc list               §7– list every registered NPC");
        sender.sendMessage("§e/npc tp <id>            §7– teleport to an NPC");
    }

    private String formatLoc(Npc npc) {
        return String.format("%s %.1f %.1f %.1f",
                npc.location().getWorld() != null ? npc.location().getWorld().getName() : "?",
                npc.location().getX(), npc.location().getY(), npc.location().getZ());
    }

    private org.bukkit.plugin.Plugin getPlugin() {
        return Bukkit.getPluginManager().getPlugin("OryzonNPCs");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return startsWith(ROOT, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("tp"))) {
            List<String> ids = new ArrayList<>();
            manager.all().forEach(n -> ids.add(n.id()));
            return startsWith(ids, args[1]);
        }
        return List.of();
    }

    private List<String> startsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) out.add(option);
        }
        return out;
    }
}
