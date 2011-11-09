package org.atlasapi.feeds.upload.ftp;

import java.io.IOException;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.time.DateTimeZones;

public class CommonsDirectoryLister {
    
    private RemoteServiceDetails remoteDetails;
    private CommonsFTPClientConnector clientConnector;

    public CommonsDirectoryLister(RemoteServiceDetails remoteDetails) {
        this.remoteDetails = remoteDetails;
        this.clientConnector = new CommonsFTPClientConnector();
    }

    public List<FileLastModified> listDir(String dir) {
        ImmutableList.Builder<FileLastModified> list = ImmutableList.builder();
        
        FTPClient client;
        try {
            client = clientConnector.connectAndLogin(remoteDetails);
        } catch (Exception e) {
            client = null;
        }
        
        if (client != null && client.isConnected()) {
            try {
                FTPFile[] files = client.listFiles(dir, ftpFilenameFilter);
                
                for (FTPFile file: files) {
                    list.add(new FileLastModified(file.getName(), new DateTime(file.getTimestamp(), DateTimeZones.UTC)));
                }
                
                return list.build();
            } catch (IOException e) {
                //TODO: remove
                e.printStackTrace();
            } finally {
                clientConnector.disconnectQuietly(client);
            }
        }
        
        return list.build();
    }
    
    private static final FTPFileFilter ftpFilenameFilter = new FTPFileFilter() {
        @Override
        public boolean accept(FTPFile file) {
            return file.isFile() && file.getName().endsWith(".xml") && file.getName().startsWith("20") && ! file.getName().endsWith("SI.xml");
        }
    };

    public static class FileLastModified {
        
        private final String fileName;
        private final DateTime lastModified;

        public FileLastModified(String fileName, DateTime lastModified) {
            this.fileName = fileName;
            this.lastModified = lastModified;
        }
        
        public String fileName() {
            return fileName;
        }
        
        public DateTime lastModified() {
            return lastModified;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FileLastModified) {
                FileLastModified target = (FileLastModified) obj;
                return Objects.equal(fileName, target.fileName) && Objects.equal(lastModified, target.lastModified);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            return fileName.hashCode();
        }
        
        @Override
        public String toString() {
            return Objects.toStringHelper(this).addValue(fileName).addValue(lastModified).toString();
        }
    }
    
}
