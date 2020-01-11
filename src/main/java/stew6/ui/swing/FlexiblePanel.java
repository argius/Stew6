package stew6.ui.swing;

import static java.awt.GridBagConstraints.*;
import java.awt.*;
import javax.swing.*;

public class FlexiblePanel extends JPanel {

    protected final GridBagLayout g;
    protected final GridBagConstraints c;

    public FlexiblePanel() {
        this.g = new GridBagLayout();
        this.c = new GridBagConstraints();
        setLayout(g);
        c.gridwidth = 8;
        c.fill = NONE;
        c.ipadx = 2;
        c.ipady = 0;
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = WEST;
        c.weightx = 1.0;
        c.weighty = 1.0;
    }

    public void addComponent(Component component, boolean isRowEnd) {
        if (isRowEnd) {
            c.gridwidth = REMAINDER;
        }
        g.setConstraints(component, c);
        add(component);
        if (isRowEnd) {
            c.gridwidth = LINE_START;
        }
    }

}
