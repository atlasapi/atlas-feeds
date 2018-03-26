package org.atlasapi.feeds;

import org.atlasapi.feeds.youview.PerPublisherConfig;
import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.common.properties.Configurer;
import com.metabroadcast.representative.api.Version;
import com.metabroadcast.representative.client.RepIdClientWithApp;
import com.metabroadcast.representative.client.http.HttpExecutor;
import com.metabroadcast.representative.client.http.RetryStrategy;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

public class RepIdClientFactory {

    private static final Logger log = LoggerFactory.getLogger(RepIdClientFactory.class);

    private RepIdClientFactory() { }

    public static RepIdClientWithApp getRepIdClient(Publisher publisher) {
        CloseableHttpClient httpClient = HttpClients.custom()
                .setServiceUnavailableRetryStrategy(new RetryStrategy())
                .build();

        String host;
        Integer port;

       String repServiceHost = checkNotNull(Configurer.get("repid.service.host").get());
        if (repServiceHost.contains(":")) {
            String[] parts = repServiceHost.split(":");
            host = parts[0];
            port = Integer.parseInt(parts[1]);
        } else {
            host = repServiceHost;
            port = null;
        }

        HttpExecutor httpExecutor = HttpExecutor.create(httpClient, host, port);

        return new RepIdClientWithApp(
                httpExecutor,
                PerPublisherConfig.TO_APP_ID_MAP.get(publisher),
                Version.OWL
        );

    }
}
