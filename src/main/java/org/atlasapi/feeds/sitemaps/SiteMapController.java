package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.query.content.parser.QueryStringBackedQueryBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

@Controller
public class SiteMapController {

	private static final String API_KEY_PARAM = "apiKey";
    private static final String HOST_PARAM = "host";
	
    private final KnownTypeQueryExecutor queryExecutor;
    private final SiteMapOutputter outputter = new SiteMapOutputter();
    private final SiteMapIndexOutputter indexOutputter = new SiteMapIndexOutputter();
    private final QueryStringBackedQueryBuilder queryBuilder = new QueryStringBackedQueryBuilder().withIgnoreParams(HOST_PARAM,API_KEY_PARAM);
    
    private final String defaultHost;

    public SiteMapController(KnownTypeQueryExecutor queryExecutor, String defaultHost) {
		this.queryExecutor = queryExecutor;
		this.defaultHost = defaultHost;
	}
	
	@RequestMapping("/feeds/sitemaps/sitemap.xml")
	public String siteMapForBrand(HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<Content> brands = queryExecutor.discover(queryBuilder.build(request));
		response.setStatus(HttpServletResponse.SC_OK);
		outputter.output(Iterables.filter(brands, Item.class), response.getOutputStream());
		return null;
	}
	
	@RequestMapping("/feeds/sitemaps/index.xml")
	public String siteMapFofPublisher(@RequestParam(value=HOST_PARAM, required=false) final String host, HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<? extends Content> brands = queryExecutor.discover(queryBuilder.build(request));
		
		Iterable<SiteMapRef> refs = Iterables.transform(brands, new Function<Content, SiteMapRef>() {

			@Override
			public SiteMapRef apply(Content brand) {
				return new SiteMapRef("http://" + hostOrDefault(host) +  "/feeds/sitemaps/sitemap.xml?brand.uri=" + brand.getCurie(), brand.getLastUpdated());
			}

		});
		response.setStatus(HttpServletResponse.SC_OK);
		indexOutputter.output(refs, response.getOutputStream());
		return null;
	}

	private String hostOrDefault(String host) {
		return host == null ? defaultHost : host;
	}
}
