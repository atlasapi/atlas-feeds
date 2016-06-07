package org.atlasapi.resizer;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class HttpResizerClientTest {

    private final HttpTransport transport = new NetHttpTransport();

    @Test
    public void testGetsCorrectImageSize() throws Exception {
        ResizerClient resizerClient = new HttpResizerClient(transport);
        ImageSize imageSize = resizerClient.getImageDimensions(
                "http://users-images-atlas.metabroadcast.com/?source=http://ichef.bbci.co.uk/images/ic/1024x576/p028s846.png&profile=monocrop&resize=1024x576"
        );
        assertNotEquals(imageSize.getWidth(), null);
        assertNotEquals(imageSize.getHeight(), null);
    }
}