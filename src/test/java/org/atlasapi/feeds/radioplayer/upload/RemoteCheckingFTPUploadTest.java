package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.atlasapi.feeds.radioplayer.upload.FTPUploadResult.FTPUploadResultType;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploaderTest.TestUser;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploaderTest.TestUserManager;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.io.Files;

public class RemoteCheckingFTPUploadTest {

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
    
    FTPUploadResult successful = DefaultFTPUploadResult.successfulUpload("success");
    
    @Test
    public void testUpload() throws Exception {
        
        Mockery context = new Mockery();
        
        final FTPUpload delegate = context.mock(FTPUpload.class);
        
        context.checking(new Expectations(){{
            one(delegate).upload(with(any(FTPClient.class)),with(any(String.class)),with(any(byte[].class)));
                will(returnValue(successful));
        }});
            
        RemoteCheckingFTPFileUpload uploader = new RemoteCheckingFTPFileUpload(delegate);

        FTPUploadResult result = uploader.upload(client, "success", new byte[]{});

        assertThat(result.type(), is(equalTo(FTPUploadResultType.SUCCESS)));

    }

    @Test
    public void testFailedException() throws Exception {
        
        Mockery context = new Mockery();
        
        final FTPUpload delegate = context.mock(FTPUpload.class);
        
        context.checking(new Expectations(){{
            one(delegate).upload(with(any(FTPClient.class)),with(any(String.class)),with(any(byte[].class))); 
                will(returnValue(successful));
        }});
            
        RemoteCheckingFTPFileUpload uploader = new RemoteCheckingFTPFileUpload(delegate);

        FTPUploadResult result = uploader.upload(client, "failure",  new byte[]{});

        assertThat(result.type(), is(equalTo(FTPUploadResultType.FAILURE)));
        
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
