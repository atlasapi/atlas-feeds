package org.atlasapi.feeds;

import org.atlasapi.feeds.interlinking.InterlinkingModule;
import org.atlasapi.feeds.radioplayer.RadioPlayerModule;
import org.atlasapi.feeds.sitemaps.SiteMapModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InterlinkingModule.class, SiteMapModule.class, RadioPlayerModule.class})
public class AtlasFeedsModule {

}
