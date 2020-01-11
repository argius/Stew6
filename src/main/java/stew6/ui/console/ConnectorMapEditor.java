package stew6.ui.console;

import java.io.*;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import net.argius.stew.*;
import stew6.*;
import stew6.io.*;

/**
 * The Connector Editor for console mode.
 */
public final class ConnectorMapEditor {

    private static final ResourceManager res = ResourceManager.getInstance(ConnectorMapEditor.class);
    private static final String[] PROP_KEYS = {"name", "classpath", "driver", "url", "user",
                                               "password", "readonly", "rollback"};

    private final ConnectorMap map;

    private ConnectorMap oldContent;

    private ConnectorMapEditor() throws IOException {
        ConnectorMap m;
        try {
            m = ConnectorConfiguration.load();
        } catch (FileNotFoundException ex) {
            printMessage("main.notice.filenotexists");
            printLine(String.format("(%s)", ex.getMessage()));
            m = new ConnectorMap();
        }
        this.oldContent = m;
        this.map = new ConnectorMap(this.oldContent);
    }

    /**
     * Processes to input properties.
     * @param id
     * @param props
     * @return true if the configuration was changed, otherwise false.
     */
    private static boolean proceedInputProperties(String id, Properties props) {
        while (true) {
            printMessage("property.start1");
            printMessage("property.start2");
            for (String key : PROP_KEYS) {
                String value = props.getProperty(key);
                print(res.get("property.input", key, value));
                String input = getInput("");
                if (input != null && input.length() > 0) {
                    props.setProperty(key, input);
                }
            }
            for (String key : PROP_KEYS) {
                printLine(key + "=" + props.getProperty(key));
            }
            if (confirmYes("property.tryconnect.confirm")) {
                try {
                    final int timeoutSeconds = App.props.getAsInt("timeout.connection.tryout", 20);
                    Connector c = new Connector(id, props);
                    Future<String> future = c.tryOutConnection();
                    while (true) {
                        try {
                            printLine(future.get(timeoutSeconds, TimeUnit.SECONDS));
                            break;
                        } catch (TimeoutException ex) {
                            if (!confirmYes(res.get("i.confirm.retry-timeout", timeoutSeconds))) {
                                break;
                            }
                        }
                    }
                } catch (Exception ex) {
                    printMessage("property.tryconnect.failed", ex.getMessage());
                }
            }
            if (confirmYes("property.update.confirm")) {
                return true;
            }
            if (!confirmYes("property.retry.confirm")) {
                printMessage("property.update.cancel");
                return false;
            }
        }
    }

    /**
     * Adds a Connector.
     * @param id
     */
    private void proceedAdd(String id) {
        if (map.containsKey(id)) {
            printMessage("proc.alreadyexists", id);
            return;
        }
        Properties props = new Properties();
        // setting default
        for (String key : PROP_KEYS) {
            props.setProperty(key, "");
        }
        if (proceedInputProperties(id, props)) {
            map.put(id, new Connector(id, props));
            printMessage("proc.added", id);
        }
    }

    /**
     * Modifies a Connector.
     * @param id
     */
    private void proceedModify(String id) {
        if (!map.containsKey(id)) {
            printMessage("proc.notexists", id);
            return;
        }
        Properties props = map.getConnector(id).toProperties();
        if (proceedInputProperties(id, props)) {
            map.put(id, new Connector(id, props));
            printMessage("proc.modified", id);
        }
    }

    /**
     * Deletes a Connector.
     * @param id
     */
    private void proceedRemove(String id) {
        Connector connector = map.getConnector(id);
        printLine("ID[" + id + "]:" + connector.getName());
        if (confirmYes("proc.remove.confirm")) {
            map.remove(id);
            printMessage("proc.remove.finished");
        } else {
            printMessage("proc.remove.canceled");
        }
        printLine(map);
    }

    /**
     * Copies from a Connector to another.
     * @param src
     * @param dst
     */
    private void proceedCopy(String src, String dst) {
        if (!map.containsKey(src)) {
            printMessage("proc.notexists", src);
            printMessage("proc.copy.canceled");
            return;
        }
        if (map.containsKey(dst)) {
            printMessage("proc.alreadyexists", dst);
            printMessage("proc.copy.canceled");
            return;
        }
        map.put(dst, new Connector(dst, map.getConnector(src)));
        printMessage("proc.copy.finished");
    }

