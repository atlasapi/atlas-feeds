package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.atlasapi.feeds.youview.services.ServiceMapping;
import org.atlasapi.feeds.youview.services.ServiceMappingLineProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.google.common.io.Resources;


public class NitroServiceMapping implements ServiceMapping {
    
    private final Logger log = LoggerFactory.getLogger(NitroServiceMapping.class);
    private final Multimap<String, String> youViewServiceIdMap;
    
    public NitroServiceMapping(String fileName) {
        this.youViewServiceIdMap = readFile(checkNotNull(fileName));
    }

    private Multimap<String, String> readFile(String fileName) {
        try {
            URL resource = Resources.getResource(getClass(), fileName);
            InputSupplier<InputStreamReader> supplier = Resources.newReaderSupplier(resource, Charsets.UTF_8);

            return CharStreams.readLines(supplier, new ServiceMappingLineProcessor());
        } catch (IOException e) {
            log.error(String.format("Error reading genre file %s", fileName), e);
            return null;
        }        
    }

    @Override
    public Iterable<String> youviewServiceIdFor(String bbcServiceId) {
        return youViewServiceIdMap.get(bbcServiceId);
    }

}
