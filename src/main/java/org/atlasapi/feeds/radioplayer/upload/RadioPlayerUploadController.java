package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.http.HttpStatusCode.BAD_REQUEST;
import static com.metabroadcast.common.http.HttpStatusCode.NOT_FOUND;
import static com.metabroadcast.common.http.HttpStatusCode.SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.persistence.logging.AdapterLog;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

@Controller
public class RadioPlayerUploadController {

    private final FTPFileUploader uploader;
    private final DayRangeGenerator rangedGenerator;
    private final RadioPlayerXMLValidator validator;
    private final AdapterLog log;
    private final ScheduledExecutorService uploadExecutor;

    private RadioPlayerRecordingExecutor radioPlayerUploadTaskRunner;

    public RadioPlayerUploadController(FTPFileUploader uploader, DayRangeGenerator rangedGenerator, RadioPlayerXMLValidator validator, AdapterLog log) {
        this.uploader = uploader;
        this.rangedGenerator = rangedGenerator;
        this.validator = validator;
        this.log = log;
        this.uploadExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @RequestMapping(value = "feeds/ukradioplayer/upload/{id}/{day}", method = RequestMethod.POST)
    public String uploadDay(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String serviceId, @PathVariable("day") String day) throws IOException {

        if (radioPlayerUploadTaskRunner == null) {
            response.sendError(SERVICE_UNAVAILABLE.code(), "Upload service not configured");
            return null;
        }

        RadioPlayerService service = RadioPlayerServices.all.get(serviceId);
        if (service == null) {
            response.sendError(NOT_FOUND.code(), "Unkown service " + serviceId);
            return null;
        }

        if (day != null && !day.matches("\\d{8}")) {
            response.sendError(BAD_REQUEST.code(), "Bad Date Format");
            return null;
        }

        Iterable<LocalDate> days = day != null ? ImmutableList.of(DateTimeFormat.forPattern("yyyyMMdd").parseDateTime(day).toLocalDate()) : rangedGenerator.generate(new LocalDate(DateTimeZones.UTC));

        Future<?> result = uploadExecutor.submit(new RadioPlayerUploadTask(uploader, radioPlayerUploadTaskRunner, ImmutableList.of(service), days).withLog(log).withValidator(validator));

        try {
            result.get();
        } catch (InterruptedException e) {
            
        } catch (ExecutionException e) {
            
        }
        
        return "redirect:/feeds/ukradioplayer/health";
    }

    @RequestMapping("feeds/ukradioplayer/upload/{id}")
    public String uploadDays(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String serviceId) throws IOException {
        return uploadDay(request, response, serviceId, null);
    }

    public RadioPlayerUploadController withUploadExecutor(RadioPlayerRecordingExecutor radioPlayerUploadTaskRunner) {
        this.radioPlayerUploadTaskRunner = radioPlayerUploadTaskRunner;
        return this;
    }

}
