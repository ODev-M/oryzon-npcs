package cv.oryzon.npcs.skin;

public record Skin(String value, String signature) {

    public static final Skin EMPTY = new Skin("", "");
}
