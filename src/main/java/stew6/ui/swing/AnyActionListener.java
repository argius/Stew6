package stew6.ui.swing;

/**
 * @see AnyAction
 */
@FunctionalInterface
interface AnyActionListener {

    void anyActionPerformed(AnyActionEvent ev);

    default void anyActionPerformed(Object source, Object actionKey, Object... args) {
        anyActionPerformed(new AnyActionEvent(source, actionKey, args));
    }

}
