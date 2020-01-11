package stew6;

import static java.io.File.pathSeparator;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.zip.*;

/**
 * A driver manager for Connector.
 */
final class ConnectorDriverManager {

    private static final Logger log = Logger.getLogger(ConnectorDriverManager.class);

    static final Set<File> driverFiles = Collections.synchronizedSet(new LinkedHashSet<File>());
    static final Set<Driver> drivers = Collections.synchronizedSet(new LinkedHashSet<Driver>());

    private ConnectorDriverManager() {
    } // forbidden

    static Driver getDriver(String url, String driverClassName, String classpath) throws SQLException {
        assert !isBlank(url);
        final boolean hasClasspath = !isBlank(classpath);
        if (!hasClasspath) {
            for (Driver driver : new ArrayList<>(drivers)) {
                if (driver.acceptsURL(url)) {
                    return driver;
                }
            }
        }
        List<File> jars = new ArrayList<>();
        ClassLoader cl;
        if (hasClasspath) {
            List<URL> urls = new ArrayList<>();
            for (String path : classpath.split(pathSeparator)) {
                final File file = new File(path);
                if (isJarFile(file)) {
                    jars.add(file);
                }
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException ex) {
                    log.warn(ex);
                }
            }
            cl = new URLClassLoader(urls.toArray(new URL[urls.size()]));
        } else {
            jars.addAll(getJarFiles("."));
            jars.addAll(driverFiles);
            List<URL> urls = new ArrayList<>();
            for (File file : jars) {
                try {
                    urls.add(file.toURI().toURL());
                } catch (MalformedURLException ex) {
                    log.warn(ex);
                }
            }
            cl = new URLClassLoader(urls.toArray(new URL[urls.size()]),
                                    ClassLoader.getSystemClassLoader());
        }
        driverFiles.addAll(jars);
        final boolean hasDriverClassName = !isBlank(driverClassName);
        if (hasDriverClassName) {
            try {
                Driver driver = DynamicLoader.newInstance(driverClassName, cl);
                assert driver != null;
                return driver;
            } catch (DynamicLoadingException ex) {
                Throwable cause = (ex.getCause() != ex) ? ex.getCause() : ex;
                SQLException exception = new SQLException(cause.toString());
                exception.initCause(cause);
                throw exception;
            }
        }
        final String jdbcDrivers = System.getProperty("jdbc.drivers");
        if (!isBlank(jdbcDrivers)) {
            for (String jdbcDriver : jdbcDrivers.split(":")) {
                try {
                    Driver driver = DynamicLoader.newInstance(jdbcDriver, cl);
                    if (driver != null) {
                        if (!hasClasspath) {
                            drivers.add(driver);
                        }
                        return driver;
                    }
                } catch (DynamicLoadingException ex) {
                    log.warn(ex);
                }
            }
        }
        for (File jar : jars) {
            try {
                Driver driver = getDriver(jar, url, cl);
                if (driver != null) {
                    if (!hasClasspath) {
                        drivers.add(driver);
                    }
                    return driver;
                }
            } catch (IOException ex) {
                log.warn(ex);
            }
        }
        for (String path : System.getProperty("java.class.path", "").split(pathSeparator)) {
            if (isJarFile(path)) {
                Driver driver;
                try {
                    driver = getDriver(new File(path), url, cl);
                    if (driver != null) {
                        drivers.add(driver);
                        return driver;
                    }
                } catch (IOException ex) {
                    log.warn(ex);
                }
            }
        }
        throw new SQLException("driver not found");
    }

    private static Driver getDriver(File jar, String url, ClassLoader cl) throws IOException {
        ZipFile zipFile = new ZipFile(jar);
        try {
            for (ZipEntry entry : Collections.list(zipFile.entries())) {
                final String name = entry.getName();
                if (name.endsWith(".class")) {
                    final String fqcn = name.replaceFirst("\\.class", "").replace('/', '.');
                    try {
                        Class<?> c = DynamicLoader.loadClass(fqcn, cl);
                        if (Driver.class.isAssignableFrom(c)) {
                            Driver driver = (Driver)c.newInstance();
                            if (driver.acceptsURL(url)) {
                                return driver;
                            }
                        }
                    } catch (Exception ex) {
                        log.trace(ex);
                    }
                }
            }
        } finally {
            zipFile.close();
        }
        return null;
    }

    private static List<File> getJarFiles(String path) {
        File root = new File(path);
        File[] files = root.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<File> jars = new ArrayList<>();
        for (File file : files) {
            if (isJarFile(file)) {
                jars.add(file);
            }
        }
        return jars;
    }

    private static boolean isJarFile(File file) {
        return isJarFile(file.getPath());
    }

    private static boolean isJarFile(String path) {
        return path.matches("(?i).+\\.(jar|zip)");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().length() == 0;
    }

}
