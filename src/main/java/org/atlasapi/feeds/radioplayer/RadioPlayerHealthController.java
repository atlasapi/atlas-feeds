package org.atlasapi.feeds.radioplayer;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.webapp.health.HealthController;

@Controller
public class RadioPlayerHealthController {

    
    private final HealthController main;
    private final Iterable<String> slugs;

    public RadioPlayerHealthController(HealthController main) {
        this.main = main;
        slugs = Iterables.concat(ImmutableList.of("ukrpFTP"), Iterables.transform(RadioPlayerServices.services, new Function<RadioPlayerService, String>() {
            @Override
            public String apply(RadioPlayerService service) {
                return "ukrp"+service.getName();
            }
        }));
    }
    
    @RequestMapping("feeds/ukradioplayer/health")
    public String radioplayerHealth(HttpServletResponse response) throws IOException {
        return main.showHealthPageForSlugs(response, slugs);
    }
    
}
