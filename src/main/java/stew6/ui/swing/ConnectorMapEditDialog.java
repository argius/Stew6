package stew6.ui.swing;

import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.JOptionPane.*;
import static javax.swing.ScrollPaneConstants.*;
import static stew6.ui.swing.ConnectorMapEditDialog.ActionKey.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.function.*;
import javax.swing.*;
import javax.swing.event.*;
import minestra.text.*;
import stew6.*;
import stew6.io.*;

final class ConnectorMapEditDialog extends JDialog implements ChangeListener, AnyActionListener {

    enum ActionKey {
        addNew, modify, rename, remove, up, down, submit, cancel
    }

    private static final ResourceSheaf res = WindowLauncher.res.derive().withClass(ConnectorMapEditDialog.class);

    private final Consumer<String> showErrorMessage = x -> showMessageDialog(this, x, null, ERROR_MESSAGE);
    private final Pred<String> confirmYes = x -> showConfirmDialog(this, x, "", YES_NO_OPTION) == YES_OPTION;
    private final BinaryOperator<String> showInputDialog = (x, y) -> showInputDialog(this, x, y);
    private final ConnectorMap connectorMap;
    private final JList<ConnectorEntry> idList;
    private final DefaultListModel<ConnectorEntry> listModel;

    ConnectorMapEditDialog(JFrame owner, Environment env) {
        // [instance]
        super(owner);
        final DefaultListModel<ConnectorEntry> listModel = new DefaultListModel<>();
        this.connectorMap = new ConnectorMap(env.getConnectorMap());
        this.idList = new JList<>(listModel);
        this.listModel = listModel;
        setTitle(res.s("title"));
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        FlexiblePanel p = new FlexiblePanel();
        p.c.anchor = GridBagConstraints.CENTER;
        p.c.insets = new Insets(8, 12, 8, 0);
        add(p);
        // [components]
        // List
        final JList<ConnectorEntry> idList = this.idList;
        idList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        for (ConnectorEntry entry : ConnectorEntry.toList(connectorMap.values())) {
            listModel.addElement(entry);
        }
        JScrollPane pane = new JScrollPane(idList, VERTICAL_SCROLLBAR_ALWAYS, HORIZONTAL_SCROLLBAR_NEVER);
        pane.setWheelScrollingEnabled(true);
        idList.addMouseListener(new IdListMouseListener());
        p.addComponent(pane, false);
        // Button 1
        JPanel p1 = new JPanel(new GridLayout(6, 1, 4, 2));
        p1.add(createJButton(addNew));
        p1.add(createJButton(modify));
        p1.add(createJButton(rename));
        p1.add(createJButton(remove));
        p1.add(createJButton(up));
        p1.add(createJButton(down));
        p.c.gridwidth = GridBagConstraints.REMAINDER;
        p.c.insets = new Insets(8, 32, 8, 32);
        p.addComponent(p1, true);
        // Button 2
        JPanel p2 = new JPanel(new GridLayout(1, 2, 16, 8));
        p2.add(createJButton(submit));
        p2.add(createJButton(cancel));
        p.c.gridwidth = GridBagConstraints.REMAINDER;
        p.c.fill = GridBagConstraints.NONE;
        p.addComponent(p2, false);
        // [events]
        AnyAction aa = new AnyAction(rootPane);
        // ESC key
        aa.bind(this, true, cancel, KeyStroke.getKeyStroke(VK_ESCAPE, 0));
    }

