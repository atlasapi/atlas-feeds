package org.atlasapi.feeds.radioplayer.upload;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.reporting.telescope.FeedsReporterNames;

import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.security.HttpBasicAuthChecker;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.http.HttpStatus;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.metabroadcast.common.http.HttpStatusCode.BAD_REQUEST;
import static com.metabroadcast.common.http.HttpStatusCode.NOT_FOUND;
import static org.atlasapi.feeds.radioplayer.upload.FileType.OD;
import static org.atlasapi.feeds.radioplayer.upload.FileType.PI;

@Controller
public class RadioPlayerUploadController {

    private static final DateTimeFormatter DATE_PATTERN = DateTimeFormat.forPattern("yyyyMMdd");
    private HttpBasicAuthChecker checker;
    private final Map<String, RadioPlayerUploadTaskBuilder> radioPlayerUploadTaskMap;
    private final DayRangeGenerator dayRangeGenerator;
    private final ExecutorService executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("radio-player-manual-upload").build());

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
    public void uploadDay(HttpServletRequest request, HttpServletResponse response,
            @PathVariable("uploadService") String uploadService,
            @PathVariable("type") String type, 
            @PathVariable("id") String serviceId, 
            @PathVariable("day") String day) throws IOException {
        if (checker == null) {
            response.setContentType(MimeType.TEXT_PLAIN.toString());
            response.getOutputStream().print("No password set up, uploader can't be used");
            return;
        }
        
        RadioPlayerUploadTaskBuilder taskBuilder = radioPlayerUploadTaskMap.get(uploadService);
        if (taskBuilder == null) {
            response.sendError(NOT_FOUND.code(), "Unknown upload service " + uploadService);
            return;
        }
        
        RadioPlayerService service = RadioPlayerServices.all.get(serviceId);
        if (service == null) {
            response.sendError(NOT_FOUND.code(), "Unknown service " + serviceId);
            return;
        }
        
        FileType fileType = null;
        for (FileType typeOption : FileType.values()) {
            if (typeOption.name().equals(type)) {
                fileType = typeOption;
            }
        }
        if (fileType == null) {
            response.sendError(NOT_FOUND.code(), "Unknown file type " + type);
            return;
        }
        
        if (fileType.equals(OD) && day == null) {
            response.sendError(BAD_REQUEST.code(), "Must specify day with OD file type");
        }

        if (day != null && !day.matches("\\d{8}")) {
            response.sendError(BAD_REQUEST.code(), "Bad Date Format");
            return;
        }

        if (fileType.equals(PI)) {
            Iterable<LocalDate> days = day != null
                                       ? ImmutableList.of(DATE_PATTERN.parseDateTime(day)
                    .toLocalDate())
                                       : dayRangeGenerator.generate(new LocalDate(DateTimeZones.UTC));
            executor.submit(taskBuilder.newBatchPiTask(
                    ImmutableList.of(service),
                    days,
                    FeedsReporterNames.RADIO_PLAYER_MANUAL_PI_UPLOADER
            ));
        } else if (fileType.equals(OD)) {
            executor.submit(
                    taskBuilder.newBatchOdTask(
                            ImmutableList.of(service),
                            DATE_PATTERN.parseDateTime(day).toLocalDate(),
                            FeedsReporterNames.RADIO_PLAYER_MANUAL_OD_UPLOADER
                    ));
        }

        response.setStatus(HttpStatus.SC_ACCEPTED);
        return;
    }

    @RequestMapping("feeds/ukradioplayer/upload/{uploadService}/{type}/{id}")
    public void uploadDays(HttpServletRequest request, HttpServletResponse response, 
            @PathVariable("uploadService") String uploadService, 
            @PathVariable("type") String type, 
            @PathVariable("id") String serviceId) throws IOException {
        uploadDay(request, response, uploadService, type, serviceId, null);
    }
}
