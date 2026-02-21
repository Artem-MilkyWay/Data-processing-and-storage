package org.example.writer;

import org.example.model.Person;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class XmlWriter {

    public static void write(String file, Map<String, Person> people) throws Exception {
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        XMLStreamWriter w = f.createXMLStreamWriter(new FileOutputStream(file), "UTF-8");

        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("people");

        for (Person p : people.values()) {
            if (p.id == null) continue;

            w.writeStartElement("person");
            w.writeAttribute("id", p.id);

            if (p.firstName != null || p.lastName != null) {
                w.writeStartElement("name");
                if (p.firstName != null) {
                    w.writeStartElement("first");
                    w.writeCharacters(p.firstName);
                    w.writeEndElement();
                }
                if (p.lastName != null) {
                    w.writeStartElement("last");
                    w.writeCharacters(p.lastName);
                    w.writeEndElement();
                }
                w.writeEndElement();
            }

            if (p.gender != null) {
                w.writeStartElement("gender");
                w.writeCharacters(p.gender);
                w.writeEndElement();
            }

            w.writeStartElement("family");

            if (!p.parents.isEmpty()) {
                w.writeStartElement("parents");
                for (String id : p.parents) {
                    w.writeEmptyElement("parent");
                    w.writeAttribute("ref", id);
                }
                w.writeEndElement();
            }

            for (String id : p.spouses) {
                w.writeEmptyElement("spouse");
                w.writeAttribute("ref", id);
            }

            if (!p.children.isEmpty()) {
                w.writeStartElement("children");
                for (String id : p.children) {
                    w.writeEmptyElement("child");
                    w.writeAttribute("ref", id);
                }
                w.writeEndElement();
            }

            if (!p.brothers.isEmpty() || !p.sisters.isEmpty()) {
                w.writeStartElement("siblings");
                for (String id : p.brothers) {
                    w.writeEmptyElement("brother");
                    w.writeAttribute("ref", id);
                }
                for (String id : p.sisters) {
                    w.writeEmptyElement("sister");
                    w.writeAttribute("ref", id);
                }
                w.writeEndElement();
            }

            w.writeEndElement(); // family
            w.writeEndElement(); // person
        }

        w.writeEndElement();
        w.writeEndDocument();
        w.close();
    }
}
