package org.example.writer;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.example.jaxb.People;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.SAXException;
import java.io.File;

public class JaxbWriter {

    public static void write(People people, String xmlFile, String xsdFile) throws Exception {
        JAXBContext context = JAXBContext.newInstance(People.class);
        Marshaller marshaller = context.createMarshaller();

        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        // Подключаем XSD для валидации
        SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = sf.newSchema(new File(xsdFile));
        marshaller.setSchema(schema);

        marshaller.marshal(people, new File(xmlFile));
    }
}