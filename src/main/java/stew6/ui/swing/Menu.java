package stew6.ui.swing;

import static stew6.ui.swing.Menu.Item.*;
import java.awt.*;
import java.beans.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;
import java.util.regex.*;
import javax.swing.*;
import minestra.text.*;
import stew6.*;

/**
 * The menu bar.
 */
final class Menu extends JMenuBar implements PropertyChangeListener {

    private static final Logger log = Logger.getLogger(Menu.class);
    private static final ResourceSheaf res = WindowLauncher.res.derive().withClass(Menu.class);

    /**
     * Menu Items.
     */
    enum Item {
        newWindow,
        closeWindow,
        quit,
        cut,
        copy,
        paste,
        selectAll,
        find,
        toggleFocus,
        clearMessage,
        showStatusBar,
        showColumnNumber,
        showInfoTree,
        showAlwaysOnTop,
        refresh,
        widenColumnWidth,
        narrowColumnWidth,
        adjustColumnWidth,
        autoAdjustMode,
        autoAdjustModeNone,
        autoAdjustModeHeader,
        autoAdjustModeValue,
        autoAdjustModeHeaderAndValue,
        executeCommand,
        breakCommand,
        lastHistory,
        nextHistory,
        sendRollback,
        sendCommit,
        connect,
        disconnect,
        postProcessMode,
        postProcessModeNone,
        postProcessModeFocus,
        postProcessModeShake,
        postProcessModeBlink,
        inputEcryptionKey,
        editConnectors,
        sortResult,
        importFile,
        exportFile,
        showHelp,
        showAbout,
        unknown;
        static Item of(String name) {
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ex) {
                return unknown;
            }
        }
    }

    private List<JMenuItem> lockingTargets;
    private List<JMenuItem> unlockingTargets;
    private EnumMap<Item, JMenuItem> itemToCompMap;

    Menu(final AnyActionListener anyActionListener) {
        this.lockingTargets = new ArrayList<>();
        this.unlockingTargets = new ArrayList<>();
        this.itemToCompMap = new EnumMap<>(Item.class);
        Map<String, JMenuItem> itemMap = new HashMap<>();
        AnyAction aa = new AnyAction(anyActionListener);
        MenuItemFactory mif = new MenuItemFactory(res);
        for (final String groupId : res.s("groups").split(",", -1)) {
            JMenu m = add(mif.createJMenu(groupId));
            for (final JMenuItem o : mif.createJMenuItems(itemMap, "group." + groupId)) {
                if (o == null) {
                    m.add(new JSeparator());
                    continue;
                }
                m.add(o);
                final String itemId = o.getActionCommand();
                Item itemEnum = Item.of(itemId);
                o.addActionListener(aa);
                itemToCompMap.put(itemEnum, o);
                switch (itemEnum) {
                    case closeWindow:
                    case quit:
                    case cut:
                    case copy:
                    case paste:
                    case selectAll:
                    case find:
                    case clearMessage:
                    case refresh:
                    case widenColumnWidth:
                    case narrowColumnWidth:
                    case adjustColumnWidth:
                    case autoAdjustMode:
                    case executeCommand:
                    case lastHistory:
                    case nextHistory:
                    case connect:
                    case disconnect:
                    case postProcessMode:
                    case sortResult:
                    case exportFile:
                        lockingTargets.add(o);
                        break;
                    case breakCommand:
                        unlockingTargets.add(o);
                        break;
                    default:
                }
            }
        }
        for (JMenuItem parent : Arrays.asList(itemToCompMap.get(autoAdjustMode), itemToCompMap.get(postProcessMode))) {
            if (parent != null) {
                for (MenuElement menuGroup : parent.getSubElements()) {
                    for (MenuElement child : menuGroup.getSubElements()) {
                        JMenuItem o = (JMenuItem)child;
                        o.addActionListener(aa);
                        itemToCompMap.put(Item.of(o.getActionCommand()), o);
                    }
                }
            }
        }
        refreshAllAccelerators(itemMap);
        customize(itemMap, anyActionListener);
        setEnabledStates(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        final String propertyName = e.getPropertyName();
        final Object source = e.getSource();
        final Menu.Item key;
        final boolean selected;
        if (source instanceof JLabel && propertyName.equals("ancestor")) {
            key = showStatusBar;
            selected = ((JLabel)source).isVisible();
        } else if (source instanceof ResultSetTable && propertyName.equals("showNumber")) {
            key = showColumnNumber;
            selected = (Boolean)e.getNewValue();
        } else if (source instanceof DatabaseInfoTree) {
            key = showInfoTree;
            selected = ((Component)source).isEnabled();
        } else if (source instanceof JFrame && propertyName.equals("alwaysOnTop")) {
            key = showAlwaysOnTop;
            selected = (Boolean)e.getNewValue();
        } else if (source instanceof ResultSetTable && propertyName.equals("autoAdjustMode")) {
            final String itemName = e.getNewValue().toString();
            if (itemName.matches("[A-Z_]+")) { // ignore old version
                return;
            } else {
                key = Item.of(itemName);
                selected = true;
            }
        } else if (source instanceof WindowOutputProcessor && propertyName.equals("postProcessMode")) {
            final String itemName = e.getNewValue().toString();
            if (itemName.matches("[A-Z_]+")) { // ignore old version
                return;
            } else {
                key = Item.of(itemName);
                selected = true;
            }
        } else {
            return;
        }
        if (itemToCompMap.containsKey(key)) {
            itemToCompMap.get(key).setSelected(selected);
        }
    }

    /**
     * Sets the state that command was started or not.
     * @param commandStarted
     */
    void setEnabledStates(boolean commandStarted) {
        final boolean lockingTargetsState = !commandStarted;
        for (JMenuItem item : lockingTargets) {
            item.setEnabled(lockingTargetsState);
        }
        final boolean unlockingTargetsState = commandStarted;
        for (JMenuItem item : unlockingTargets) {
            item.setEnabled(unlockingTargetsState);
        }
    }

    // Menu factory utilities

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

    private void customize(Map<String, JMenuItem> itemMap, final AnyActionListener anyActionListener) {
        log.debug("customize:start");
        try {
            final String javaVersionString = System.getProperty("java.runtime.version", "0");
            final int javaMajorVersion = Integer.parseInt(javaVersionString.replaceFirst("^(\\d+).+?$", "$1"));
            if (System.getProperty("os.name", "").regionMatches(true, 0, "Mac OS X", 0, 8)) {
                AppleMenu.customize(this, javaMajorVersion, itemMap, anyActionListener);
            }
        } catch (Throwable th) {
            WindowOutputProcessor.showErrorDialog(this.getParent(), th);
        }
        log.debug("customize:end");
    }

}
