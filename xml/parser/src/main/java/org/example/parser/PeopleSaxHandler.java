package org.example.parser;

import org.example.model.Person;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

public class PeopleSaxHandler extends DefaultHandler {

    private final Map<String, Person> people = new HashMap<>();
    private final Map<String, List<RawRelation>> rawRelations = new HashMap<>();
    private Person current;
    private StringBuilder buffer = new StringBuilder();

    public Map<String, Person> getPeople() {
        return people;
    }

    public Map<String, List<RawRelation>> getRawRelations() {
        return rawRelations;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        buffer.setLength(0);

        if (qName.equalsIgnoreCase("person")) {
            String id = attrs.getValue("id");
            if (id != null && !id.isBlank()) {
                current = people.computeIfAbsent(id, k -> new Person());
                current.id = id;
            }
            return;
        }

        if (current == null) return;

        String ref = attrs.getValue("ref");
        if (ref != null && !ref.isBlank()) {
            addRaw(qName.toLowerCase(), ref.trim());
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        buffer.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if (current == null) return;

        String value = buffer.toString().trim();
        if (!value.isEmpty()) {
            switch (qName.toLowerCase()) {
                case "first":
                case "firstname":
                    current.firstName = value;
                    break;
                case "last":
                case "lastname":
                    current.lastName = value;
                    break;
                case "gender":
                    current.gender = value;
                    break;
                case "child":
                case "parent":
                case "father":
                case "mother":
                case "son":
                case "daughter":
                case "brother":
                case "sister":
                case "sibling":
                case "spouse":
                case "husband":
                case "wife":
                    addRaw(qName.toLowerCase(), value);
                    break;
            }
        }
    }

    private void addRaw(String tag, String value) {
        rawRelations.computeIfAbsent(current.id, k -> new ArrayList<>())
                .add(new RawRelation(tag, value));
    }

    public static class RawRelation {
        public final String tag;
        public final String value;

        public RawRelation(String tag, String value) {
            this.tag = tag;
            this.value = value;
        }
    }
}
