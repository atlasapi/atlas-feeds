package org.atlasapi.feeds;

import java.util.Map;

import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.representative.client.RepIdClientWithApp;
import com.metabroadcast.representative.client.http.HttpExecutor;
import com.metabroadcast.representative.client.http.RetryStrategy;

import com.google.common.collect.ImmutableMap;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import static com.google.common.base.Preconditions.checkNotNull;

public class RepIdClientFactory {

    private static final String REP_SERVICE_HOST = "representative-id-service.stage.svc.cluster.local";
    private static final Map<Publisher, String> publisherToAppIdMap = ImmutableMap.of(
            Publisher.BBC_NITRO, "nitroAppId",
            Publisher.AMAZON_UNBOX, "jd9" //TODO:hardcoded appId and host ^
    );

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
        HttpExecutor httpExecutor = HttpExecutor.create(httpClient, host, port);

        return new RepIdClientWithApp(
                httpExecutor,
                publisherToAppIdMap.get(publisher)
        );

    }
}
