package org.atlasapi.feeds.youview.payload;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import tva.metadata._2010.TVAMainType;

import com.google.common.base.Charsets;


public class TVAnytimeStringConverter implements Converter<JAXBElement<TVAMainType>, String> {
    
    private final JAXBContext context;
    
    public TVAnytimeStringConverter() throws JAXBException {
        this.context = JAXBContext.newInstance("tva.metadata._2010");
    }

    private static final Pattern TVAMAIN_PATTERN = Pattern.compile("<TVAMain[^>]+>");
    private static final Pattern NS_PATTERN = Pattern.compile("</?\bns[0-9]?:");

    @Override
    public String convert(JAXBElement<TVAMainType> input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(input, baos);
            String s = baos.toString(Charsets.UTF_8.name());
            /* TODO FIXME This output hack was put it to support weirdness from YouView */
            s = s.replace(" xsi:nil=\"true\"", "");
            //We have seen namespaces swapping order in the TVAMain element tag. This causes
            //different hashes without actual difference in the xml. We will remove the namespacing.
            s = TVAMAIN_PATTERN.matcher(s).replaceAll("");
            s = NS_PATTERN.matcher(s).replaceAll("");
            return s;
        } catch (JAXBException | UnsupportedEncodingException e) {
            throw new TVAnytimeConverterException(e);
        }
    }
}
