package stew6.io;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * XmlRowEntity is an entity for JAXB marshalling in XmlImporter/Exporter.
 */
@XmlRootElement(name = "row")
@XmlAccessorType(XmlAccessType.NONE)
public final class XmlRowEntity {

    @XmlElementWrapper(name = "values")
    @XmlElement(name = "value")
    private List<? extends Object> values;

    public XmlRowEntity() {
        this.values = new ArrayList<>();
    }

    public XmlRowEntity(List<? extends Object> row) {
        this.values = row;
    }

    public List<?> getValues() {
        return values;
    }

}
