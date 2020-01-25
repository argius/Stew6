package stew6.ui.swing;

import static stew6.ui.swing.Utilities.getImageIcon;
import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import javax.swing.*;
import org.apache.commons.lang3.*;
import minestra.text.*;
import stew6.*;

final class MenuItemFactory {

    private final ResourceSheaf res;
    private final boolean autoMnemonic;

    MenuItemFactory(ResourceSheaf res) {
        this.res = res;
        this.autoMnemonic = checkAutoMnemonicIsAvailable();
    }

    JMenu createJMenu(String groupId) {
        final String key = (res.stringOpt("group." + groupId).isPresent() ? "group" : "item") + '.' + groupId;
        final char mn = getChar(key + ".mnemonic");
        final String groupString = res.s(key) + (autoMnemonic ? "(" + mn + ")" : "");
        JMenu group = new JMenu(groupString);
        group.setMnemonic(mn);
        return group;
    }

    private static void refreshAllAccelerators(Map<String, JMenuItem> itemMap) {
        // This method is called everytime menu and popup-menu is created.
        File keyBindConf = App.getSystemFile("keybind.conf");
        if (!keyBindConf.exists()) {
            return;
        }
        Map<String, KeyStroke> keyMap = new HashMap<>();
        try (Scanner r = new Scanner(keyBindConf)) {
            final Pattern p = Pattern.compile("\\s*([^=\\s]+)\\s*=(.*)");
            while (r.hasNextLine()) {
                final String line = r.nextLine();
                if (line.trim().length() == 0 || line.matches("\\s*#.*")) {
                    continue;
                }
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    keyMap.put(m.group(1), Utilities.getKeyStroke(m.group(2)));
                }
            }
        } catch (FileNotFoundException ex) {
            throw new RuntimeException(ex);
        }
        for (final Entry<String, KeyStroke> entry : keyMap.entrySet()) {
            final String k = entry.getKey();
            if (itemMap.containsKey(k)) {
                itemMap.get(k).setAccelerator(entry.getValue());
            }
        }
    }

    List<JMenuItem> createJMenuItems(String groupKey) {
        HashMap<String, JMenuItem> itemMap = new HashMap<>();
        List<JMenuItem> a = createJMenuItems(itemMap, groupKey);
        refreshAllAccelerators(itemMap);
        return a;
    }

    List<JMenuItem> createJMenuItems(Map<String, JMenuItem> itemMap, String groupKey) {
        List<JMenuItem> a = new ArrayList<>();
        for (final String itemId : res.s(groupKey + ".items").split(",", -1)) {
            if (itemId.length() == 0) {
                a.add(null);
            } else {
                JMenuItem o = createJMenuItem(itemId);
                a.add(o);
                itemMap.put(itemId, o);
            }
        }
        return a;
    }

    JMenuItem createJMenuItem(String itemId) {
        final String itemKey = "item." + itemId;
        final char mn = getChar(itemKey + ".mnemonic");
        final String shortcutKey = itemKey + ".shortcut";
        final JMenuItem o;
        if (StringUtils.equalsIgnoreCase(res.s(itemKey + ".checkbox"), "YES")) {
            o = new JCheckBoxMenuItem();
        } else if (StringUtils.equalsIgnoreCase(res.s(itemKey + ".subgroup"), "YES")) {
            o = createJMenu(itemId);
            ButtonGroup buttonGroup = new ButtonGroup();
            boolean selected = false;
            for (final String id : res.s(itemKey + ".items").split(",", -1)) {
                final JMenuItem sub = createJMenuItem(itemId + id);
                o.add(sub);
                buttonGroup.add(sub);
                if (!selected) {
                    sub.setSelected(true);
                    selected = true;
                }
            }
        } else {
            o = new JMenuItem();
        }
        final String shortcutKeyStroke = res.string(shortcutKey, "");
        if (StringUtils.isNotBlank(shortcutKeyStroke)) {
            KeyStroke ks = Utilities.getKeyStroke(shortcutKeyStroke);
            if (ks != null) {
                o.setAccelerator(ks);
            }
        }
        o.setText(res.s(itemKey) + (autoMnemonic ? "(" + mn + ")" : ""));
        o.setMnemonic(mn);
        o.setActionCommand(itemId);
        o.setIcon(getImageIcon(String.format("menu-%s.png", itemId)));
        o.setDisabledIcon(getImageIcon(String.format("menu-disabled-%s.png", itemId)));
        return o;
    }

    private boolean checkAutoMnemonicIsAvailable() {
        return !App.props.getAsBoolean("ui.suppressGenerateMnemonic") && res.i("auto-mnemonic") == 1;
    }

    char getChar(String key) {
        final String s = res.s(key);
        return s.isEmpty() ? ' ' : s.charAt(0);
    }

}
