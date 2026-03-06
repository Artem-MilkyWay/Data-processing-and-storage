package org.example;

import org.example.model.Person;
import org.example.parser.PeopleSaxHandler;
import org.example.resolver.NameResolver;
import org.example.validator.ConsistencyValidator;
import org.example.jaxb.People;
import org.example.writer.Converter;
import org.example.writer.JaxbWriter;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import jakarta.xml.bind.JAXBException;
import org.example.writer.XmlWriter;

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

        People jaxbPeople = Converter.convert(people);

        File xsdFile;
        try {
            xsdFile = new File(Main.class.getClassLoader().getResource("family.xsd").toURI());
        } catch (NullPointerException | java.net.URISyntaxException e) {
            throw new RuntimeException("Не найден файл family.xsd в resources", e);
        }

        try {
            JaxbWriter.write(jaxbPeople, "output_validated.xml", xsdFile.getAbsolutePath());
            System.out.println("XML успешно записан и валидирован по XSD!");
        } catch (JAXBException e) {
            System.err.println("Ошибка JAXB при записи XML: " + e);
            if (e.getLinkedException() != null) {
                System.err.println("Внутренняя причина: " + e.getLinkedException().getMessage());
                e.getLinkedException().printStackTrace();
            }
        }
    }
}