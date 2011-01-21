package org.atlasapi.feeds.radioplayer.upload;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.atlasapi.persistence.logging.AdapterLogEntry.ExceptionSummary;
import org.joda.time.DateTime;

public class RemoteCheckingRadioPlayerUploader implements RadioPlayerUploader {

    private final RadioPlayerUploader delegate;
    private final FTPClient client;
    private CompletionService<Boolean> checker;

    public RemoteCheckingRadioPlayerUploader(FTPClient checkerClient, RadioPlayerUploader delegate) {
        this.client = checkerClient;
        this.delegate = delegate;
        checker = new ExecutorCompletionService<Boolean>(Executors.newFixedThreadPool(5));
    }

    @Override
    public RadioPlayerUploadResult upload(String filename, byte[] fileData) {
        RadioPlayerUploadResult status = delegate.upload(filename, fileData);
        if(!status.wasSuccessful()) {
            return status;
        }
        Future<Boolean> success = checker.submit(new RemoteSuccessChecker(filename));
        return futureUploadStatus(filename, success, status.uploadTime());
    }

    private RadioPlayerUploadResult futureUploadStatus(final String filename, final Future<Boolean> success, final DateTime dateTime) {
        return new FutureRadioPlayerUploadResult(filename, dateTime, success);
    }

    private final class FutureRadioPlayerUploadResult extends DefaultRadioPlayerUploadResult {
        private final Future<Boolean> success;
        private Boolean checkSuccess = null;

        private FutureRadioPlayerUploadResult(String filename, DateTime dateTime, Future<Boolean> success) {
            super(filename, dateTime);
            this.success = success;
        }

        private Boolean getSuccess() {
            if (checkSuccess == null) {
                try {
                    checkSuccess = success.get(45, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                    this.thrown = new ExceptionSummary(e);
                    checkSuccess = false;
                }
            }
            return checkSuccess;
        }

        @Override
        public String filename() {
            return filename;
        }

        @Override
        public Boolean wasSuccessful() {
            return getSuccess();
        }

        @Override
        public String message() {
            Boolean success = getSuccess();
            if(success == null){
                return "Couldn't determine upload success on remote server";
            } else {
                return success ? "Uploaded successfully" : "Upload rejected on remote server";
            }
        }

        @Override
        public DateTime uploadTime() {
            return time;
        }

        @Override
        public ExceptionSummary exception() {
            return thrown;
        }
    }

    private final class RemoteSuccessChecker implements Callable<Boolean> {
        

        private String filename;

        public RemoteSuccessChecker(String filename) {
            this.filename = filename;
        }
        
        @Override
        public Boolean call() throws Exception {
            
            for (int i = 0; i < 5; i++) {
                if (checkForFile("Processed")) {
                    return true;
                }
                if (checkForFile("Failed")) {
                    return false;
                }
                Thread.sleep(5000); //TODO: will return null, sensible?
            }
            
            return null;
        }

        private Boolean checkForFile(String pathname) throws IOException {
            FTPFile[] listFiles = null;
            synchronized (client) {
                client.changeWorkingDirectory(pathname);
                listFiles = client.listFiles(filename);
                client.changeToParentDirectory();
            }
            if(listFiles != null && listFiles.length == 1 && filename.equals(listFiles[0].getName())) {
                return true;
            }
            return false;
        }
    }
    
}
