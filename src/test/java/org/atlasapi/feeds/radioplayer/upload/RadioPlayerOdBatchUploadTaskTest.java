package org.atlasapi.feeds.radioplayer.upload;

import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.atlasapi.persistence.logging.AdapterLog;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RadioPlayerOdBatchUploadTaskTest {

    private static final Publisher PUBLISHER = Publisher.BBC_NITRO;
    private static final RadioPlayerService SERVICE = RadioPlayerServices.all.get("300");

    @Mock private RadioPlayerRecordingExecutor executor;
    @Mock private AdapterLog adapterLog;
    @Mock private LastUpdatedContentFinder contentFinder;
    @Mock private ContentLister contentLister;
    @Mock private RadioPlayerUploadResultStore resultStore;

    private RadioPlayerOdBatchUploadTask task;

    @Before
    public void setUp() throws Exception {
        task = new RadioPlayerOdBatchUploadTask(
                ImmutableList.of(),
                executor,
                ImmutableList.of(SERVICE),
                LocalDate.now(),
                false,
                adapterLog,
                contentFinder,
                contentLister,
                PUBLISHER,
                resultStore
        );
    }

    @Test
    public void savesNoopResultIfNoUrisFoundForService() throws Exception {
        when(contentFinder.updatedSince(
                eq(PUBLISHER),
                any(DateTime.class)
        )).thenReturn(ImmutableList.<Content>of().iterator());

        task.run();

        ArgumentCaptor<RadioPlayerUploadResult> captor = ArgumentCaptor.forClass(
                RadioPlayerUploadResult.class
        );
        verify(resultStore).record(captor.capture());

        RadioPlayerUploadResult result = captor.getValue();
        assertThat(result.getService(), is(SERVICE));

        assertThat(result.getType(), is(FileType.OD));

        FileUploadResult upload = result.getUpload();
        assertThat(upload.type(), is(FileUploadResult.FileUploadResultType.NO_OP));
    }
}