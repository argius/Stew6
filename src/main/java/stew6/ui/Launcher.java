package stew6.ui;

import stew6.*;

public interface Launcher {

    void launch(Environment env);

    default void launchWith(OutputProcessor op) {
        Environment env = new Environment();
        env.setOutputProcessor(op);
        launch(env);
    }

    default void launchWith(OutputProcessor op, Environment env) {
        env.setOutputProcessor(op);
        launch(env);
    }

}
