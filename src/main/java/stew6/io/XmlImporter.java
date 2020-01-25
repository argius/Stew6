package stew6.io;

import java.io.*;
import java.util.*;
import javax.xml.bind.*;
import javax.xml.datatype.*;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

/**
 * XmlImporter provides a feature that reads data from XML file.
 */
public final class XmlImporter extends Importer {

    private static String TAG_HEADERROW = "headerrow";
    private static String TAG_HEADER = "header";
    private static String TAG_ROW = "row";

    private XMLEventReader xer;

    /**
     * A constructor.
     * @param is InputStream
     * @throws IOException
     */
    public XmlImporter(InputStream is) throws IOException {
        super(is);
        try {
            this.xer = XMLInputFactory.newFactory().createXMLEventReader(is);
        } catch (XMLStreamException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected Object[] readHeader() throws IOException {
        ensureOpen();
        try {
            // tracking headerrow element
            while (xer.hasNext()) {
                XMLEvent evt = xer.peek();
                if (evt.isStartElement() && isElementNameEquals(evt, TAG_HEADERROW)) {
                    xer.nextEvent();
                    break;
                }
                xer.nextEvent();
            }
            List<Object> a = new ArrayList<>();
            while (xer.hasNext()) {
                XMLEvent evt0 = xer.peek();
                if (evt0.isStartElement() && evt0.asStartElement().getName().getLocalPart().equals(TAG_ROW)) {
                    break;
                }
                XMLEvent evt1 = xer.nextEvent(); // header:start
                if (evt1.isStartElement() && evt1.asStartElement().getName().getLocalPart().equals(TAG_HEADER)) {
                    XMLEvent evt2 = xer.nextEvent(); // header:characters
                    if (!evt2.isCharacters()) {
                        throw new XMLStreamException("expects char event(4), but " + evt2.getEventType());
                    }
                    a.add(evt2.asCharacters().getData());
                    XMLEvent evt3 = xer.nextEvent(); // header:end
                    if (!evt3.isEndElement() || !evt3.asEndElement().getName().getLocalPart().equals(TAG_HEADER)) {
                        throw new XMLStreamException("expects end event(2), but " + evt2.getEventType());
                    }
                }
            }
            return a.toArray();
        } catch (XMLStreamException | RuntimeException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public Object[] nextRow() throws IOException {
        ensureOpen();
        try {
            // tracking row element
            while (xer.hasNext()) {
                XMLEvent evt = xer.peek();
                if (evt.isStartElement() && isElementNameEquals(evt, "row")) {
                    break;
                }
                xer.nextEvent();
            }
            if (!xer.hasNext()) {
                return new Object[0];
            }
            JAXBContext jc = JAXBContext.newInstance(XmlRowEntity.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            XmlRowEntity o = (XmlRowEntity)unmarshaller.unmarshal(xer);
            return convert(o.getValues());
        } catch (JAXBException | XMLStreamException | RuntimeException ex) {
            throw new IOException(ex);
        }
    }

    static Object[] convert(List<?> row) {
        Object[] a = new Object[row.size()];
        for (int i = 0; i < a.length; i++) {
            final Object o = row.get(i);
            a[i] = (o instanceof XMLGregorianCalendar) ? ((XMLGregorianCalendar)o).toGregorianCalendar().getTime() : o;
        }
        return a;
    }

    static boolean isElementNameEquals(XMLEvent evt, String name) {
        switch (evt.getEventType()) {
            case XMLEvent.START_ELEMENT:
                return evt.asStartElement().getName().getLocalPart().equals(name);
            case XMLEvent.END_ELEMENT:
                return evt.asEndElement().getName().getLocalPart().equals(name);
            default:
        }
        throw new IllegalStateException("unexpected event: " + evt);
    }

    @Override
    public void close() throws IOException {
        try {
            // this.xer = null;
        } finally {
            super.close();
        }
    }

}
