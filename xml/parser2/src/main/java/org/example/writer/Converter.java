package org.example.writer;

import org.example.model.Person;
import org.example.jaxb.People;
import org.example.jaxb.PersonJAXB;

import java.util.HashMap;
import java.util.Map;

public class Converter {

    public static People convert(Map<String, Person> peopleMap) {
        People people = new People();
        Map<String, PersonJAXB> jaxbMap = new HashMap<>();

        // Создаём JAXB объекты и заполняем firstName/lastName/gender
        for (Person p : peopleMap.values()) {
            PersonJAXB pj = new PersonJAXB();
            pj.id = p.id;
            // Если имени нет, не подставляем текст "Unknown", а оставляем пустую строку,
            // чтобы "Unknown" не появлялось как реальное имя человека.
            pj.firstName = (p.firstName == null || p.firstName.isBlank()) ? "" : p.firstName;
            pj.lastName = p.lastName;
            pj.gender = (p.gender == null || p.gender.isBlank()) ? "unknown" : p.gender;

            jaxbMap.put(p.id, pj);
            people.personList.add(pj);
        }

        // Разрешаем связи через IDREF
        for (Person p : peopleMap.values()) {
            PersonJAXB pj = jaxbMap.get(p.id);

            for (String spouseId : p.spouses) if (jaxbMap.containsKey(spouseId))
                pj.spouses.add(jaxbMap.get(spouseId));
            for (String parentId : p.parents) if (jaxbMap.containsKey(parentId))
                pj.parents.add(jaxbMap.get(parentId));
            for (String childId : p.children) if (jaxbMap.containsKey(childId))
                pj.children.add(jaxbMap.get(childId));
            for (String broId : p.brothers) if (jaxbMap.containsKey(broId))
                pj.brothers.add(jaxbMap.get(broId));
            for (String sisId : p.sisters) if (jaxbMap.containsKey(sisId))
                pj.sisters.add(jaxbMap.get(sisId));
        }

        return people;
    }
}