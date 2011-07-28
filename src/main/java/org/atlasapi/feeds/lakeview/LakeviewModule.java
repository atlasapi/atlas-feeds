package org.atlasapi.feeds.lakeview;

import org.atlasapi.feeds.xml.XMLValidator;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

@Configuration
public class LakeviewModule {

    @Autowired ContentLister contentLister;
    @Autowired ContentResolver contentResolver;
    @Autowired AdapterLog log;
    
    public @Bean LakeviewController lakeviewController() {
        return new LakeviewController(lakeviewContentFetcher(), lakeviewFeedCompiler(), lakeviewFeedOutputter());
    }

    public @Bean LakeviewContentFetcher lakeviewContentFetcher() {
        return new LakeviewContentFetcher(contentLister, contentResolver);
    }
    
    public @Bean LakeviewFeedCompiler lakeviewFeedCompiler() {
        return new LakeviewFeedCompiler();
    }

    public @Bean XmlFeedOutputter lakeviewFeedOutputter() {
        return new ValidatingXmlFeedOutputter(lakeViewValidator(), new SerializingFeedOutputter());
    }

    public @Bean XMLValidator lakeViewValidator() {
        try {
            return XMLValidator.forSchemas(ImmutableSet.of(
                    Resources.getResource("xml.xsd").openStream(), 
                    Resources.getResource("Lakeview_Content_Catalog_Feed.xsd").openStream()
                ));
        } catch (Exception e) {
            log.record(new AdapterLogEntry(Severity.WARN).withDescription("Couldn't load schemas for Lakeview XML validation").withCause(e));
            return null;
        }
    }
}
