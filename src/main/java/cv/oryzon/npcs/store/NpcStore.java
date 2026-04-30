package cv.oryzon.npcs.store;

import java.util.Collection;

/**
 * Persistence boundary for NPCs. M3a ships a JSON file implementation; M3b will
 * add a ZeroBase-backed implementation behind the same interface, selected via
 * config. Anything that needs to know about disk goes through here.
 */
public interface NpcStore {

    Collection<NpcRecord> loadAll();

    void save(NpcRecord record);

    void delete(String id);

    void close();
}
