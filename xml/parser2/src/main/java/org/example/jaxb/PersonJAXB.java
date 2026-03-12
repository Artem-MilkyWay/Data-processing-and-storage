package org.example.jaxb;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"spouses", "parents", "children", "brothers", "sisters"})
public class PersonJAXB {

    @XmlAttribute(required = true)
    @XmlID
    public String id;

    @XmlAttribute(required = true)
    public String firstName;

    @XmlAttribute
    public String lastName;

    @XmlAttribute(required = true)
    public String gender;

    @XmlElement(name = "spouse")
    @XmlIDREF
    public List<PersonJAXB> spouses = new ArrayList<>();

    @XmlElement(name = "parent")
    @XmlIDREF
    public List<PersonJAXB> parents = new ArrayList<>();

    @XmlElement(name = "child")
    @XmlIDREF
    public List<PersonJAXB> children = new ArrayList<>();

    @XmlElement(name = "brother")
    @XmlIDREF
    public List<PersonJAXB> brothers = new ArrayList<>();

    @XmlElement(name = "sister")
    @XmlIDREF
    public List<PersonJAXB> sisters = new ArrayList<>();
}