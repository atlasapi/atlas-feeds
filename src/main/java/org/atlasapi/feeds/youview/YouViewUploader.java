package org.atlasapi.feeds.youview;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.http.HttpResponse;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.http.StringPayload;
import com.metabroadcast.common.scheduling.ScheduledTask;

public class YouViewUploader extends ScheduledTask {

    private final String youViewUrl;
    private final String youViewUser;
    private final String youViewPassword;
    
    private final Logger log = LoggerFactory.getLogger(YouViewUploader.class);

    public YouViewUploader(String youViewUrl, String youViewUser, String youViewPassword) {
        this.youViewUrl = youViewUrl;
        this.youViewUser = youViewUser;
        this.youViewPassword = youViewPassword;
    }
    
    @Override
    protected void runTask() {
//        String url = youViewUrl;
//        
//        //String jsonItem = gson.toJson(item);
//        log.info(String.format("Posting YouView output xml to %s", youViewUrl));
//        HttpResponse response = httpClient.post(url, new StringPayload(jsonItem));
//        if (response.statusCode() != HttpServletResponse.SC_CREATED) {
//            throw new RuntimeException("An Http status code of " + response.statusCode() + " was returned when POSTing to YouView");
//        }
        
    }

}
