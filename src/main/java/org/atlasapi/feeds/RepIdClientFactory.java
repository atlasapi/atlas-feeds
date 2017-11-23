package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.feeds.tasks.youview.creation.TaskCreationTask;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.representative.client.RepIdClientWithApp;
import com.metabroadcast.representative.client.http.HttpExecutor;
import com.metabroadcast.representative.client.http.RetryStrategy;

import com.google.common.collect.ImmutableMap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class RepIdClientFactory {

    private static final String REP_SERVICE_HOST = Configurer.get("repid.service.host").get();
    private static final Map<Publisher, String> publisherToAppIdMap = ImmutableMap.of(
            Publisher.BBC_NITRO, "nitroAppId", //not in use. Equiv not running for nitro.
            Publisher.AMAZON_UNBOX, Configurer.get("youview.upload.unbox.equivapikey").get()
    );

    private static final Logger log = LoggerFactory.getLogger(RepIdClientFactory.class);

    private RepIdClientFactory() { }

    public static RepIdClientWithApp getRepIdClient(Publisher publisher) {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setServiceUnavailableRetryStrategy(new RetryStrategy())
                .build();

        String host;
        Integer port;

        checkNotNull(REP_SERVICE_HOST);
        if (REP_SERVICE_HOST.contains(":")) {
            String[] parts = REP_SERVICE_HOST.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            host = REP_SERVICE_HOST;
            port = null;
        }

        log.info("Asked to create an executor for host={} port={}", host, port);
        HttpExecutor httpExecutor = HttpExecutor.create(httpClient, host, port);

        return new RepIdClientWithApp(
                httpExecutor,
                publisherToAppIdMap.get(publisher)
        );

    }
}