    /**
     * Displays all of the Connectors.
     */
    private void proceedDisplayIds() {
        for (Entry<String, Connector> entry : map.entrySet()) {
            String id = entry.getKey();
            printLine(String.format("%10s : %s", id, map.getConnector(id).getName()));
        }
    }

    /**
     * Displays the Connector info specified by ID.
     * @param id
     */
    private void proceedDisplayDetail(String id) {
        if (!map.containsKey(id)) {
            printMessage("proc.notexists", id);
            return;
        }
        Properties props = map.getConnector(id).toProperties();
        for (String key : PROP_KEYS) {
            printLine(String.format("%10s : %s", key, props.getProperty(key)));
        }
    }

    /**
     * Saves the configuration to a file.
     * @throws IOException
     */
    private void proceedSave() throws IOException {
        if (map.equals(oldContent)) {
            printMessage("proc.nomodification");
        } else if (confirmYes("proc.save.confirm")) {
            File systemDirectory = App.getSystemDirectory();
            if (!systemDirectory.exists()) {
                if (confirmYes(res.get("i.confirm.makesystemdir", systemDirectory))) {
                    FileUtilities.makeDirectory(systemDirectory);
                } else {
                    printMessage("proc.save.canceled");
                    return;
                }
            }
            ConnectorConfiguration.save(map);
            oldContent = new ConnectorMap(map);
            printMessage("proc.save.finished");
        } else {
            printMessage("proc.save.canceled");
        }
    }

    /**
     * Loads from file.
     * @throws IOException
     */
    private void proceedLoad() throws IOException {
        ConnectorMap m = ConnectorConfiguration.load();
        if (m.equals(oldContent) && m.equals(map)) {
            printMessage("proc.nomodification");
            return;
        }
        printMessage("proc.load.confirm1");
        if (confirmYes("proc.load.confirm2")) {
            map.clear();
            map.putAll(ConnectorConfiguration.load());
            printMessage("proc.load.finished");
        } else {
            printMessage("proc.load.canceled");
        }
    }

    /**
     * Returns the input string from STDIN.
     * @param messageId
     * @param args
     * @return
     */
    private static String getInput(String messageId, Object... args) {
        if (messageId.length() > 0) {
            print(res.get(messageId, args));
        }
        print(res.get("proc.prompt"));
        @SuppressWarnings("resource")
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        }
        return "";
    }

    /**
     * Returns the result to confirm Yes or No as boolean.
     * @param messageId
     * @return
     */
    private static boolean confirmYes(String messageId) {
        if (messageId.length() > 0) {
            print(res.get(messageId));
        }
        print("(y/N)");
        return getInput("").equalsIgnoreCase("y");
    }

    private static void print(Object o) {
        System.out.print(o);
    }

    private static void printLine() {
        System.out.println();
    }

    private static void printLine(Object o) {
        System.out.println(o);
    }

    private static void printMessage(String messageId, Object... args) {
        printLine(res.get(messageId, args));
    }

    /**
     * Invokes this editor.
     */
    static void invoke() {
        printLine();
        printMessage("main.start");
        printLine();
        try {
            ConnectorMapEditor editor = new ConnectorMapEditor();
            while (true) {
                Parameter p = new Parameter(getInput("main.wait"));
                final String command = p.at(0);
                final String id = p.at(1);
                if (command.equalsIgnoreCase("help")) {
                    printMessage("help");
                } else if (command.equalsIgnoreCase("a")) {
                    editor.proceedAdd(id);
                } else if (command.equalsIgnoreCase("m")) {
                    editor.proceedModify(id);
                } else if (command.equalsIgnoreCase("r")) {
                    editor.proceedRemove(id);
                } else if (command.equalsIgnoreCase("copy")) {
                    editor.proceedCopy(id, p.at(2));
                } else if (command.equalsIgnoreCase("disp")) {
                    if (id.length() == 0) {
                        editor.proceedDisplayIds();
                    } else {
                        editor.proceedDisplayDetail(id);
                    }
                } else if (command.equalsIgnoreCase("save")) {
                    editor.proceedSave();
                } else if (command.equalsIgnoreCase("load")) {
                    editor.proceedLoad();
                } else if (command.equalsIgnoreCase("exit")) {
                    if (editor.map.equals(editor.oldContent)) {
                        break;
                    } else if (confirmYes("proc.exit.confirm")) {
                        break;
                    }
                }
                printLine();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        printLine();
        printMessage("main.end");
    }

    /**
     * @param args
     */
    public static void main(String... args) {
        System.out.println("Stew " + App.getVersion());
        invoke();
    }

}
