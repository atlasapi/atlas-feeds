package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.atlasapi.feeds.youview.persistence.IdMappingStore;
import org.atlasapi.feeds.youview.resolutionapi.ResolutionApiOutput;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Optional;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.webapp.http.CacheHeaderWriter;
import com.metabroadcast.common.webapp.json.GsonFactory;

@Controller
public class NitroYouViewResolutionController {

    private static final String YOUVIEW_CRID_TYPE = "youview_crid";
    private static final String VERSION_PID_TYPE = "version_pid";
    private static final String NITRO_URI_PREFIX = "http://nitro.bbc.co.uk/programmes/";
    private static final Duration CACHE_DURATION = Duration.standardMinutes(16);
    private static final Gson GSON = GsonFactory.defaultGsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final IdMappingStore mappingStore;

    public NitroYouViewResolutionController(IdMappingStore mappingStore) {
        this.mappingStore = checkNotNull(mappingStore);
    }

    @RequestMapping(value = "/feeds/nitro-youview/version.json", method = RequestMethod.GET)
    public void getYouViewVersionCridJson(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "pid", required = false) String pid,
            @RequestParam(value = "crid", required = false) String crid)
            throws IOException {
        setHeaders(request, response, MimeType.APPLICATION_JSON.toString());
        serializeJson(response, outputFor(pid, crid));
    }

    @RequestMapping(value = "/feeds/nitro-youview/version.xml", method = RequestMethod.GET)
    public void getYouViewVersionCridXml(HttpServletRequest request, HttpServletResponse response,
            @RequestParam(value = "pid", required = false) String pid,
            @RequestParam(value = "crid", required = false) String crid)
            throws JAXBException, IOException {
        setHeaders(request, response, MimeType.APPLICATION_XML.toString());
        serializeXml(response, outputFor(pid, crid));
    }

    private void setHeaders(HttpServletRequest request, HttpServletResponse response, String contentType) {
        response.setContentType(contentType);
        CacheHeaderWriter cacheHeaderWriter = CacheHeaderWriter.cacheUntil(cacheUntil());
        cacheHeaderWriter.writeHeaders(request, response);
    }

    private DateTime cacheUntil() {
        return DateTime.now(DateTimeZone.UTC)
                .plus(CACHE_DURATION);
    }

    private ResolutionApiOutput outputFor(String pid, String crid) {
        if (pid == null && crid == null) {
            throw new IllegalArgumentException("Must specify pid or crid");
        }
        if (pid != null && crid != null) {
            throw new IllegalArgumentException("Must not specify both pid and crid");
        }
        
        if (pid != null) {
            return outputFor(VERSION_PID_TYPE, pid, YOUVIEW_CRID_TYPE, mappingStore.getValueFor(canonicalUriFor(pid)));
        } 
        
        return outputFor(YOUVIEW_CRID_TYPE, crid, VERSION_PID_TYPE, mappingStore.getKeyFor(crid));
    }

    private void serializeJson(HttpServletResponse response,
            ResolutionApiOutput output) throws IOException {
        GSON.toJson(output.getBlocklist(), response.getWriter());
    }


    private void serializeXml(HttpServletResponse response, ResolutionApiOutput output)
            throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(ResolutionApiOutput.class);
        Marshaller jaxbMarshaller = context.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(output, response.getWriter());
    }

    private String canonicalUriFor(String pid) {
        return NITRO_URI_PREFIX + pid;
    }

    private ResolutionApiOutput outputFor(String inputType, String inputValue, String outputType, Optional<String> outputValue) {
        if (!outputValue.isPresent()) {
            return emptyOutput();
        }
        
        ResolutionApiOutput.Resolution.Builder builder = ResolutionApiOutput.Resolution.builder();

        ResolutionApiOutput.Resolution resolution = builder.withInputId(inputValue)
                .withInputType(inputType)
                .withResolvedAs(outputValue.get())
                .withResolvedType(outputType)
                .build();

        return ResolutionApiOutput.from(resolution);
    }

    private ResolutionApiOutput emptyOutput() {
        return ResolutionApiOutput.empty();
    }

}
