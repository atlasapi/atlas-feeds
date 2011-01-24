package org.atlasapi.feeds.radioplayer;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.metabroadcast.common.webapp.health.HealthController;

@Controller
public class RadioPlayerHealthController {

    
    private final HealthController main;

    public RadioPlayerHealthController(HealthController main) {
        this.main = main;
    }
    
    @RequestMapping("feeds/ukradioplayer/health")
    public String radioplayerHealth(HttpServletResponse response) throws IOException {
        return main.showHealthPageForSlugs(response, "ukrpfiles");
    }
    
}
