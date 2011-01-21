package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploaderTest.TestUser;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploaderTest.TestUserManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;

public class RemoteCheckingRadioPlayerUploadTaskTest {

    private static final int PORT = 9521;
    private static final String TEST_PASSWORD = "testpassword";
    private static final String TEST_USERNAME = "test";
    private static File dir;

    private static FtpServer server;
    private static FTPClient client;
    
    @BeforeClass
    public static void setup() throws Exception {
        
        dir = Files.createTempDir();
        System.out.println(dir.getAbsolutePath());
        dir.deleteOnExit();
        
        File success = new File(dir + File.separator + "Processed" + File.separator + "success");
        File otherSuccess = new File(dir + File.separator + "Processed" + File.separator + "othersuccess");
        Files.createParentDirs(success);
        success.createNewFile();
        otherSuccess.createNewFile();
        
        File failure = new File(dir + File.separator + "Failed" + File.separator + "failure");
        Files.createParentDirs(failure);
        failure.createNewFile();
        
        startServer();
        
        client = new FTPClient();
        client.connect("localhost",PORT);
        client.login(TEST_USERNAME, TEST_PASSWORD);
        client.enterLocalPassiveMode();
        
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        server.stop();
    }
    
    
    @Test
    public void testUpload() {
            
            RadioPlayerUploader uploader = new RemoteCheckingRadioPlayerUploader(client, new NullRadioPlayerUploadTask());

            RadioPlayerUploadResult result = uploader.upload("success", null);
            
            assertThat(result.wasSuccessful(), is(true));

    }

    @Test
    public void testFailedException() {
        
        RadioPlayerUploader uploader = new RemoteCheckingRadioPlayerUploader(client, new NullRadioPlayerUploadTask());

        RadioPlayerUploadResult result = uploader.upload("failure", null);
        
        assertThat(result.wasSuccessful(), is(false));
        
     }
    
    public class NullRadioPlayerUploadTask implements RadioPlayerUploader {
        @Override
        public RadioPlayerUploadResult upload(String filename, byte[] fileData) {
            return DefaultRadioPlayerUploadResult.successfulUpload(filename);
        }
    }

    private static void startServer() throws FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();

        ListenerFactory factory = new ListenerFactory();

        factory.setPort(9521);

        serverFactory.addListener("default", factory.createListener());

        serverFactory.setUserManager(new TestUserManager(new TestUser(TEST_USERNAME, TEST_PASSWORD, dir)));

        server = serverFactory.createServer();

        server.start();
    }

}
