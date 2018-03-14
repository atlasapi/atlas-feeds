package org.atlasapi.feeds.youview;

import java.util.Map;

import org.atlasapi.feeds.youview.hierarchy.ContentHierarchyExpander;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import( { NitroTVAnytimeModule.class, AmazonTVAnytimeModule.class } )
public class ContentHierarchyExpanderFactory {

    private static final Logger log = LoggerFactory.getLogger(ContentHierarchyExpanderFactory.class);

    private @Autowired ContentHierarchyExpander amazonContentHierarchyExpander;
    private @Autowired ContentHierarchyExpander nitroContentHierarchyExpander;

    public ContentHierarchyExpander create(Publisher publisher){
        Map<Publisher, ContentHierarchyExpander> expanderMapping
                = ImmutableMap.<Publisher, ContentHierarchyExpander>builder()
                .put(Publisher.AMAZON_UNBOX, amazonContentHierarchyExpander)
                .put(Publisher.BBC_NITRO, nitroContentHierarchyExpander)
                .build();
        return expanderMapping.get(publisher);
    }

}
