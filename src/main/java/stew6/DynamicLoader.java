package stew6;

import java.net.*;
import java.security.*;

/**
 * DynamicLoader provides functions to load Class dynamically.
 */
public final class DynamicLoader {

    private DynamicLoader() {
        // empty
    }

    /**
     * Loads the Class specified by the name.
     * @param <T>
     * @param className
     * @return
     * @throws DynamicLoadingException
     */
    public static <T> Class<T> loadClass(String className) throws DynamicLoadingException {
        return loadClass(className, ClassLoader.getSystemClassLoader());
    }

    /**
     * Loads the Class specified by the name and ClassLoader.
     * @param <T>
     * @param className
     * @param classLoader
     * @return
     * @throws DynamicLoadingException
     */
    public static <T> Class<T> loadClass(String className, ClassLoader classLoader) throws DynamicLoadingException {
        try {
            @SuppressWarnings("unchecked")
            Class<T> c = (Class<T>)classLoader.loadClass(className);
            return c;
        } catch (Throwable th) {
            throw new DynamicLoadingException("class loading error", th);
        }
    }

    /**
     * Creates a new instance.
     * @param <T>
     * @param className
     * @return
     * @throws DynamicLoadingException
     */
    public static <T> T newInstance(String className) throws DynamicLoadingException {
        // T o = newInstance(className, DynamicLoader.class.getClassLoader());
        @SuppressWarnings("unchecked")
        T o = (T)newInstance(className, DynamicLoader.class.getClassLoader());
        return o;
    }

    /**
     * Creates a new instance.
     * @param <T>
     * @param className
     * @param urls
     * @return
     * @throws DynamicLoadingException
     */
    public static <T> T newInstance(String className, URL... urls) throws DynamicLoadingException {
        // T o = newInstance(className, getURLClassLoader(urls));
        @SuppressWarnings("unchecked")
        T o = (T)newInstance(className, getClassLoader(urls));
        return o;
    }

    /**
     * Creates a new instance.
     * @param <T>
     * @param className
     * @param classLoader
     * @return
     * @throws DynamicLoadingException
     */
    public static <T> T newInstance(String className, ClassLoader classLoader) throws DynamicLoadingException {
        try {
            Class<T> c = loadClass(className, classLoader);
            return c.newInstance();
        } catch (DynamicLoadingException ex) {
            throw ex;
        } catch (Throwable th) {
            throw new DynamicLoadingException("load error: " + className, th);
        }
    }

    /**
     * Creates a new instance.
     * @param <T>
     * @param classObject
     * @return
     * @throws DynamicLoadingException
     */
    public static <T> T newInstance(Class<T> classObject) throws DynamicLoadingException {
        try {
            return classObject.newInstance();
        } catch (Throwable th) {
            throw new DynamicLoadingException("load error: " + classObject, th);
        }
    }

    /**
     * Returns a new URLClassLoader that creates from URL.
     * @param urls
     * @return
     */
    public static URLClassLoader getClassLoader(URL... urls) {
        return (URLClassLoader)AccessController.doPrivileged(new GettingURLClassLoaderPrivilegedAction(urls));
    }

    private static final class GettingURLClassLoaderPrivilegedAction implements PrivilegedAction<Object> {

        private final URL[] urls;

        GettingURLClassLoaderPrivilegedAction(URL[] urls) {
            this.urls = urls;
        }

        @Override
        public Object run() {
            return new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
        }

    }

}
