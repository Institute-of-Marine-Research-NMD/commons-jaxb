package no.imr.nmdapi.common.jaxb.converters;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import no.imr.nmdapi.common.jaxb.exceptions.ConversionException;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
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
     * Initalizes the convertes.
     *
     * @param nsMapper
     * @param packages All packages that contains supported jaxb classes.
     */
    public JAXBHttpMessageConverter(NamespacePrefixMapper nsMapper, String... packages) throws JAXBException {
        super(MediaType.APPLICATION_XML);
        LOGGER.info("Initalize");
        for (String pack : packages) {
            for (Class<?> clazz : new Reflections(pack).getTypesAnnotatedWith(XmlRootElement.class)) {
                LOGGER.info("JAXB message converter initalize class: " + clazz.getName());
                supportedClasses.add(clazz);
            }
        }
        this.nsMapper = nsMapper;
        jaxbContext = JAXBContext.newInstance(supportedClasses.toArray(new Class[supportedClasses.size()]));
        LOGGER.info("Initalization complete");
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return supportedClasses.contains(clazz);
    }

    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage) throws IOException {
        LOGGER.info("Unmarshall start");
        try {
            return jaxbContext.createUnmarshaller().unmarshal(inputMessage.getBody());
        } catch (JAXBException e) {
            throw new ConversionException("Could not complete unmarshalling", e);
        }
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage) throws IOException {
        LOGGER.info("Marshall start");
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", nsMapper);
            marshaller.marshal(o, outputMessage.getBody());
        } catch (JAXBException e) {
            throw new ConversionException("Could not complete marshalling", e);
        }
        LOGGER.info("Marshall complete");
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return canRead(mediaType) && supports(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return canWrite(mediaType) && supports(clazz);
    }

}