package org.atlasapi.feeds.youview.www;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.atlasapi.feeds.youview.persistence.IdMappingStore;
import org.atlasapi.feeds.youview.resolutionapi.ResolutionApiOutput;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.metabroadcast.common.webapp.json.GsonFactory;

@Controller
public class NitroYouViewResolutionController {

    private static final String NITRO_URI_PREFIX = "http://nitro.bbc.co.uk/programmes/";
    private final IdMappingStore mappingStore;
    private static final Gson GSON = GsonFactory.defaultGson();

    public NitroYouViewResolutionController(IdMappingStore mappingStore) {
        this.mappingStore = checkNotNull(mappingStore);
    }

    @RequestMapping(value = "/feeds/nitro-youview/version.json", method = RequestMethod.GET)
    public void getYouViewVersionCridJson(HttpServletResponse response,
            @RequestParam(value = "pid", required = true) String pid)
            throws IOException {
        response.setContentType("application/json");
        serializeJSON(response, outputForPid(pid));
    }

    @RequestMapping(value = "/feeds/nitro-youview/version.xml", method = RequestMethod.GET)
    public void getYouViewVersionCridXml(HttpServletResponse response,
            @RequestParam(value = "pid", required = true) String pid)
            throws JAXBException, IOException {
        response.setContentType("application/xml");
        serializeXML(response, outputForPid(pid));
    }

    private ResolutionApiOutput outputForPid(String pid) {
        Optional<String> versionCrid = mappingStore.getValueFor(canonicalUriFor(pid));
        if (versionCrid.isPresent()) {
            return outputFor(pid, versionCrid.get());
        } else {
            return emptyOutput();
        }
    }

    private void serializeJSON(HttpServletResponse response,
            ResolutionApiOutput output) throws IOException {
        GSON.toJson(output.getBlocklist(), response.getWriter());
    }


    private void serializeXML(HttpServletResponse response, ResolutionApiOutput output)
            throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(ResolutionApiOutput.class);
        Marshaller jaxbMarshaller = context.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        jaxbMarshaller.marshal(output, response.getWriter());
    }

    private String canonicalUriFor(String pid) {
        return NITRO_URI_PREFIX + pid;
    }

    private ResolutionApiOutput outputFor(String versionPid, String versionCrid) {
        ResolutionApiOutput.Resolution.Builder builder = ResolutionApiOutput.Resolution.builder();

        ResolutionApiOutput.Resolution resolution = builder.withInputId(versionPid)
                .withInputType("version_pid")
                .withResolvedAs(versionCrid)
                .withResolvedType("youview_crid")
                .build();

        return ResolutionApiOutput.from(resolution);
    }

    private ResolutionApiOutput emptyOutput() {
        return ResolutionApiOutput.empty();
    }

}
