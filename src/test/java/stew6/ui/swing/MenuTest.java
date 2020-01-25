package stew6.ui.swing;

import static org.junit.Assert.*;
import static stew6.ui.swing.Menu.*;
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;
import org.junit.*;
import minestra.text.*;
import stew6.*;

// Notice: It doesn't work in headless environments
public final class MenuTest {

    private static final ResourceSheaf res = WindowLauncher.res.withClass(Menu.class);

    @Test
    public void testPropertyChange() {
        if (TestUtils.isInHeadless()) {
            return; // skip test
        }
        final DoNothingAnyActionListener aal = new DoNothingAnyActionListener();
        Menu o = new Menu(aal);
        DatabaseInfoTree dbtree = new DatabaseInfoTree(aal);
        ResultSetTable rst = new ResultSetTable(aal);
        WindowOutputProcessor op = new WindowOutputProcessor(null, rst, null);
        JLabel statusBar = new JLabel();
        Object src;
        String propName;
        JMenuItem item;
        JMenuItem item1;
        JMenuItem item2;
        JMenuItem item3;
        JMenuItem item4;
        // showStatusBar
        item = getItem(o, Item.showStatusBar);
        src = statusBar;
        propName = "ancestor";
        assertFalse(item.isSelected());
        statusBar.setVisible(false);
        firePropertyChange(o, src, propName, null, null);
        assertFalse(item.isSelected());
        statusBar.setVisible(true);
        firePropertyChange(o, src, propName, null, null);
        assertTrue(item.isSelected());
        statusBar.setVisible(false);
        firePropertyChange(o, src, propName, null, null);
        assertFalse(item.isSelected());
        // showColumnNumber
        item = getItem(o, Item.showColumnNumber);
        src = rst;
        propName = "showNumber";
        assertFalse(item.isSelected());
        firePropertyChange(o, src, propName, false, true);
        assertTrue(item.isSelected());
        firePropertyChange(o, src, propName, true, false);
        assertFalse(item.isSelected());
        // showInfoTree
        item = getItem(o, Item.showInfoTree);
        src = dbtree;
        assertFalse(item.isSelected());
        dbtree.setEnabled(true);
        firePropertyChange(o, src, "dummy", null, null);
        assertTrue(item.isSelected());
        dbtree.setEnabled(false);
        firePropertyChange(o, src, "dummy", null, null);
        assertFalse(item.isSelected());
        // showInfoTree
        item = getItem(o, Item.showAlwaysOnTop);
        src = op;
        propName = "alwaysOnTop";
        assertFalse(item.isSelected());
        firePropertyChange(o, src, propName, false, true);
        assertTrue(item.isSelected());
        firePropertyChange(o, src, propName, true, false);
        assertFalse(item.isSelected());
        // autoAdjustMode
        final String none0 = "autoAdjustModeNone";
        final String header = "autoAdjustModeHeader";
        final String value = "autoAdjustModeValue";
        final String handv = "autoAdjustModeHeaderAndValue";
        item1 = getItem(o, Item.autoAdjustModeNone);
        item2 = getItem(o, Item.autoAdjustModeHeader);
        item3 = getItem(o, Item.autoAdjustModeValue);
        item4 = getItem(o, Item.autoAdjustModeHeaderAndValue);
        src = rst;
        propName = "autoAdjustMode";
        assertTrue(item1.isSelected());
        assertFalse(item2.isSelected());
        assertFalse(item3.isSelected());
        assertFalse(item4.isSelected());
        firePropertyChange(o, src, propName, none0, header);
        assertFalse(item1.isSelected());
        assertTrue(item2.isSelected());
        assertFalse(item3.isSelected());
        assertFalse(item4.isSelected());
        firePropertyChange(o, src, propName, header, value);
        assertFalse(item1.isSelected());
        assertFalse(item2.isSelected());
        assertTrue(item3.isSelected());
        assertFalse(item4.isSelected());
        firePropertyChange(o, src, propName, value, handv);
        assertFalse(item1.isSelected());
        assertFalse(item2.isSelected());
        assertFalse(item3.isSelected());
        assertTrue(item4.isSelected());
        firePropertyChange(o, src, propName, handv, none0);
        assertTrue(item1.isSelected());
        assertFalse(item2.isSelected());
        assertFalse(item3.isSelected());
        assertFalse(item4.isSelected());
        // postProcessMode
        final String none = "postProcessModeNone";
        final String focus = "postProcessModeFocus";
        final String shake = "postProcessModeShake";
        final String blink = "postProcessModeBlink";
        item1 = getItem(o, Item.postProcessModeNone);
        item2 = getItem(o, Item.postProcessModeFocus);
        item3 = getItem(o, Item.postProcessModeShake);
        item4 = getItem(o, Item.postProcessModeBlink);
        src = op;
        propName = "postProcessMode";
        assertTrue(item1.isSelected());
        assertFalse(item2.isSelected());
        assertFalse(item3.isSelected());
        assertFalse(item4.isSelected());
        firePropertyChange(o, src, propName, none, focus);
        assertFalse(item1.isSelected());
        assertTrue(item2.isSelected());
        assertFalse(item3.isSelected());
        assertFalse(item4.isSelected());
        firePropertyChange(o, src, propName, focus, shake);
        assertFalse(item1.isSelected());
        assertFalse(item2.isSelected());
        assertTrue(item3.isSelected());
        assertFalse(item4.isSelected());
        firePropertyChange(o, src, propName, shake, blink);
        assertFalse(item1.isSelected());
        assertFalse(item2.isSelected());
        assertFalse(item3.isSelected());
        assertTrue(item4.isSelected());
        firePropertyChange(o, src, propName, blink, none);
        assertTrue(item1.isSelected());
        assertFalse(item2.isSelected());
        assertFalse(item3.isSelected());
        assertFalse(item4.isSelected());
    }

