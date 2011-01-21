package org.atlasapi.feeds.radioplayer.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.List;
import java.util.Set;

import org.apache.commons.net.ftp.FTPClient;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedType;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerFileUploader implements Runnable {
	
	private final RadioPlayerFTPCredentials credentials;
	private final String ftpPath;
	
	private final AdapterLog log;
	private final KnownTypeQueryExecutor queryExecutor;
	
	private RadioPlayerXMLValidator validator = null;
	private Set<RadioPlayerService> services = RadioPlayerServices.services;
	private int lookAhead = 7;
    private int lookBack = 2;
    private RadioPlayerUploadResultRecorder recorder;

	public RadioPlayerFileUploader(RadioPlayerFTPCredentials credentials, String ftpPath, KnownTypeQueryExecutor queryExecutor, AdapterLog log) {
		this.credentials = credentials;
		this.ftpPath = ftpPath;
		this.queryExecutor = queryExecutor;
		this.log = log;
	}
	
	public RadioPlayerFileUploader withServices(Iterable<RadioPlayerService> services) {
		this.services = ImmutableSet.copyOf(services);
		return this;
	}
	
	public RadioPlayerFileUploader withValidator(RadioPlayerXMLValidator validator) {
		this.validator = validator;
		return this;
	}
	
	public RadioPlayerFileUploader withLookAhead(int lookAhead) {
		this.lookAhead  = lookAhead;
		return this;
	}
	
	public RadioPlayerFileUploader withLookBack(int lookBack) {
        this.lookBack = lookBack;
	    return this;
	}
	
	public RadioPlayerFileUploader withResultRecorder(RadioPlayerUploadResultRecorder recorder) {
	    this.recorder = recorder;
	    return this;
	}
	
	@Override
	public void run() {
		log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("RadioPlayerFileUploader started"));
		
		try {
			checkNotNull(Strings.emptyToNull(credentials.server()), "No Radioplayer FTP Host, set rp.ftp.host");
			checkNotNull(Strings.emptyToNull(credentials.username()), "No Radioplayer FTP Username, set rp.ftp.username");
			checkNotNull(Strings.emptyToNull(credentials.password()), "No Radioplayer FTP Password, set rp.ftp.password");
			
			checkNotNull(ftpPath, "No Radioplayer FTP Path, set rp.ftp.path");

			FTPClient client = connectAndLogin();
			FTPClient checkerClient = connectAndLogin();
            
            uploadFiles(client, checkerClient);
            
		} catch (Exception e) {
			log.record(new AdapterLogEntry(Severity.WARN).withCause(e).withDescription("Exception running RadioPlayerFileUploader"));
		}
	}

    private FTPClient connectAndLogin() throws SocketException, IOException {
        FTPClient client = new FTPClient();

        client.connect(credentials.server(), credentials.port());
        
        client.enterLocalPassiveMode();
        
        if (!client.login(credentials.username(), credentials.password())) {
            throw new RuntimeException("Unable to connect to " + credentials.server() + " with username: " + credentials.username() + " and password...");
        }
        
        if (!ftpPath.isEmpty() && !client.changeWorkingDirectory(ftpPath)) {
            throw new RuntimeException("Unable to change working directory to " + ftpPath);
        }
        return client;
    }

    private void uploadFiles(FTPClient client, FTPClient checkerClient) {
//        RemoteCheckingRadioPlayerUploader uploader = new RemoteCheckingRadioPlayerUploader(checkerClient, new LoggingRadioPlayerUploader(log, new ValidatingRadioPlayerUploader(validator, new BasicRadioPlayerUploader(client))));
        RadioPlayerUploader uploader = new ValidatingRadioPlayerUploader(validator, new BasicRadioPlayerUploader(client));
        int count = 0;
        DateTime day = new LocalDate().toInterval(DateTimeZones.UTC).getStart().minusDays(lookBack);
        
        List<RadioPlayerUploadResult> results = Lists.newArrayListWithCapacity((lookAhead+lookBack+1) * services.size());
        
        for (int i = 0; i < (lookAhead+lookBack+1); i++, day = day.plusDays(1)) {
        	for (RadioPlayerService service : services) {
        	    RadioPlayerFeedType type = RadioPlayerFeedType.PI;
        	    String filename = filenameFrom(day, service, type);
    			try {
    				ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    type.compileFeedFor(day, service, queryExecutor, baos);

                    results.add(uploader.upload(filename, baos.toByteArray()));

                    count++;
    					
    			} catch (Exception e) {
    				String desc = String.format("Exception creating %s feed for service %s for %s", type, service.getName(), day.toString("dd/MM/yyyy"));
    				log.record(new AdapterLogEntry(Severity.WARN).withSource(getClass()).withCause(e).withDescription(desc ));
    				results.add(DefaultRadioPlayerUploadResult.failedUpload(filename).withCause(e).withMessage(e.getMessage()));
    			} 
        	}
        }
        
        if (recorder != null) {
            recorder.record(results);
        }

        log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription("RadioPlayerFileUploader finished: "+count+" files uploaded"));
    }

	private String filenameFrom(DateTime today, RadioPlayerService service, RadioPlayerFeedType type) {
		String date = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZone.UTC).print(today);
		return String.format("%s_%s_%s.xml", date, service.getRadioplayerId(), type.toString());
	}

}
