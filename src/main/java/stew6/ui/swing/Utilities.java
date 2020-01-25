package stew6.ui.swing;

import static java.awt.event.InputEvent.*;
import java.awt.*;
import javax.swing.*;

final class Utilities {

    static final Class<Utilities> CLASS = Utilities.class;

    private Utilities() { // empty, forbidden
    }

    static ImageIcon getImageIcon(String name) {
        try {
            return new ImageIcon(CLASS.getResource("icon/" + name));
        } catch (RuntimeException ex) {
            return new ImageIcon();
        }
    }

    static KeyStroke getKeyStroke(String s) {
        KeyStroke ks = KeyStroke.getKeyStroke(s);
        if (ks != null && s.matches("(?i).*Ctrl.*")) {
            return convertShortcutMask(ks, getMenuShortcutKeyMask());
        }
        return ks;
    }

    static KeyStroke convertShortcutMask(KeyStroke ks, int shortcutMask) {
        final int mod = ks.getModifiers();
        if ((mod & (CTRL_DOWN_MASK | CTRL_MASK)) != 0) {
            final int newmod = mod & ~(CTRL_DOWN_MASK | CTRL_MASK) | shortcutMask;
            return KeyStroke.getKeyStroke(ks.getKeyCode(), newmod);
        }
        return ks;
    }

    static int getMenuShortcutKeyMask() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    }

}
