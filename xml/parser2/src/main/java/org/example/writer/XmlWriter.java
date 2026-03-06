package org.example.writer;

import org.example.model.Person;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class XmlWriter {

    private static void nl(XMLStreamWriter w, int indent) throws Exception {
        w.writeCharacters("\n");
        for (int i = 0; i < indent; i++) {
            w.writeCharacters("    ");
        }
    }

    public static void write(String file, Map<String, Person> people) throws Exception {
        XMLOutputFactory f = XMLOutputFactory.newInstance();
        XMLStreamWriter w = f.createXMLStreamWriter(new FileOutputStream(file), "UTF-8");

        w.writeStartDocument("UTF-8", "1.0");
        w.writeCharacters("\n");
        w.writeStartElement("people");

        for (Person p : people.values()) {
            if (p.id == null) continue;

            nl(w, 1);
            w.writeStartElement("person");
            w.writeAttribute("id", p.id);

            int siblingsCount = p.brothers.size() + p.sisters.size();
            w.writeAttribute("siblings", String.valueOf(siblingsCount));

            if (p.firstName != null || p.lastName != null) {
                nl(w, 2);
                w.writeStartElement("name");

                if (p.firstName != null) {
                    nl(w, 3);
                    w.writeStartElement("first");
                    w.writeCharacters(p.firstName);
                    w.writeEndElement();
                }

                if (p.lastName != null) {
                    nl(w, 3);
                    w.writeStartElement("last");
                    w.writeCharacters(p.lastName);
                    w.writeEndElement();
                }

                nl(w, 2);
                w.writeEndElement();
            }

            if (p.gender != null) {
                nl(w, 2);
                w.writeStartElement("gender");
                w.writeCharacters(p.gender);
                w.writeEndElement();
            }

            nl(w, 2);
            w.writeStartElement("family");

            if (!p.parents.isEmpty()) {
                nl(w, 3);
                w.writeStartElement("parents");
                for (String id : p.parents) {
                    nl(w, 4);
                    w.writeEmptyElement("parent");
                    w.writeAttribute("ref", id);
                }
                nl(w, 3);
                w.writeEndElement();
            }

            for (String id : p.spouses) {
                nl(w, 3);
                w.writeEmptyElement("spouse");
                w.writeAttribute("ref", id);
            }

            if (!p.brothers.isEmpty() || !p.sisters.isEmpty()) {
                nl(w, 3);
                w.writeStartElement("siblings");

                for (String id : p.brothers) {
                    nl(w, 4);
                    w.writeEmptyElement("brother");
                    w.writeAttribute("ref", id);
                }

                for (String id : p.sisters) {
                    nl(w, 4);
                    w.writeEmptyElement("sister");
                    w.writeAttribute("ref", id);
                }

                nl(w, 3);
                w.writeEndElement();
            }

            nl(w, 2);
            w.writeEndElement();

            nl(w, 1);
            w.writeEndElement();
        }

        w.writeCharacters("\n");
        w.writeEndElement();
        w.writeEndDocument();
        w.close();
    }
}