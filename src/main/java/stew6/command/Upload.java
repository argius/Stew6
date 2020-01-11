package stew6.command;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.sql.*;
import net.argius.stew.*;
import stew6.*;

/**
 * The Upload command used to upload a file into specified column.
 */
public final class Upload extends Command {

    private static final Logger log = Logger.getLogger(Upload.class);

    @Override
    public void execute(Connection conn, Parameter p) throws CommandException {
        if (!p.has(2)) {
            throw new UsageException(getUsage());
        }
        final File file = resolvePath(p.at(1));
        final String p2 = p.at(2);
        if (log.isDebugEnabled()) {
            log.debug("file=" + file.getAbsolutePath());
            log.debug("file length=%d", file.length());
            log.debug("p2=" + p2);
            log.debug("after3=" + p.after(3));
        }
        if (file.length() > Integer.MAX_VALUE) {
            throw new CommandException("file too large: " + file);
        }
        final int modeOption = getModeOption(p2);
        final String sql = p.after(modeOption > 0 ? 3 : 2);
        if (log.isDebugEnabled()) {
            log.debug("SQL=[%s]", sql);
            log.debug("modeOption=" + modeOption);
        }
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            uploadFile(stmt, file, modeOption);
        } catch (IOException | SQLException ex) {
            throw new CommandException(ex);
        }
    }

    void uploadFile(PreparedStatement stmt, File f, int modeOption) throws IOException, SQLException {
        setTimeout(stmt);
        final int length = (int)f.length();
        assert modeOption >= 0 && modeOption <= 2 : "mode option=" + modeOption;
        boolean requiresBinaryStream = false;
        if (modeOption == 0) {
            try {
                log.debug("getParameterMetaData");
                final int type = stmt.getParameterMetaData().getParameterType(1);
                final String typeString = SqlTypes.toTypeString(type);
                log.debug("destination type=[%s]", typeString);
                outputMessage("Upload.msg.destinationTypeIs", typeString);
                requiresBinaryStream = SqlTypes.shouldReadDataAsBinary(type);
            } catch (SQLException ex) {
                log.warn("failed to detect type with ParameterMetaData: %s", ex);
                outputMessage("Upload.msg.failedToDetectDestinationType", ex.getMessage());
                try {
                    final String detectedContentType = Files.probeContentType(f.toPath());
                    log.debug("detected content type=[%s]", detectedContentType);
                    outputMessage("Upload.msg.fileContentTypeIs", detectedContentType);
                    requiresBinaryStream = detectedContentType == null || !detectedContentType.startsWith("text/");
                } catch (IOException ex2) {
                    log.warn("failed to detect type by Files.probeContentType: %s", ex2);
                }
            }
        } else {
            requiresBinaryStream = modeOption == 1;
        }
        try (InputStream is = new FileInputStream(f)) {
            if (requiresBinaryStream) {
                stmt.setBinaryStream(1, is, length);
            } else {
                Charset charset = Charset.defaultCharset();
                Reader r = new InputStreamReader(is, charset);
                stmt.setCharacterStream(1, r, length);
            }
            final int updatedCount = stmt.executeUpdate();
            outputMessage("i.updated", updatedCount);
        }
    }

    static int getModeOption(String keyword) {
        switch (keyword.toUpperCase()) {
            case "T":
            case "TEXT":
                return 2; // Text (manually)
            case "B":
            case "BIN":
                return 1; // Binary (manually)
            case "UPDATE":
            case "INSERT":
                return 0; // Not specified (automatically)
            default:
                return -1; // Illegal argument
        }
    }

}
