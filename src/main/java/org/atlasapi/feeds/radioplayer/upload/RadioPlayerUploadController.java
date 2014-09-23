package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.metabroadcast.common.http.HttpStatusCode.BAD_REQUEST;
import static com.metabroadcast.common.http.HttpStatusCode.NOT_FOUND;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadManager;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadService;
import org.atlasapi.feeds.radioplayer.upload.queue.UploadTask;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.security.HttpBasicAuthChecker;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

@Controller
public class RadioPlayerUploadController {

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormat.forPattern("yyyyMMdd");
    private static final String HEALTH_PAGE_URL = "redirect:/feeds/ukradioplayer/health/%s/services/%d";
    private HttpBasicAuthChecker checker;
    private final DayRangeGenerator dayRangeGenerator;
    private final UploadManager stateUpdater;

    public RadioPlayerUploadController(DayRangeGenerator dayRangeGenerator, String password, UploadManager stateUpdater) {
        this.dayRangeGenerator = checkNotNull(dayRangeGenerator);
        if (!Strings.isNullOrEmpty(password)) {
            this.checker = new HttpBasicAuthChecker(ImmutableList.of(new UsernameAndPassword("bbc", password)));
        } else {
            this.checker = null;
        }
        this.stateUpdater = checkNotNull(stateUpdater);
    }

    @RequestMapping(value = "feeds/ukradioplayer/upload/{uploadService}/{type}/{id}/{day}", method = RequestMethod.POST)
    public String uploadDay(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("uploadService") String uploadServiceName,
            @PathVariable("type") String type, 
            @PathVariable("id") String serviceId, 
            @PathVariable("day") String day) throws IOException {
        if (checker == null) {
            response.setContentType(MimeType.TEXT_PLAIN.toString());
            response.getOutputStream().print("No password set up, uploader can't be used");
            return null;
        }
        
        UploadService uploadService = UploadService.fromString(uploadServiceName);
        if (uploadService == null) {
            response.sendError(NOT_FOUND.code(), "Unknown upload service " + uploadServiceName);
            return null;
        }
        
        RadioPlayerService service = RadioPlayerServices.all.get(serviceId);
        if (service == null) {
            response.sendError(NOT_FOUND.code(), "Unknown service " + serviceId);
            return null;
        }
        
        FileType fileType = FileType.fromString(type);
        if (fileType == null) {
            response.sendError(NOT_FOUND.code(), "Unknown file type " + type);
            return null;
        }
        
        if (fileType.equals(OD) && day == null) {
            response.sendError(BAD_REQUEST.code(), "Must specify day with OD file type");
        }

        if (day != null && !day.matches("\\d{8}")) {
            response.sendError(BAD_REQUEST.code(), "Bad Date Format");
            return null;
        }
        
        if (fileType.equals(PI)) {
            Iterable<LocalDate> days = day != null ? ImmutableList.of(DATE_PATTERN.parseDateTime(day).toLocalDate()) : dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
            for (LocalDate aDay : days) {
                UploadTask task = new UploadTask(new RadioPlayerFile(uploadService, service, fileType, aDay));
                stateUpdater.enqueueUploadTask(task);
            }
        } else if (fileType.equals(OD)) {
            UploadTask task = new UploadTask(new RadioPlayerFile(uploadService, service, fileType, DATE_PATTERN.parseDateTime(day).toLocalDate()));
            stateUpdater.enqueueUploadTask(task);
        }

        return String.format(HEALTH_PAGE_URL, uploadService.name().toLowerCase(), service.getRadioplayerId());
    }

    @RequestMapping("feeds/ukradioplayer/upload/{uploadService}/{type}/{id}")
    public String uploadDays(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("uploadService") String uploadService, 
            @PathVariable("type") String type, 
            @PathVariable("id") String serviceId) throws IOException {
        return uploadDay(request, response, uploadService, type, serviceId, null);
    }
}
