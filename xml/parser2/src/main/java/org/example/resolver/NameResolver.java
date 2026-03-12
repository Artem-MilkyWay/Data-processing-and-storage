package org.example.resolver;

import org.example.model.Person;
import org.example.parser.PeopleSaxHandler.RawRelation;

import java.util.*;

public class NameResolver {

    public static void resolve(Map<String, Person> people,
                               Map<String, List<RawRelation>> raw) {

        Map<String, String> nameToId = new HashMap<>();
        for (Person p : people.values()) {
            String name = p.fullName();
            if (!name.isBlank()) nameToId.put(name, p.id);
        }

        for (var entry : raw.entrySet()) {
            Person owner = people.get(entry.getKey());

            for (RawRelation r : entry.getValue()) {
                String targetId = nameToId.get(r.value);
                String normalizedTag = r.tag.toLowerCase();

                if (targetId == null) {
                    targetId = "AUTO_" + UUID.randomUUID();
                    Person p = new Person();
                    p.id = targetId;
                    p.setFullName(r.value);
                     // Пытаемся угадать пол по типу связи
                    switch (normalizedTag) {
                        case "father", "husband", "son", "brother" -> p.gender = "M";
                        case "mother", "wife", "daughter", "sister" -> p.gender = "F";
                    }
                    people.put(targetId, p);
                    nameToId.put(r.value, targetId);
                }

                switch (normalizedTag) {
                    case "parent", "father", "mother" -> {
                        owner.parents.add(targetId);
                        Person parent = people.get(targetId);
                        if (parent != null && (parent.gender == null || parent.gender.isBlank() || parent.gender.equalsIgnoreCase("unknown"))) {
                            if ("father".equals(normalizedTag)) parent.gender = "M";
                            if ("mother".equals(normalizedTag)) parent.gender = "F";
                        }
                    }
                    case "child", "son", "daughter" -> owner.children.add(targetId);
                    case "brother" -> owner.brothers.add(targetId);
                    case "sister" -> owner.sisters.add(targetId);
                    case "sibling" -> {
                        owner.brothers.add(targetId);
                        owner.sisters.add(targetId);
                    }
                    case "spouse", "husband", "wife" -> owner.spouses.add(targetId);
                }
            }
        }
    }
}