    private final class IdListMouseListener extends MouseAdapter {
        IdListMouseListener() {
        } // empty
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() % 2 == 0) {
                openConnectorEditDialog();
            }
        }
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        super.processWindowEvent(e);
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            anyActionPerformed(this, cancel);
        }
    }

    @Override
    public void anyActionPerformed(AnyActionEvent ev) {
        if (ev.isAnyOf(addNew)) {
            final String id = showInputDialog.apply(res.s("i.input-new-connector-id"), "");
            if (id == null) {
                return;
            }
            if (connectorMap.containsKey(id)) {
                showErrorMessage.accept(res.format("e.id-already-exists", id));
            } else {
                Connector connector;
                try {
                    connector = new Connector(id, new Properties());
                } catch (IllegalArgumentException ex) {
                    showErrorMessage.accept(ex.getMessage());
                    return;
                }
                openConnectorEditDialog(connector);
            }
        } else if (ev.isAnyOf(modify)) {
            ConnectorEntry entry = idList.getSelectedValue();
            if (entry != null) {
                openConnectorEditDialog(entry.getConnector());
            }
        } else if (ev.isAnyOf(rename)) {
            Object o = idList.getSelectedValue();
            if (o == null) {
                return;
            }
            ConnectorEntry entry = (ConnectorEntry)o;
            final String newId = showInputDialog.apply(res.s("i.input-new-connector-id"), entry.getId());
            if (newId == null || newId.equals(entry.getId())) {
                return;
            }
            connectorMap.remove(entry.getId());
            connectorMap.put(newId, entry.getConnector());
            DefaultListModel<ConnectorEntry> m = (DefaultListModel<ConnectorEntry>)idList.getModel();
            Connector newConnector;
            try {
                newConnector = new Connector(newId, entry.getConnector());
            } catch (IllegalArgumentException ex) {
                showErrorMessage.accept(ex.getMessage());
                return;
            }
            m.set(m.indexOf(entry), new ConnectorEntry(newId, newConnector));
            idList.repaint();
        } else if (ev.isAnyOf(remove)) {
            if (!confirmYes.exam(res.s("i.confirm-remove"))) {
                return;
            }
            ConnectorEntry selected = idList.getSelectedValue();
            connectorMap.remove(selected.getId());
            DefaultListModel<ConnectorEntry> m = (DefaultListModel<ConnectorEntry>)idList.getModel();
            m.removeElement(selected);
        } else if (ev.isAnyOf(up)) {
            shiftSelectedElementUpward();
        } else if (ev.isAnyOf(down)) {
            shiftSelectedElementDownward();
        } else if (ev.isAnyOf(submit)) {
            requestClose(true);
        } else if (ev.isAnyOf(cancel)) {
            requestClose(false);
        }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        Object source = e.getSource();
        if (source instanceof Connector) {
            Connector connector = (Connector)source;
            final String id = connector.getId();
            connectorMap.setConnector(id, connector);
            ConnectorEntry changed = new ConnectorEntry(id, connector);
            final int index = listModel.indexOf(changed);
            if (index >= 0) {
                listModel.set(index, changed);
                idList.setSelectedIndex(index);
            } else {
                listModel.add(0, changed);
                idList.setSelectedIndex(0);
            }
        }
    }

    private JButton createJButton(Object cmdObject) {
        final String cmd = String.valueOf(cmdObject);
        JButton button = new JButton(res.s("button." + cmd));
        button.setActionCommand(cmd);
        button.addActionListener(new AnyAction(this));
        return button;
    }

    void openConnectorEditDialog() {
        ConnectorEntry entry = idList.getSelectedValue();
        openConnectorEditDialog(entry.getConnector());
    }

    private void openConnectorEditDialog(Connector connector) {
        ConnectorEditDialog dialog = new ConnectorEditDialog(this, connector);
        dialog.addChangeListener(this);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(getParent());
        dialog.setSize(dialog.getPreferredSize());
        dialog.setVisible(true);
    }

    private void shiftSelectedElementUpward() {
        final int index = idList.getSelectedIndex();
        if (index == 0) {
            return;
        }
        final int newIndex = index - 1;
        swap(listModel, index, newIndex);
        idList.setSelectedIndex(newIndex);
        idList.ensureIndexIsVisible(newIndex);
    }

    private void shiftSelectedElementDownward() {
        final int index = idList.getSelectedIndex();
        final int size = listModel.getSize();
        if (index == size - 1) {
            return;
        }
        final int newIndex = index + 1;
        swap(listModel, index, newIndex);
        idList.setSelectedIndex(newIndex);
        idList.ensureIndexIsVisible(newIndex);
    }

    private static void swap(DefaultListModel<ConnectorEntry> listModel, int index1, int index2) {
        ConnectorEntry o = listModel.get(index1);
        listModel.set(index1, listModel.get(index2));
        listModel.set(index2, o);
    }

    private void requestClose(boolean withSaving) {
        if (withSaving) {
            if (!confirmYes.exam(res.s("i.confirm-save"))) {
                return;
            }
            File systemDirectory = App.getSystemDirectory();
            if (!systemDirectory.exists()) {
                if (confirmYes.exam(res.format("i.confirm.makesystemdir", systemDirectory))) {
                    try {
                        FileUtilities.makeDirectory(systemDirectory);
                    } catch (IOException ex) {
                        WindowOutputProcessor.showErrorDialog(this, ex);
                    }
                }
                if (!systemDirectory.exists()) {
                    return;
                }
            }
            ConnectorMap m = new ConnectorMap();
            for (Object o : listModel.toArray()) {
                ConnectorEntry entry = (ConnectorEntry)o;
                final String id = entry.getId();
                m.setConnector(id, connectorMap.getConnector(id));
            }
            m.saveToFile();
            connectorMap.clear();
            connectorMap.putAll(m);
        } else {
            ConnectorMap fileContent = ConnectorMap.createFromFile();
            if (!connectorMap.equals(fileContent)) {
                if (!confirmYes.exam(res.s("i.confirm-without-save"))) {
                    return;
                }
            }
        }
        dispose();
    }

}