    private static void firePropertyChange(Menu menu, Object src, String propertyName, Object oldValue,
                                           Object newValue) {
        menu.propertyChange(new PropertyChangeEvent(src, propertyName, oldValue, newValue));
    }

    @Test
    public void testSetEnabledStates() {
        if (TestUtils.isInHeadless()) {
            return; // skip test
        }
        Menu o = new Menu(new DoNothingAnyActionListener());
        o.setEnabled(true);
        // TODO assertion of Menu.setEnabled
    }

    @Test
    public void testRefreshAllAccelerators() throws FileNotFoundException {
        if (TestUtils.isInHeadless()) {
            return; // skip test
        }
        final File keyBindConf = App.getSystemFile("keybind.conf");
        if (keyBindConf.exists()) {
            // throw new IllegalStateException("file exists: " + keyBindConf.getAbsolutePath());
            return; // skip test
        }
        List<JMenuItem> a;
        try {
            try (PrintWriter out = new PrintWriter(keyBindConf)) {
                out.println("# for unittest");
                out.println("sendCommit = ctrl shift K");
                out.println("sendRollback = ctrl shift R");
            }
            MenuItemFactory mif = new MenuItemFactory(res);
            a = mif.createJMenuItems("group.command");
        } finally {
            assertTrue(keyBindConf.delete());
        }
        int checkedCount = 0;
        for (JMenuItem o : a) {
            if (o == null) {
                continue;
            }
            final String s = o.getActionCommand();
            if (s.equals("sendCommit")) {
                assertEquals("shift ctrl pressed K", o.getAccelerator().toString());
                ++checkedCount;
            } else if (s.equals("sendRollback")) {
                assertEquals("shift ctrl pressed R", o.getAccelerator().toString());
                ++checkedCount;
            }
        }
        assertEquals(2, checkedCount);
    }

    @Test
    public void testCreateJMenuItems() {
        if (TestUtils.isInHeadless()) {
            return; // skip test
        }
        Map<String, JMenuItem> m = new HashMap<>();
        MenuItemFactory mif = new MenuItemFactory(res);
        List<JMenuItem> a = mif.createJMenuItems(m, "group.data");
        JMenuItem menuSortResult = m.get("sortResult");
        assertEquals(3, a.size());
        assertEquals("sortResult", menuSortResult.getActionCommand());
        assertEquals("alt pressed S", menuSortResult.getAccelerator().toString());
    }

    @Test
    public void testCreateJMenuItem() {
        if (TestUtils.isInHeadless()) {
            return; // skip test
        }
        MenuItemFactory mif = new MenuItemFactory(res);
        JMenuItem menuSortResult = mif.createJMenuItem("sortResult");
        assertEquals("sortResult", menuSortResult.getActionCommand());
        assertEquals("alt pressed S", menuSortResult.getAccelerator().toString());
    }

    @Test
    public void testEnumItem() {
        if (TestUtils.isInHeadless()) {
            return; // skip test
        }
        assertEquals(Item.unknown, Item.of(""));
    }

    private static JMenuItem getItem(Menu o, Item itemEnum) {
        try {
            Field f = o.getClass().getDeclaredField("itemToCompMap");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            EnumMap<Item, JMenuItem> m = (EnumMap<Item, JMenuItem>)f.get(o);
            return m.get(itemEnum);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final class DoNothingAnyActionListener implements AnyActionListener {
        DoNothingAnyActionListener() {
        } // empty
        @Override
        public void anyActionPerformed(AnyActionEvent ev) {
            // empty
        }
    }

}
