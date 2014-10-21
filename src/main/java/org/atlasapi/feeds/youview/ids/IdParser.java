package org.atlasapi.feeds.youview.ids;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;


public interface IdParser {

    String createCrid(String cridPrefix, Content content);
    String createVersionCrid(String cridPrefix, Content content);
    String createImi(String imiPrefix, Item item);
}
