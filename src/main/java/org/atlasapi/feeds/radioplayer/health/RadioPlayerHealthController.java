package org.atlasapi.feeds.radioplayer.health;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.FileHistory;
import org.atlasapi.feeds.radioplayer.upload.FileType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFile;
import org.atlasapi.feeds.radioplayer.upload.persistence.FileHistoryStore;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadAttempt;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.security.HttpBasicAuthChecker;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.webapp.health.HealthController;

@Controller
public class RadioPlayerHealthController {

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormat.forPattern("yyyyMMdd");
    
    private final HealthController main;
    private final Map<String, Iterable<String>> slugs;
    private final HttpBasicAuthChecker checker;
    private final Iterable<UploadService> uploadServices;
    private final FileHistoryStore fileStore;

    public RadioPlayerHealthController(HealthController main, Iterable<UploadService> uploadServices, String password, FileHistoryStore fileStore) {
        if (!Strings.isNullOrEmpty(password)) {
            this.checker = new HttpBasicAuthChecker(ImmutableList.of(new UsernameAndPassword("bbc", password)));
        } else {
            this.checker = null;
        }
        this.main = checkNotNull(main);
        this.uploadServices = checkNotNull(uploadServices);
        this.fileStore = checkNotNull(fileStore);
        slugs = createSlugMap();
    }

    private Map<String, Iterable<String>> createSlugMap() {
        Builder<String, Iterable<String>> slugMap = ImmutableMap.builder();
        for (UploadService uploadService : uploadServices) {
            slugMap.put(uploadService.name().toLowerCase(), slugsFor(uploadService));
            for (RadioPlayerService service : RadioPlayerServices.services) {
                slugMap.put(createSlugKey(uploadService, String.valueOf(service.getRadioplayerId())), slugsFor(uploadService, service));
            }
        }
        return slugMap.build();
    }

    private Iterable<String> slugsFor(final UploadService uploadService) {
        return Iterables.concat(
                ImmutableList.of(
                    "ukrp-connect-" + uploadService.name().toLowerCase()
                ),
                Iterables.transform(RadioPlayerServices.services, new Function<RadioPlayerService, String>() {
                    @Override
                    public String apply(RadioPlayerService service) {
                        return String.format("ukrp-summary-%s-%s", uploadService.name().toLowerCase(), service.getName());
                    }
                }
        ));
    }
    
    private Iterable<String> slugsFor(UploadService uploadService, RadioPlayerService service) {
        return ImmutableList.of(
                "ukrp-connect-" + uploadService.name().toLowerCase(),
                String.format(String.format("ukrp-%s-%s", uploadService.name().toLowerCase(), service.getName()))
        );
    }

    @RequestMapping("feeds/ukradioplayer/health/{uploadService}")
    public String radioPlayerHealth(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("uploadService") String uploadService) throws IOException {
        if (checker == null) {
            response.setContentType(MimeType.TEXT_PLAIN.toString());
            response.getOutputStream().print("No password set up, health page cannot be viewed");
            return null;
        }
        boolean allowed = checker.check(request);
        if (allowed) {
            if(slugs.containsKey(uploadService)) {
                
                return main.showHealthPageForSlugs(response, slugs.get(uploadService), false);
            } else {
                response.sendError(HttpStatusCode.NOT_FOUND.code());
            }
        }
        HttpBasicAuthChecker.requestAuth(response, "Heath Page");
        return null;
    }

    @RequestMapping("feeds/ukradioplayer/health/{uploadService}/services/{service}")
    public String radioPlayerServiceHealth(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("uploadService") String uploadService, @PathVariable("service") String rpServiceId) throws IOException {
        if (checker == null) {
            response.setContentType(MimeType.TEXT_PLAIN.toString());
            response.getOutputStream().print("No password set up, health page cannot be viewed");
            return null;
        }
        boolean allowed = checker.check(request);
        if (allowed) {
            String serviceKey = createSlugKey(UploadService.fromString(uploadService), rpServiceId);
            if(slugs.containsKey(serviceKey)) {
                return main.showHealthPageForSlugs(response, slugs.get(serviceKey), false);
            } else {
                response.sendError(HttpStatusCode.NOT_FOUND.code());
            }
        }
        HttpBasicAuthChecker.requestAuth(response, "Heath Page");
        return null;
    }
    
    @RequestMapping("feeds/ukradioplayer/health/{uploadService}/services/{service}/files/{type}/{date}")
    public String radioPlayerFileHealth(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("uploadService") String uploadServiceStr, @PathVariable("service") String rpServiceId,
            @PathVariable("type") String typeStr, @PathVariable("date") String dateStr) throws IOException {
        if (checker == null) {
            response.setContentType(MimeType.TEXT_PLAIN.toString());
            response.getOutputStream().print("No password set up, health page cannot be viewed");
            return null;
        }
        boolean allowed = checker.check(request);
        if (allowed) {
            RadioPlayerFile file = new RadioPlayerFile(
                    UploadService.fromString(uploadServiceStr), 
                    RadioPlayerServices.all.get(rpServiceId), 
                    FileType.fromString(typeStr), 
                    DATE_PATTERN.parseLocalDate(dateStr)
            );
            Optional<FileHistory> fetched = fileStore.fetch(file);
            
            if(fetched.isPresent()) {
                FileHistory fileHistory = fetched.get();
                boolean success = getLastResultStatus(fileHistory);
                
                if (request.getRequestURI().endsWith(".json")) {
                    FileHistoryOutputter.printJsonResponse(response, fileHistory);
                } else {
                    FileHistoryOutputter.printHtmlResponse(response, fileHistory, success);
                }
            } else {
                response.sendError(HttpStatusCode.NOT_FOUND.code());
            }
        }
        HttpBasicAuthChecker.requestAuth(response, "Heath Page");
        return null;
    }

    private boolean getLastResultStatus(FileHistory fileHistory) {
        if (fileHistory.uploadAttempts().isEmpty()) {
            return false;
        }
        UploadAttempt latest = fileHistory.getLatestUpload();
        if (latest == null || FileUploadResultType.FAILURE.equals(latest.uploadResult())) {
            return false;
        }
        return false;
    }

    private String createSlugKey(UploadService uploadService, String rpServiceId) {
        return uploadService.name() + ":" + rpServiceId;
    }
}
