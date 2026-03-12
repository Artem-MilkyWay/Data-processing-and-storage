package org.example.parser;

import org.example.model.Person;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.util.*;

public class PeopleSaxHandler extends DefaultHandler {

    private final Map<String, Person> people = new HashMap<>();
    private final Map<String, List<RawRelation>> rawRelations = new HashMap<>();
    private Person current;
    private String currentId;
    private String currentTag;
    private String attrVal;
    private String attrCount;
    private final StringBuilder text = new StringBuilder(128);

    public Map<String, Person> getPeople() {
        return people;
    }

    public Map<String, List<RawRelation>> getRawRelations() {
        return rawRelations;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attrs) {
        currentTag = qName.toLowerCase();
        text.setLength(0);
        attrVal = null;
        attrCount = null;

        for (int i = 0, n = attrs.getLength(); i < n; i++) {
            String an = attrs.getQName(i).toLowerCase();
            String av = attrs.getValue(i) == null ? null : attrs.getValue(i).trim();
            if (av == null || av.isEmpty()) continue;

            if (an.equals("value") || an.equals("val") || an.equals("id") || an.equals("ref")) {
                attrVal = av;
            } else if (an.equals("count")) {
                attrCount = av;
            } else if ("name".equals(an) && "person".equals(currentTag)) {
                // name у person — сразу разбираем как полное имя
                if (current != null) parseName(av, current);
            } else if ("firstname".equals(an) && "person".equals(currentTag) && current != null) {
                current.firstName = av;
            } else if (("lastname".equals(an) || "surname".equals(an)) && "person".equals(currentTag) && current != null) {
                current.lastName = av;
            } else if (("gender".equals(an) || "sex".equals(an)) && "person".equals(currentTag) && current != null) {
                current.gender = normGender(av);
            }
        }

        if ("person".equals(currentTag)) {
            currentId = attrVal;
            if (currentId != null && !currentId.isBlank()) {
                current = people.get(currentId);
                if (current == null) {
                    current = new Person();
                    current.id = currentId;
                    people.put(currentId, current);
                }
            } else {
                current = null;
            }
        } else if (current != null) {
            applyAttr(currentTag, attrVal, attrCount, current);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        if (current != null && currentTag != null && length > 0) {
            text.append(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String endName = qName.toLowerCase();

        if ("person".equals(endName)) {
            current = null;
            currentId = null;
            currentTag = null;
            text.setLength(0);
            return;
        }

        if (current != null) {
            String t = text.toString().trim();
            if (!t.isEmpty()) {
                applyText(endName, t, current);
            }
            text.setLength(0);
            currentTag = null;
        }
    }

    // ---------------------- applyAttr для модели Person ----------------------
    private void applyAttr(String tag, String val, String count, Person p) {
        if (val == null && count == null) return;

        String t = tag.toLowerCase();

        switch (t) {
            case "firstname": case "first-name": case "first_name":
                if (val != null) p.firstName = val;
                break;

            case "lastname": case "last-name": case "last_name": case "surname":
                if (val != null) p.lastName = val;
                break;

            case "name": case "fullname": case "full-name":
                if (val != null) parseName(val, p);
                break;

            case "gender": case "sex":
                if (val != null) p.gender = normGender(val);
                break;

            case "husband":
                if (p.gender == null) p.gender = "F";
                handleRelation("husband", val, p);
                break;
            case "wife":
                if (p.gender == null) p.gender = "M";
                handleRelation("wife", val, p);
                break;
            case "spouse":
                handleRelation("spouse", val, p);
                break;
            case "father":
                handleRelation("father", val, p);
                break;
            case "mother":
                handleRelation("mother", val, p);
                break;
            case "parent":
                handleRelation("parent", val, p);
                break;
            case "child": case "son": case "daughter":
                handleRelation(t, val, p);
                break;
            case "children": case "children-number": case "childrencount": case "children-count":
                String c = count != null ? count : val;
                // В этой модели declaredChildrenCount нет, поэтому просто игнорируем
                if (c != null) { /* no-op */ }
                break;
            case "siblings": case "sibling":
            case "brother":
            case "sister":
                handleRelation(t, val, p);
                break;
        }
    }

    // ---------------------- applyText для модели Person ----------------------
    private void applyText(String tag, String t, Person p) {
        String value = t == null ? "" : t.trim();
        if (value.isEmpty()) return;

        String tt = tag.toLowerCase();

        switch (tt) {
            case "firstname": case "first-name": case "first_name": case "first":
                p.firstName = value; break;
            case "lastname": case "last-name": case "last_name": case "surname": case "family":
                p.lastName = value; break;
            case "name": case "fullname": case "full-name":
                parseName(value, p); break;
            case "gender": case "sex":
                p.gender = normGender(value); break;

            case "husband":
                if (p.gender == null) p.gender = "F";
                handleRelation("husband", value, p);
                break;
            case "wife":
                if (p.gender == null) p.gender = "M";
                handleRelation("wife", value, p);
                break;
            case "spouse":
                handleRelation("spouse", value, p);
                break;
            case "father":
                handleRelation("father", value, p);
                break;
            case "mother":
                handleRelation("mother", value, p);
                break;
            case "parent":
                handleRelation("parent", value, p);
                break;
            case "child": case "son": case "daughter":
                handleRelation(tt, value, p);
                break;
            case "children": case "children-number": case "childrencount": case "children-count":
                // declaredChildrenCount отсутствует — пропускаем
                try { Integer.parseInt(value); } catch (NumberFormatException ignored) {}
                break;
            case "siblings": case "sibling":
            case "brother":
            case "sister":
                handleRelation(tt, value, p);
                break;
        }
    }

    private boolean isId(String s) {
        return s != null && s.length() > 1 && s.charAt(0) == 'P' && Character.isDigit(s.charAt(1));
    }

    private void handleRelation(String tag, String val, Person owner) {
        if (val == null) return;
        String v = val.trim();
        if (v.isEmpty()) return;
        if (v.equalsIgnoreCase("UNKNOWN")) return;

        boolean id = isId(v) || v.startsWith("AUTO_");

        if (!id && owner.id != null) {
            rawRelations
                    .computeIfAbsent(owner.id, k -> new ArrayList<>())
                    .add(new RawRelation(tag, v));
            return;
        }

        // заполняем множества ID
        switch (tag) {
            case "husband":
            case "wife":
            case "spouse":
                owner.spouses.add(v); break;
            case "father":
            case "mother":
            case "parent":
                owner.parents.add(v); break;
            case "child":
            case "son":
            case "daughter":
                owner.children.add(v); break;
            case "siblings":
            case "sibling":
                for (String sid : v.split("\\s+")) {
                    if (sid.isBlank()) continue;
                    owner.brothers.add(sid);
                    owner.sisters.add(sid);
                }
                break;
            case "brother":
                owner.brothers.add(v); break;
            case "sister":
                owner.sisters.add(v); break;
        }
    }

    // ---------------------- Вспомогательные методы ----------------------
    private String normGender(String g) {
        if (g == null || g.isEmpty()) return null;
        char c = Character.toLowerCase(g.charAt(0));
        return (c == 'm') ? "M" : (c == 'f' || c == 'w') ? "F" : null;
    }

    private void parseName(String fullName, Person p) {
        if(fullName == null || fullName.isBlank()) return;
        String[] parts = fullName.trim().split("\\s+", 2);
        if (p.firstName == null && parts.length >= 1) p.firstName = parts[0];
        if (p.lastName == null && parts.length == 2) p.lastName = parts[1];
    }

    // ---------------------- RawRelation ----------------------
    public static class RawRelation {
        public final String tag;
        public final String value;
        public RawRelation(String tag, String value) { this.tag = tag; this.value = value; }
    }
}