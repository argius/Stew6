package stew6.io;

import java.io.*;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import stew6.*;

/**
 * XmlExporter provides a feature that writes data to file as XML.
 */
public final class XmlExporter extends Exporter {

    private static final String fqcn = XmlExporter.class.getName();

    private static final String TAG_TABLE = "table";
    private static final String TAG_HEADERROW = "headerrow";
    private static final String TAG_HEADER = "header";

    private XMLEventWriter xew;
    private XMLEventFactory xef;
    private Characters newLine;
    private boolean doneWriteBeginning;

    /**
     * A constructor.
     * @param os {@link OutputStream}
     */
    public XmlExporter(OutputStream os) throws IOException {
        super(os);
        try {
            this.xew = XMLOutputFactory.newFactory().createXMLEventWriter(os);
            this.xef = XMLEventFactory.newInstance();
            this.newLine = xef.createCharacters("\n");
        } catch (XMLStreamException | FactoryConfigurationError ex) {
            throw new IOException(ex);
        }
    }

    private void ensureOpen0() throws IOException {
        ensureOpen();
        if (!doneWriteBeginning) {
            try {
                writeBeginning();
            } catch (XMLStreamException ex) {
                throw new IOException(ex);
            }
            doneWriteBeginning = true;
        }
    }

    private void writeBeginning() throws XMLStreamException {
        xew.add(xef.createStartDocument());
        xew.add(newLine);
        xew.add(xef.createDTD("<!DOCTYPE table >"));
        xew.add(newLine);
        xew.add(xef.createStartElement(QName.valueOf(TAG_TABLE), null, null));
        xew.add(newLine);
        List<Attribute> attrs = Arrays.asList(xef.createAttribute("name", "generator"));
        xew.add(xef.createStartElement(QName.valueOf("meta"), attrs.iterator(), null));
        xew.add(xef.createCharacters(fqcn + " version " + App.getVersion()));
        xew.add(xef.createEndElement(QName.valueOf("meta"), null));
        xew.add(newLine);
        xew.flush();
    }

    @Override
    protected void writeHeader(Object[] header) throws IOException {
        ensureOpen0();
        try {
            QName parentTag = QName.valueOf(TAG_HEADERROW);
            QName childTag = QName.valueOf(TAG_HEADER);
            xew.add(xef.createStartElement(parentTag, null, null));
            for (Object o : header) {
                xew.add(xef.createStartElement(childTag, null, null));
                xew.add(xef.createCharacters(String.valueOf(o)));
                xew.add(xef.createEndElement(childTag, null));
            }
            xew.add(xef.createEndElement(parentTag, null));
            xew.add(newLine);
            xew.flush();
        } catch (FactoryConfigurationError | XMLStreamException | RuntimeException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void addRow(Object... values) throws IOException {
        ensureOpen0();
        try {
            JAXBContext jc = JAXBContext.newInstance(XmlRowEntity.class);
            Marshaller m = jc.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            XmlRowEntity row = new XmlRowEntity(Arrays.asList(values));
            m.marshal(row, xew);
            xew.add(newLine);
            xew.flush();
        } catch (JAXBException | XMLStreamException | RuntimeException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void close() throws IOException {
        ensureOpen0();
        try {
            if (xew != null) {
                try {
                    xew.add(xef.createEndElement(QName.valueOf(TAG_TABLE), null));
                } finally {
                    xew.close();
                }
            }
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        } finally {
            super.close();
        }
    }

}
