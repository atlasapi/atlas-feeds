package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploaderTest.TestUser;
import org.atlasapi.feeds.radioplayer.upload.RadioPlayerFileUploaderTest.TestUserManager;
import org.atlasapi.feeds.upload.FileUploader;
import org.atlasapi.feeds.upload.FileUpload;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.ftp.CommonsFTPFileUploader;
import org.atlasapi.feeds.upload.ftp.FTPCredentials;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;
import com.google.common.io.Resources;

public class FTPFileUploadTest {

    private static final String TEST_PASSWORD = "testpassword";
    private static final String TEST_USERNAME = "test";
    private FtpServer server;
    private File dir;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testCall() throws Exception {
        
        dir = Files.createTempDir();
        dir.deleteOnExit();
        
        startServer();
        
        FileUploader upload = new CommonsFTPFileUploader(FTPCredentials.forServer("localhost").withPort(9521).withUsername(TEST_USERNAME).withPassword(TEST_USERNAME).build());
        
        FileUploadResult result = upload.upload(new FileUpload("success", Resources.toByteArray(Resources.getResource("org/atlasapi/feeds/radioplayer/basicPIFeedTest.xml"))));
        
        assertThat(result.type(), is(equalTo(FileUploadResultType.SUCCESS)));
        assertThat(dir.listFiles().length, is(1));

        server.stop();
    }

    private void startServer() throws FtpException {
        FtpServerFactory serverFactory = new FtpServerFactory();

        ListenerFactory factory = new ListenerFactory();

        factory.setPort(9521);

        serverFactory.addListener("default", factory.createListener());

        serverFactory.setUserManager(new TestUserManager(new TestUser(TEST_USERNAME, TEST_PASSWORD, dir)));
        
        server = serverFactory.createServer();

        server.start();
    }
}
