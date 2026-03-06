package org.example.jaxb;

import jakarta.xml.bind.annotation.*;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class Family {

    @XmlElement(name = "parent")
    public List<Ref> parents;

    @XmlElement(name = "child")
    public List<Ref> children;

    @XmlElement(name = "brother")
    public List<Ref> brothers;

    @XmlElement(name = "sister")
    public List<Ref> sisters;

    @XmlElement(name = "spouse")
    public List<Ref> spouses;

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Ref {
        @XmlAttribute(name = "ref", required = true)
        public String ref;
    }
}