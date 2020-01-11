package stew6.io;

import java.io.*;
import java.nio.channels.*;

/**
 * Utility methods for files.
 */
public final class FileUtilities {

    private static final String PATTERN_EXTENSION = "^.*\\.([^\\.]+)$";

    private FileUtilities() { // empty
    }

    /**
     * Resolves the path.
     * @param parent
     * @param child
     * @return
     */
    public static File resolve(File parent, File child) {
        if (child.isAbsolute()) {
            return new File(child.getAbsolutePath());
        }
        return new File(parent, child.getPath());
    }

    /**
     * Resolves the path.
     * @param parent
     * @param child
     * @return
     */
    public static File resolve(File parent, String child) {
        return resolve(parent, new File(child));
    }

    /**
     * Resolves the path.
     * @param parent
     * @param child
     * @return
     */
    public static File resolve(String parent, String child) {
        return resolve(new File(parent), child);
    }

    /**
     * Returns the extension of this path.
     * @param file
     * @return
     * @see #getExtension(String)
     */
    public static String getExtension(File file) {
        return getExtension(file.getName());
    }

    /**
     * Returns the extension of this path.
     * If the path string contains periods,
     * the extension is a string from after the last period to the end.
     * Otherwise, returns an empty string.
     * @param path
     * @return
     */
    public static String getExtension(String path) {
        if (path.matches(PATTERN_EXTENSION)) {
            return path.replaceFirst(PATTERN_EXTENSION, "$1");
        }
        return "";
    }

    /**
     * Creates directories if not exists.
     * @param file
     * @throws IOException If it failed to create directories
     */
    public static void makeDirectory(File file) throws IOException {
        if (!file.isDirectory()) {
            if (!file.mkdirs() || !file.isDirectory()) {
                throw new IOException("can't make directory: " + file);
            }
        }
    }

    /**
     * Returns the string that read from file.
     * @param file
     * @return
     * @throws IOException
     */
    public static String readAllBytesAsString(File file) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); FileInputStream fis = new FileInputStream(file)) {
            fis.getChannel().transferTo(0, file.length(), Channels.newChannel(bos));
            return bos.toString();
        }
    }

}
