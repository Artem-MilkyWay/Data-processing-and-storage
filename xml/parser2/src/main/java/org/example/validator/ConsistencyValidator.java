package org.example.validator;

import org.example.model.Person;

import java.util.*;

public class ConsistencyValidator {

    public static void validate(Map<String, Person> people) {

        for (Person p : people.values()) {
            for (String spouseId : p.spouses) {
                Person s = people.get(spouseId);
                if (s != null) s.spouses.add(p.id);
            }

            for (String parentId : p.parents) {
                Person parent = people.get(parentId);
                if (parent != null) parent.children.add(p.id);
            }

            for (String childId : p.children) {
                Person child = people.get(childId);
                if (child != null) child.parents.add(p.id);
            }

            for (String bro : p.brothers) {
                Person b = people.get(bro);
                if (b != null) b.brothers.add(p.id);
            }

            for (String sis : p.sisters) {
                Person s = people.get(sis);
                if (s != null) s.sisters.add(p.id);
            }
        }

        // Дополнительное разрешение связей и догадка пола по супругам
        resolveRelations(people);
    }

    public static void resolveRelations(Map<String, Person> people) {
        for (Person p : people.values()) {
            // Если у человека известен пол, а у супруга он неизвестен/unknown — пытаемся вывести противоположный
            for (String spouseId : p.spouses) {
                Person spouse = people.get(spouseId);
                if (spouse == null) continue;

                boolean spouseGenderUnknown =
                        spouse.gender == null ||
                        spouse.gender.isBlank() ||
                        spouse.gender.equalsIgnoreCase("unknown");

                if (!spouseGenderUnknown) continue;

                if ("M".equals(p.gender)) {
                    spouse.gender = "F";
                } else if ("F".equals(p.gender)) {
                    spouse.gender = "M";
                }
            }
        }
    }
}
