package stew6.io;

import java.io.*;

/**
 * A basic implementation of Exporter.
 */
public abstract class Exporter implements AutoCloseable {

    protected OutputStream os;
    protected boolean wasWrittenHeader;

    private boolean closed;

    /**
     * A constructor.
     * @param os
     */
    protected Exporter(OutputStream os) {
        this.os = os;
        this.wasWrittenHeader = false;
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
     * Adds a header.
     * This method can be called only once after opening stream.
     * @param header
     * @throws IOException a header was already written, or another I/O error
     */
    public void addHeader(Object... header) throws IOException {
        ensureOpen();
        if (wasWrittenHeader) {
            throw new IOException("header was already written");
        }
        writeHeader(header);
        wasWrittenHeader = true;
    }

    /**
     * Writes a header.
     * @param header
     * @throws IOException
     */
    protected void writeHeader(Object[] header) throws IOException {
        ensureOpen();
        addRow(header);
    }

    /**
     * Closes stream.
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        ensureOpen();
        if (os != null) {
            try {
                os.close();
            } finally {
                closed = true;
                os = null;
            }
        }
    }

    /**
     * Returns an Exporter.
     * @param file
     * @return
     * @throws IOException
     */
    public static Exporter getExporter(File file) throws IOException {
        return ExporterFactory.createExporter(file);
    }

    /**
     * Returns an Exporter.
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Exporter getExporter(String fileName) throws IOException {
        return ExporterFactory.createExporter(new File(fileName));
    }

    /**
     * Adds a row.
     * @param values
     * @throws IOException
     */

    public abstract void addRow(Object... values) throws IOException;

}
