package stew6.io;

import java.io.*;

/**
 * A basic implementation of Importer.
 */
public abstract class Importer implements AutoCloseable {

    protected InputStream is;
    protected boolean isHeaderReady;
    protected Object[] header;

    private boolean closed;

    /**
     * A constructor.
     * @param is InputStream
     */
    protected Importer(InputStream is) {
        this.is = is;
        this.isHeaderReady = false;
        this.header = new Object[0];
        this.closed = false;
    }

    /**
     * Ensures that this stream is opened.
     * @throws IOException this stream was closed
     */
    protected final void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("stream closed");
        }
    }

    /**
     * Returns the header.
     * @return
     * @throws IOException
     */
    public Object[] getHeader() throws IOException {
        ensureOpen();
        if (!isHeaderReady) {
            this.header = readHeader();
            isHeaderReady = true;
        }
        return header;
    }

    /**
     * Reads the header.
     * @return
     * @throws IOException
     */
    protected Object[] readHeader() throws IOException {
        ensureOpen();
        return nextRow();
    }

    /**
     * Closes this stream.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        ensureOpen();
        if (is != null) {
            try {
                is.close();
            } finally {
                closed = true;
                is = null;
            }
        }
    }

    /**
     * Returns an Importer.
     * @param file
     * @return
     * @throws IOException
     */
    public static Importer getImporter(File file) throws IOException {
        return ImporterFactory.createImporter(file);
    }

    /**
     * Returns an Importer.
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Importer getImporter(String fileName) throws IOException {
        return ImporterFactory.createImporter(new File(fileName));
    }

    /**
     * Returns the next row.
     * @return nex row. when it has no more row, returns empty array
     * @throws IOException
     */
    public abstract Object[] nextRow() throws IOException;

}
