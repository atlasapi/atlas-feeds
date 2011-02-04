package org.atlasapi.feeds.radioplayer.upload;

import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.failedUpload;
import static org.atlasapi.feeds.radioplayer.upload.DefaultFTPUploadResult.successfulUpload;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.joda.time.Duration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class RadioPlayerFtpAwareExecutor {

    private static final int MAX_CONNECTIONS = 10;
	private static final Duration RECONNECT_DELAY = Duration.standardSeconds(5);
	private static final int RECONNECT_ATTEMPTS = 5;
    
	private final FTPCredentials credentials;
    private final RadioPlayerFTPUploadResultRecorder recorder;
    private final AdapterLog log;
    
    // static to avoid multiple pools
	private static ExecutorService executor = Executors.newFixedThreadPool(MAX_CONNECTIONS, new ThreadFactoryBuilder().setNameFormat("RadioPlayerUploader: %s").build());
    
    public RadioPlayerFtpAwareExecutor(FTPCredentials credentials, RadioPlayerFTPUploadResultRecorder recorder, AdapterLog log) {
        this.credentials = credentials;
        this.recorder = recorder;
        this.log = log;
    }
    
    interface CallableWithClient<T> {

		Callable<T> create(FTPClient createClient);
    	
    }
    
	public <T> ExecutorCompletionService<T> submit(final Iterable<CallableWithClient<T>> callables) {
		ExecutorCompletionService<T> completionService = new ExecutorCompletionService<T>(executor);

		for (final CallableWithClient<T> callable : callables) {
			completionService.submit(new Callable<T>() {
	
				@Override
				public T call() throws Exception {
					FTPClient client = createClient();
					try {
						return callable.create(client).call();
					} finally {
						try {
							client.disconnect();
						} catch (IOException e) {
							// ignore IOE during disconnect
						}
					}
				}
			});
		}
		return completionService;
	}
    
    private FTPClient createClient() {
        FTPClient client = tryToConnectAndLogin();
        
        for (int i = 0; i < RECONNECT_ATTEMPTS && client == null; i++) {
        	try {
				Thread.sleep(RECONNECT_DELAY.getMillis());
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
        	client = tryToConnectAndLogin();
        }
        	
        FTPUploadResult result;
        if(client == null) {
            result = failedUpload(credentials.toString()).withMessage("Failed to connect/login to server");
        } else {
            result = successfulUpload(credentials.toString()).withMessage("Connected and logged-in successully");
        }
        recorder.record(result);
        
        return client;
    }

    private FTPClient tryToConnectAndLogin() {
        try {
            FTPClient client = new FTPClient();
            
            client.connect(credentials.server(), credentials.port());
            
            if(!FTPReply.isPositiveCompletion(client.getReplyCode())) {
                client.disconnect();
                return null;
            }
            
            client.enterLocalPassiveMode();
            
            if (!client.login(credentials.username(), credentials.password())) {
                client.disconnect();
                return null;
            }
            
            return client;
        } catch (Exception e) {
            log.record(new AdapterLogEntry(WARN).withCause(e).withSource(getClass()).withDescription("RadioPlayerUploader failed to connect client"));
            return null;
        }
    }
}
