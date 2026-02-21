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
    }
}
