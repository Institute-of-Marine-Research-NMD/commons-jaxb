package no.imr.nmdapi.common.jaxb.converters;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import no.imr.nmdapi.exceptions.ConversionException;
import no.imr.nmdapi.exceptions.S2DException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.xml.sax.SAXException;
/**
 *
 * @author kjetilf
 *
 * Converts the jaxb objects to and from xml.
 */
public class JAXBHttpMessageConverter extends AbstractHttpMessageConverter<Object> {

    /**
     * Class logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JAXBHttpMessageConverter.class);

    /**
     * Default encoding used.
     */
    public static final String ENCODING = "UTF-8";

    /**
     * A list of all supported classes. These must be jaxb annotated.
     */
    private final Set<Class<?>> supportedClasses = new HashSet<Class<?>>();

    /**
     * JaxB context.
     */
    private JAXBContext jaxbContext;

    /**
     * Namespace prefix mapper.
     */
    private final NamespacePrefixMapper nsMapper;

    /**
     * Telles the system if it should check for supportedclasses.
     * Reccomended is false.
     */
    private final boolean overrideSupport;

    /**
     *
     */
    private Schema schema = null;

    /**
     * Initalizes the convertes. Only XMLRoot elements in specified packages are supported.
     *
     * @param nsMapper
     * @param overrideSupport
     * @param schemaFile
     * @param packages All packages that contains supported jaxb classes.
     * @throws javax.xml.bind.JAXBException
     */
    public JAXBHttpMessageConverter(NamespacePrefixMapper nsMapper, boolean overrideSupport, URL schemaFile, String... packages) throws JAXBException {
        super(MediaType.APPLICATION_XML);
        LOGGER.info("Initalize");
        for (String pack : packages) {
            for (Class<?> clazz : new Reflections(pack).getTypesAnnotatedWith(XmlRootElement.class)) {
                LOGGER.info("JAXB message converter initalize class: " + clazz.getName());
                supportedClasses.add(clazz);
            }
        }
        this.schema = getSchema(schemaFile);
        this.nsMapper = nsMapper;
        this.overrideSupport = overrideSupport;
        jaxbContext = JAXBContext.newInstance(supportedClasses.toArray(new Class[supportedClasses.size()]));
        LOGGER.info("Initalization complete");
    }


    /**
     * Initalizes the convertes. Only XMLRoot elements in specified packages are supported.
     *
     * @param nsMapper
     * @param overrideSupport
     * @param packages All packages that contains supported jaxb classes.
     * @throws javax.xml.bind.JAXBException
     */
    public JAXBHttpMessageConverter(NamespacePrefixMapper nsMapper, boolean overrideSupport, String... packages) throws JAXBException {
        super(MediaType.APPLICATION_XML);
        LOGGER.info("Initalize");
        for (String pack : packages) {
            for (Class<?> clazz : new Reflections(pack).getTypesAnnotatedWith(XmlRootElement.class)) {
                LOGGER.info("JAXB message converter initalize class: " + clazz.getName());
                supportedClasses.add(clazz);
            }
        }
        this.nsMapper = nsMapper;
        this.overrideSupport = overrideSupport;
        jaxbContext = JAXBContext.newInstance(supportedClasses.toArray(new Class[supportedClasses.size()]));
        LOGGER.info("Initalization complete");
    }

    @Override
    public boolean supports(Class<?> clazz) {
        if (!overrideSupport) {
            return supportedClasses.contains(clazz);
        } else {
            return true;
        }
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        LOGGER.info("Unmarshall start");
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            if (this.schema != null) {
                unmarshaller.setSchema(schema);
            }
            unmarshaller.setEventHandler(new ValidationEventHandler() {

                public boolean handleEvent(ValidationEvent event) {
                    return false;
                };
            });
            return unmarshaller.unmarshal(inputMessage.getBody());
        } catch (JAXBException e) {
            String message = e.getMessage();
            if (message == null) {
                message = e.getCause().getMessage();
            }
            LOGGER.error("Could not complete unmarshalling", e);
            throw new ConversionException("Could not complete unmarshalling: ".concat(message != null ? message : ""), e);
        }
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException {
        LOGGER.info("Marshall start");
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", nsMapper);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, ENCODING);
            marshaller.marshal(o, outputMessage.getBody());
        } catch (JAXBException e) {
            LOGGER.error("Could not complete marshalling", e);
            throw new ConversionException("Could not complete marshalling".concat(e.getMessage()), e);
        }
        LOGGER.info("Marshall complete");
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return canRead(mediaType) && supports(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return canWrite(mediaType) && supportedClasses.contains(clazz);
    }

    private Schema getSchema(URL schemaFile) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            return schemaFactory.newSchema(schemaFile);
        } catch (SAXException ex) {
            LOGGER.error("Error importing schema definition.", ex);
            throw new S2DException("Error importing schema definition.", ex);
        }
    }

}
