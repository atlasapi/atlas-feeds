package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.query.content.parser.QueryStringBackedQueryBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.Lists;

@Controller
public class SiteMapExperimentalController {

	private static final String HOST_PARAM = "host";
	private static final String FORMAT_PARAM = "format";
	
    private final KnownTypeQueryExecutor queryExecutor;
    private final SiteMapOutputter outputter = new SiteMapOutputter();
    private final SiteMapIndexOutputter indexOutputter = new SiteMapIndexOutputter();
    private final QueryStringBackedQueryBuilder queryBuilder = new QueryStringBackedQueryBuilder().withIgnoreParams(FORMAT_PARAM);
    
    private final String defaultHost;

    public SiteMapExperimentalController(KnownTypeQueryExecutor queryExecutor, String defaultHost) {
		this.queryExecutor = queryExecutor;
		this.defaultHost = defaultHost;
	}
	
	@RequestMapping("/feeds/experimental/sitemaps/sitemap.xml")
	public String siteMapForBrand(HttpServletRequest request, HttpServletResponse response, @RequestParam(value=FORMAT_PARAM) String format) throws IOException {
		List<Brand> brands = queryExecutor.executeBrandQuery(queryBuilder.build(request, Brand.class));
		response.setStatus(HttpServletResponse.SC_OK);
		outputter.outputBrands(brands, format, response.getOutputStream());
		return null;
	}
	
	@RequestMapping("/feeds/experimental/sitemaps/index.xml")
	public String siteMapFofPublisher(@RequestParam(value=HOST_PARAM, required=false) final String host, @RequestParam(value=FORMAT_PARAM, required=false) final String format,  HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<SiteMapRef> refs = Lists.newArrayList();
		refs.add(new SiteMapRef("http://" + hostOrDefault(host) +  "/feeds/experimental/sitemaps/sitemap.xml?publisher=bbc.co.uk&format=" + format , null));
		refs.add(new SiteMapRef("http://" + hostOrDefault(host) +  "/feeds/experimental/sitemaps/sitemap.xml?publisher=channel4.com&format=" + format , null));
		response.setStatus(HttpServletResponse.SC_OK);
		indexOutputter.output(refs, response.getOutputStream());
		return null;
	}

	private String hostOrDefault(String host) {
		return host == null ? defaultHost : host;
	}
}
