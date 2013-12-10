package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.metabroadcast.common.http.HttpStatusCode.BAD_REQUEST;
import static com.metabroadcast.common.http.HttpStatusCode.NOT_FOUND;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.base.Preconditions;
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
    private HttpBasicAuthChecker checker;
    private final Map<String, RadioPlayerUploadTaskBuilder> radioPlayerUploadTaskMap;
    private final DayRangeGenerator dayRangeGenerator;

    public RadioPlayerUploadController(Map<String, RadioPlayerUploadTaskBuilder> radioPlayerUploadTaskMap, DayRangeGenerator dayRangeGenerator, String password) {
        this.radioPlayerUploadTaskMap = checkNotNull(radioPlayerUploadTaskMap);
        this.dayRangeGenerator = checkNotNull(dayRangeGenerator);
        if (!Strings.isNullOrEmpty(password)) {
            this.checker = new HttpBasicAuthChecker(ImmutableList.of(new UsernameAndPassword("bbc", password)));
        } else {
            this.checker = null;
        }
    }

    @RequestMapping(value = "feeds/ukradioplayer/upload/{uploadService}/{type}/{id}/{day}", method = RequestMethod.POST)
    public String uploadDay(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("uploadService") String uploadService,
            @PathVariable("type") String type, 
            @PathVariable("id") String serviceId, 
            @PathVariable("day") String day) throws IOException {
        if (checker == null) {
            response.setContentType(MimeType.TEXT_PLAIN.toString());
            response.getOutputStream().print("No password set up, uploader can't be used");
            return null;
        }
        
        RadioPlayerUploadTaskBuilder taskBuilder = radioPlayerUploadTaskMap.get(uploadService);
        if (taskBuilder == null) {
            response.sendError(NOT_FOUND.code(), "Unknown upload service " + uploadService);
            return null;
        }
        
        RadioPlayerService service = RadioPlayerServices.all.get(serviceId);
        if (service == null) {
            response.sendError(NOT_FOUND.code(), "Unknown service " + serviceId);
            return null;
        }
        
        FileType fileType = null;
        for (FileType typeOption : FileType.values()) {
            if (typeOption.name().equals(type)) {
                fileType = typeOption;
            }
        }
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
            taskBuilder.newBatchPiTask(ImmutableList.of(service), days).run();
        } else if (fileType.equals(OD)) {
            taskBuilder.newBatchOdTask(ImmutableList.of(service), DATE_PATTERN.parseDateTime(day).toLocalDate()).run();
        }

        return "redirect:/feeds/ukradioplayer/health/" + uploadService;
    }

    @RequestMapping("feeds/ukradioplayer/upload/{uploadService}/{type}/{id}")
    public String uploadDays(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("uploadService") String uploadService, 
            @PathVariable("type") String type, 
            @PathVariable("id") String serviceId) throws IOException {
        return uploadDay(request, response, uploadService, type, serviceId, null);
    }
}
