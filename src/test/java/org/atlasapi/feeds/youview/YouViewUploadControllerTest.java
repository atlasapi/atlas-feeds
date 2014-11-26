package org.atlasapi.feeds.youview;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.youview.upload.YouViewClient;
import org.atlasapi.feeds.youview.www.YouViewUploadController;
import org.atlasapi.media.entity.Content;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.http.HttpException;


public class YouViewUploadControllerTest {
    
    private static final String VALID_CONTENT_URI = "valid_content";
    private static final String INVALID_CONTENT_URI = "invalid_content";
    private static final String VALID_PUBLISHER_STR = "bbc_nitro";
    private static final Content VALID_CONTENT = Mockito.mock(Content.class);
    
    private ContentResolver contentResolver = Mockito.mock(ContentResolver.class);
    private YouViewClient remoteClient = Mockito.mock(YouViewClient.class);
    private HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
    
    private final YouViewUploadController controller = new YouViewUploadController(contentResolver, remoteClient);
    
    @Before
    public void setup() throws IOException {
        when(contentResolver.findByCanonicalUris(ImmutableList.of(VALID_CONTENT_URI))).thenReturn(validResolvedContent());
        when(contentResolver.findByCanonicalUris(ImmutableList.of(INVALID_CONTENT_URI))).thenReturn(invalidResolvedContent());
        when(response.getOutputStream()).thenReturn(Mockito.mock(ServletOutputStream.class));
    }

    private ResolvedContent validResolvedContent() {
        return ResolvedContent.builder()
                .put(VALID_CONTENT_URI, VALID_CONTENT)
                .build();
    }

    private ResolvedContent invalidResolvedContent() {
        return ResolvedContent.builder()
                .build();
    }

    @Test
    public void testUploadRequestTriggersUploadIfContentFound() throws IOException, HttpException {
        controller.uploadContent(response, VALID_PUBLISHER_STR, VALID_CONTENT_URI);
        
        verify(remoteClient).upload(VALID_CONTENT);
        verify(response).setStatus(SC_OK);
    }

    @Test
    public void testUploadRequestReturnsNotFoundIfPublisherNotValid() throws IOException, HttpException {
        controller.uploadContent(response, "invalid_publisher", VALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testUploadRequestReturnsBadRequestIfUriParamNotPresent() throws IOException, HttpException {
        controller.uploadContent(response, VALID_PUBLISHER_STR, null);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testUploadRequestReturnsBadRequestIfContentNotFound() throws IOException, HttpException {
        controller.uploadContent(response, VALID_PUBLISHER_STR, INVALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testDeleteRequestTriggersUploadIfContentFound() throws IOException, HttpException {
        controller.deleteContent(response, VALID_PUBLISHER_STR, VALID_CONTENT_URI);
        
        verify(remoteClient).sendDeleteFor(VALID_CONTENT);
        verify(response).setStatus(SC_OK);
    }

    @Test
    public void testDeleteRequestReturnsNotFoundIfPublisherNotValid() throws IOException, HttpException {
        controller.deleteContent(response, "invalid_publisher", VALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testDeleteRequestReturnsBadRequestIfUriParamNotPresent() throws IOException, HttpException {
        controller.deleteContent(response, VALID_PUBLISHER_STR, null);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testDeleteRequestReturnsBadRequestIfContentNotFound() throws IOException, HttpException {
        controller.deleteContent(response, VALID_PUBLISHER_STR, INVALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testRevokeRequestTriggersUploadIfContentFound() throws IOException, HttpException {
        controller.revokeContent(response, VALID_PUBLISHER_STR, VALID_CONTENT_URI);
        
        verify(remoteClient).revoke(VALID_CONTENT);
        verify(response).setStatus(SC_OK);
    }

    @Test
    public void testRevokeRequestReturnsNotFoundIfPublisherNotValid() throws IOException, HttpException {
        controller.revokeContent(response, "invalid_publisher", VALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testRevokeRequestReturnsBadRequestIfUriParamNotPresent() throws IOException, HttpException {
        controller.revokeContent(response, VALID_PUBLISHER_STR, null);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testRevokeRequestReturnsBadRequestIfContentNotFound() throws IOException, HttpException {
        controller.revokeContent(response, VALID_PUBLISHER_STR, INVALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testUnrevokeRequestTriggersUploadIfContentFound() throws IOException, HttpException {
        controller.unrevokeContent(response, VALID_PUBLISHER_STR, VALID_CONTENT_URI);
        
        verify(remoteClient).unrevoke(VALID_CONTENT);
        verify(response).setStatus(SC_OK);
    }

    @Test
    public void testUnrevokeRequestReturnsNotFoundIfPublisherNotValid() throws IOException, HttpException {
        controller.unrevokeContent(response, "invalid_publisher", VALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
    }

    @Test
    public void testUnrevokeRequestReturnsBadRequestIfUriParamNotPresent() throws IOException, HttpException {
        controller.unrevokeContent(response, VALID_PUBLISHER_STR, null);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }

    @Test
    public void testUnrevokeRequestReturnsBadRequestIfContentNotFound() throws IOException, HttpException {
        controller.unrevokeContent(response, VALID_PUBLISHER_STR, INVALID_CONTENT_URI);
    
        verifyZeroInteractions(remoteClient);
        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), anyString());
    }
}
