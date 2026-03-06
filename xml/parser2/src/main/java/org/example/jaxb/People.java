package org.example.jaxb;

import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "people")
@XmlAccessorType(XmlAccessType.FIELD)
public class People {

    @XmlElement(name = "person")
    public List<PersonJAXB> personList = new ArrayList<>();
}