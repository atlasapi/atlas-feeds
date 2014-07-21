package org.atlasapi.feeds.radioplayer.upload.queue;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.persistence.translator.TranslatorUtils;
import com.mongodb.DBObject;


public class TranslationUtils {

    private TranslationUtils() {}
    
    public static Map<String, String> toMap(DBObject dbo, String key) {
        ImmutableMap.Builder<String, String> map = ImmutableMap.<String, String>builder();
        for (String mapKey : TranslatorUtils.toDBObject(dbo, key).keySet()) {
            map.put(mapKey, (String) TranslatorUtils.toDBObject(dbo, key).get(mapKey));
        }
        return map.build();
    }
}
