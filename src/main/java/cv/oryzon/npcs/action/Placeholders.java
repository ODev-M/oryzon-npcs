package cv.oryzon.npcs.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Optional PlaceholderAPI bridge. We don't depend on the jar at compile time —
 * the integration is reflective so PAPI stays a soft dependency. If it's not
 * installed, every input passes through unchanged.
 */
public final class Placeholders {

    private static final boolean PRESENT;
    private static final Method SET_PLACEHOLDERS;

    static {
        boolean ok = false;
        Method method = null;
        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                method = clazz.getMethod("setPlaceholders", Player.class, String.class);
                ok = true;
            }
        } catch (ReflectiveOperationException ignored) {
            // PAPI not installed or API mismatch; bridge stays inert.
        }
        PRESENT = ok;
        SET_PLACEHOLDERS = method;
    }

    private Placeholders() {}

    public static String apply(Player viewer, String input) {
        if (!PRESENT || input == null || input.isEmpty()) return input;
        try {
            return (String) SET_PLACEHOLDERS.invoke(null, viewer, input);
        } catch (ReflectiveOperationException e) {
            return input;
        }
    }
}
