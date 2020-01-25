package stew6.ui.swing;

import static java.awt.event.KeyEvent.*;
import static javax.swing.JOptionPane.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static stew6.ui.swing.TextSearchPanel.ActionKey.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import javax.swing.*;
import minestra.text.*;
import stew6.ui.swing.TextSearch.*;

/**
 * This is a panel that contains input controls for text-search.
 */
final class TextSearchPanel extends JPanel implements AnyActionListener {

    enum ActionKey {
        close, search, forward, backward
    }

    private static final ResourceSheaf res = WindowLauncher.res.derive().withClass(TextSearchPanel.class);

    private final List<TextSearch> targets;
    private final JTextField text;
    private final JCheckBox useRegexCheck;
    private final JCheckBox ignoreCaseCheck;

    private TextSearch currentTarget;
    private boolean searchBackward;

    TextSearchPanel(JFrame frame) {
        // [Init Instances]
        this.targets = new ArrayList<>();
        this.text = new JTextField(20);
        this.useRegexCheck = new JCheckBox(res.s("useregex"));
        this.ignoreCaseCheck = new JCheckBox(res.s("ignorecase"));
        setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        setVisible(false);
        final JButton closeButton = new JButton(Utilities.getImageIcon("close.png"));
        final JTextField text = this.text;
        final JButton forwardButton = new JButton(res.s("forward"));
        final JButton backwardButton = new JButton(res.s("backward"));
        final JCheckBox useRegexCheck = this.useRegexCheck;
        final JCheckBox ignoreCaseCheck = this.ignoreCaseCheck;
        // [Layout]
        closeButton.setToolTipText(res.s("close"));
        closeButton.setMargin(new Insets(0, 0, 0, 0));
        text.setMargin(new Insets(1, 2, 1, 2));
        forwardButton.setMargin(new Insets(0, 7, 0, 7));
        backwardButton.setMargin(new Insets(0, 7, 0, 7));
        changeFontSize(useRegexCheck, 0.8f);
        changeFontSize(ignoreCaseCheck, 0.8f);
        add(createFiller(2));
        add(closeButton);
        add(createFiller(2));
        add(new JLabel(res.s("label")));
        add(text);
        add(createFiller(1));
        add(forwardButton);
        add(backwardButton);
        add(useRegexCheck);
        add(ignoreCaseCheck);
        // [Setup Focus Policy]
        final FocusTraversalPolicy parentPolicy = frame.getFocusTraversalPolicy();
        class LayoutFocusTraversalPolicyImpl extends LayoutFocusTraversalPolicy {
            @Override
            public Component getComponentAfter(Container focusCycleRoot, Component component) {
                if (component == ignoreCaseCheck) {
                    return closeButton;
                }
                return parentPolicy.getComponentAfter(focusCycleRoot, component);
            }
            @Override
            public Component getComponentBefore(Container focusCycleRoot, Component component) {
                if (component == closeButton) {
                    return ignoreCaseCheck;
                }
                return parentPolicy.getComponentBefore(focusCycleRoot, component);
            }
        }
        frame.setFocusTraversalPolicy(new LayoutFocusTraversalPolicyImpl());
        // [Events]
        // text field
        ContextMenu.createForText(text);
        // buttons
        bindEvent(text, search, getKeyStroke(VK_ENTER, 0));
        bindEvent(closeButton, close);
        bindEvent(forwardButton, forward);
        bindEvent(backwardButton, backward);
        // ESC key
        final KeyStroke ksESC = getKeyStroke(VK_ESCAPE, 0);
        Stream.of(closeButton, text, forwardButton, backwardButton, useRegexCheck, ignoreCaseCheck)
              .forEach(x -> bindEvent(x, close, ksESC));
    }

    private void bindEvent(JButton b, Object key) {
        b.setActionCommand(String.valueOf(key));
        b.addActionListener(new AnyAction(this));
    }

    private void bindEvent(JComponent c, Object key, KeyStroke ks) {
        AnyAction aa = new AnyAction(c);
        aa.bind(this, key, ks);
    }

    @Override
    protected void processComponentEvent(ComponentEvent e) {
        if (e.getID() == ComponentEvent.COMPONENT_RESIZED) {
            validate();
        }
    }

    @Override
    public void anyActionPerformed(AnyActionEvent ev) {
        if (ev.isAnyOf(close)) {
            setVisible(false);
        } else if (ev.isAnyOf(search)) {
            startSearch();
        } else if (ev.isAnyOf(forward)) {
            setSearchBackward(false);
            startSearch();
        } else if (ev.isAnyOf(backward)) {
            setSearchBackward(true);
            startSearch();
        }
    }

    private static void changeFontSize(Component c, float rate) {
        Font f = c.getFont();
        c.setFont(f.deriveFont(Font.PLAIN, f.getSize() * rate));
    }

    private static Component createFiller(int width) {
        char[] a = new char[width];
        Arrays.fill(a, ' ');
        return new JLabel(String.valueOf(a));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            final int length = text.getText().length();
            if (length > 0) {
                text.setSelectionStart(0);
                text.setSelectionEnd(length);
            }
            text.requestFocus();
        } else {
            for (TextSearch target : targets) {
                target.reset();
            }
            if (currentTarget instanceof Component) {
                Component c = (Component)currentTarget;
                c.requestFocus();
            }
        }
    }

    void addTarget(TextSearch target) {
        targets.add(target);
    }

    void removeTarget(TextSearch target) {
        targets.remove(target);
    }

    void setCurrentTarget(TextSearch currentTarget) {
        this.currentTarget = currentTarget;
    }

    void setSearchBackward(boolean searchBackward) {
        this.searchBackward = searchBackward;
    }

    void startSearch() {
        final String s = text.getText();
        if (s == null || s.length() == 0 || currentTarget == null) {
            return;
        }
        final boolean useRegularExpression = useRegexCheck.isSelected();
        final boolean ignoreCase = ignoreCaseCheck.isSelected();
        Matcher matcher = new Matcher(s, useRegularExpression, ignoreCase);
        matcher.setBackward(searchBackward);
        matcher.setContinuously(true);
        for (TextSearch target : targets) {
            target.reset();
        }
        final boolean found = currentTarget.search(matcher);
        if (!found) {
            Component parent;
            if (currentTarget instanceof Component) {
                parent = ((Component)currentTarget).getParent();
            } else {
                parent = getParent();
            }
            showMessageDialog(parent, res.format("message.notfound", s), null, WARNING_MESSAGE);
        }
    }

}
