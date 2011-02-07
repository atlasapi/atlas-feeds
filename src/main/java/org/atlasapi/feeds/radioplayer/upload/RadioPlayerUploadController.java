package org.atlasapi.feeds.radioplayer.upload;

import static com.metabroadcast.common.http.HttpStatusCode.BAD_REQUEST;
import static com.metabroadcast.common.http.HttpStatusCode.NOT_FOUND;
import static com.metabroadcast.common.http.HttpStatusCode.SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.util.concurrent.Executors;
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

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;

@Controller
public class RadioPlayerUploadController {
    
    private final DayRangeGenerator rangedGenerator;
    private final RadioPlayerXMLValidator validator;
    private final AdapterLog log;

    private final DayRangeGenerator todayGenerator;
    private final ScheduledExecutorService uploadExecutor;
    
    private RadioPlayerFtpAwareExecutor radioPlayerUploadTaskRunner;

    public RadioPlayerUploadController(DayRangeGenerator rangedGenerator, RadioPlayerXMLValidator validator, AdapterLog log) {
        this.rangedGenerator = rangedGenerator;
        this.validator = validator;
        this.log = log;
        this.todayGenerator = new DayRangeGenerator();
        this.uploadExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    @RequestMapping("feeds/ukradioplayer/upload/{id}/{day}")
    public void upload(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String  serviceId, @PathVariable("day") String day) throws IOException {
        
        if(radioPlayerUploadTaskRunner == null) {
            response.sendError(SERVICE_UNAVAILABLE.code(),"Upload service not configured");
            return;
        }
        
        RadioPlayerService service = RadioPlayerServices.all.get(serviceId);
        if(service == null) {
            response.sendError(NOT_FOUND.code(),"Unkown service " + serviceId);
            return;
        }
        
        if(day != null && !day.matches("\\d{8}")) {
            response.sendError(BAD_REQUEST.code(), "Bad Date Format");
            return;
        }
        
        DayRange days = day != null ? todayGenerator.generate(DateTimeFormat.forPattern("yyyyMMdd").parseDateTime(day).toLocalDate()) : rangedGenerator.generate(new LocalDate(DateTimeZones.UTC));
        
        uploadExecutor.submit(new RadioPlayerUploadTask(radioPlayerUploadTaskRunner, ImmutableList.of(service), days).withLog(log).withValidator(validator));
        
        response.setStatus(HttpStatusCode.OK.code());
        response.setContentLength(0);
    }

    public RadioPlayerUploadController withUploadExecutor(RadioPlayerFtpAwareExecutor radioPlayerUploadTaskRunner) {
        this.radioPlayerUploadTaskRunner = radioPlayerUploadTaskRunner;
        return this;
    }

}
