package stew6.ui.swing;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import javax.script.*;
import javax.swing.*;
import stew6.*;

final class AppleMenu {

    private static final Logger log = Logger.getLogger(Menu.class);

    private AppleMenu() {
    }

    public static void customize(Menu menu, int javaMajorVersion, Map<String, JMenuItem> itemMap,
                                 final AnyActionListener anyActionListener) {
        log.info("customizing for macOS, java version=%d", javaMajorVersion);
        ScriptEngine se = new ScriptEngineManager().getEngineByExtension("js");
        se.put("d", new Delegate(menu, anyActionListener));
        final String gettingAppCode;
        if (javaMajorVersion >= 9) {
            log.debug("OrangeMenu.applyCustomize() version 9+");
            gettingAppCode = "java.awt.Desktop.getDesktop()";
        } else {
            log.debug("OrangeMenu.applyCustomize() version 8-");
            gettingAppCode = "com.apple.eawt.Application.getApplication()";
        }
        // @formatter:off
        final String s;
        s = ""
            + "var app = "
            + gettingAppCode
            + ";"
            + "app.setAboutHandler(function(evt) {"
            + "  d.showAbout(evt.source);"
            + "});"
            + "app.setQuitHandler(function(evt,qr) {"
            + "  if (d.confirmQuitAndYes()) {"
            + "    d.quit();"
            + "    qr.performQuit();"
            + "  } else {"
            + "    qr.cancelQuit();"
            + "  }"
            + "});"
            + "d.setupDockerIcon();"
            + "";
        // @formatter:on
        try {
            se.eval(s);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        for (Entry<String, JMenuItem> entry : new ArrayList<>(itemMap.entrySet())) {
            String key = entry.getKey();
            if (key.equals(AnyActionKey.quit.toString()) || key.equals(AnyActionKey.showAbout.toString())) {
                itemMap.remove(key);
                JMenuItem item = entry.getValue();
                item.getParent().remove(item);
            }
        }
    }

    // must declare as public to be enable to call by nashorn
    public static final class Delegate {
        private final Component parent;
        private final AnyActionListener anyActionListener;
        public Delegate(Component parent, AnyActionListener anyActionListener) {
            this.parent = parent;
            this.anyActionListener = anyActionListener;
        }
        public boolean confirmQuitAndYes() {
            return JOptionPane.showConfirmDialog(parent, WindowLauncher.res.s("i.confirm-quit"), "",
                                                 JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        }
        public void quit() {
            WindowLauncher.exit();
        }
        public void showAbout(Object srcComponent) {
            anyActionListener.anyActionPerformed(new AnyActionEvent(srcComponent, Menu.Item.showAbout));
        }
        public void setupDockerIcon() throws Exception {
            Class<?> c = Class.forName("com.apple.eawt.Application");
            Method m = c.getDeclaredMethod("getApplication");
            Method m2 = c.getDeclaredMethod("setDockIconImage", Image.class);
            m2.invoke(m.invoke(null), Utilities.getImageIcon("stew.png").getImage());
        }
    }

}
