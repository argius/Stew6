package stew6;

import java.io.*;
import java.util.*;
import stew6.ui.console.*;
import stew6.ui.swing.*;

/**
 * Main class.
 */
public final class App {

    // These fields need to evaluate orderly on static-initializer
    private static final Logger log = Logger.getLogger(App.class);

    public static final String rootPackageName = App.class.getPackage().getName();
    private static final File dir = initializeDirectory();
    public static final LayeredProperties props = initializeProperties();

    private static final String PropFileName = "stew.properties";

    private App() { // empty
    }

    private static File initializeDirectory() {
        log.info("init app");
        LayeredProperties tmpProps = new LayeredProperties(System.getenv(), System.getProperties());
        String s = tmpProps.get("home", "");
        if (s.isEmpty()) {
            s = System.getenv("HOME");
            if (s == null || s.isEmpty()) {
                s = System.getProperty("user.home");
            }
        }
        log.debug("HOMEDIR=[%s]", s);
        File homeDir = new File(s);
        try {
            File canonicalFile = homeDir.getCanonicalFile().getAbsoluteFile();
            log.info("HOMEDIR(canonical path)=[%s]", canonicalFile);
            return new File(canonicalFile, ".stew");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static LayeredProperties initializeProperties() {
        Map<String, String> layer1 = System.getenv();
        Properties layer2 = getFileProperties();
        Properties layer3 = System.getProperties();
        LayeredProperties newProps = new LayeredProperties(layer1, layer2, layer3);
        if (log.isDebugEnabled()) {
            log.debug("dump properties%s", newProps.dump());
        }
        return newProps;
    }

    private static Properties getFileProperties() {
        Properties props = new Properties();
        try (InputStream is = new FileInputStream(new File(dir, PropFileName))) {
            props.load(is);
        } catch (FileNotFoundException e) {
            log.warn("%s", e);
        } catch (IOException e) {
            log.warn(e, "getFileProperties");
        }
        return props;
    }

    /**
     * Returns system directory.
     * @return
     */
    public static File getSystemDirectory() {
        return dir;
    }

    public static File getSystemFile(String name) {
        return new File(dir, name);
    }

    /**
     * Returns app version.
     * @return
     */
    public static String getVersion() {
        return ResourceManager.Default.read("version", "(UNKNOWN)");
    }

    static void showUsage() {
        ResourceManager res = ResourceManager.Default;
        System.out.println(res.get("i.usagePrefix") + res.get("i.usage.syntax"));
        System.out.println(res.get("usage.message"));
    }

    static int run(String... args) {
        OptionSet opts;
        try {
            opts = OptionSet.parseArguments(args);
        } catch (Exception e) {
            System.err.println(ResourceManager.Default.get("e.invalid-cli-option", e.getMessage()));
            return 1;
        }
        if (opts.isShowVersion()) {
            System.out.println("Stew " + App.getVersion());
        } else if (opts.isHelp()) {
            OptionSet.showHelp();
        } else if (opts.isCui()) {
            return ConsoleLauncher.main(opts);
        } else if (opts.isGui()) {
            WindowLauncher.main(args);
        } else if (opts.isEdit()) {
            ConnectorMapEditor.main(args);
        } else {
            final String v = props.get("bootstrap", props.get("boot", ""));
            if (v.equalsIgnoreCase("CUI")) {
                return ConsoleLauncher.main(opts);
            } else if (v.equalsIgnoreCase("GUI")) {
                WindowLauncher.main(args);
            } else {
                if (!v.isEmpty()) {
                    System.err.printf("warning: invalid bootstrap option: %s%n", v);
                }
                showUsage();
            }
        }
        return 0;
    }

    /** main **/
    public static void main(String... args) {
        log.info("start (version: %s)", getVersion());
        log.debug("args=%s", Arrays.asList(args));
        int exitStatus = run(args);
        if (exitStatus == 0) {
            log.info("end");
        } else {
            log.info("end abnormally, status=%d", exitStatus);
            System.exit(exitStatus);
        }
    }

}
