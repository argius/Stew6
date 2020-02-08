package stew6.ui.fx;

import stew6.*;
import stew6.io.*;

public final class FxWindowConfigFile extends YamlFile<FxWindowConfig> {

    private static final String CONFIG_FILE_NAME = "stew6.ui.fx.window.config.yml";

    protected FxWindowConfigFile() {
        super(FxWindowConfig.class, () -> App.getSystemFile(CONFIG_FILE_NAME).toPath());
    }

}
