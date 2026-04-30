package cv.oryzon.npcs.action;

/**
 * Which click triggers an action. Mirrors the three useful PE InteractActions
 * on the client: left-click (ATTACK), right-click (INTERACT_AT) and
 * shift+right-click. Shift+right-click is reserved for the OP edit menu when
 * the clicker has the admin permission, so it doubles as an action trigger
 * only for non-admin viewers.
 */
public enum ClickType {
    RIGHT,
    LEFT,
    SHIFT_RIGHT
}
