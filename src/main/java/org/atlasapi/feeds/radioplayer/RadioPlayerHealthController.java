package org.atlasapi.feeds.radioplayer;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.security.HttpBasicAuthChecker;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.webapp.health.HealthController;

@Controller
public class RadioPlayerHealthController {

    private final HealthController main;
    private final Iterable<String> slugs;
    private final HttpBasicAuthChecker checker;

    public RadioPlayerHealthController(HealthController main, String password) {
    	if (!Strings.isNullOrEmpty(password)) {
    		this.checker = new HttpBasicAuthChecker(ImmutableList.of(new UsernameAndPassword("bbc", password)));
    	} else {
    		this.checker = null;
    	}
        this.main = main;
        slugs = Iterables.concat(ImmutableList.of("ukrpFTP"), Iterables.transform(RadioPlayerServices.services, new Function<RadioPlayerService, String>() {
            @Override
            public String apply(RadioPlayerService service) {
                return "ukrp"+service.getName();
            }
        }));
    }
    
    @RequestMapping("feeds/ukradioplayer/health")
    public String radioplayerHealth(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	if (checker == null) {
    		response.setContentType(MimeType.TEXT_PLAIN.toString());
    		response.getOutputStream().print("No password set up, health page cannot be viewed");
    		return null;
    	}
    	boolean allowed = checker.check(request);
    	if (allowed) {
    		return main.showHealthPageForSlugs(response, slugs);
    	}
    	HttpBasicAuthChecker.requestAuth(response, "Heath Page");
    	return null;
    }
    
}
