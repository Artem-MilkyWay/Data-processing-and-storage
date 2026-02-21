package org.example;

import org.example.model.Person;
import org.example.parser.PeopleSaxHandler;
import org.example.resolver.NameResolver;
import org.example.validator.ConsistencyValidator;
import org.example.writer.XmlWriter;

import javax.xml.parsers.*;
import java.io.File;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        PeopleSaxHandler handler = new PeopleSaxHandler();
        parser.parse(new File("src/main/resources/people.xml"), handler);

        Map<String, Person> people = handler.getPeople();

        NameResolver.resolve(people, handler.getRawRelations());
        ConsistencyValidator.validate(people);

        XmlWriter.write("output.xml", people);

        System.out.println("Готово: output.xml");
    }
}
