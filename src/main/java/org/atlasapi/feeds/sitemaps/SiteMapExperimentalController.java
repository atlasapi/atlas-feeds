package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.MongoDbBackedContentStore;
import org.atlasapi.query.content.parser.ApplicationConfigurationIncludingQueryBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.metabroadcast.common.query.Selection;

@Controller
public class SiteMapExperimentalController {

	private static final String HOST_PARAM = "host";
	private static final String FORMAT_PARAM = "format";
	
    private final MongoDbBackedContentStore queryExecutor;
    private final SiteMapOutputter outputter = new SiteMapOutputter();
    private final SiteMapIndexOutputter indexOutputter = new SiteMapIndexOutputter();
    
    private final String defaultHost;
	private final ApplicationConfigurationIncludingQueryBuilder queryBuilder;

    public SiteMapExperimentalController(MongoDbBackedContentStore mongoStore, ApplicationConfigurationIncludingQueryBuilder queryBuilder, String defaultHost) {
		this.queryExecutor = mongoStore;
		this.queryBuilder = queryBuilder;
		this.defaultHost = defaultHost;
	}
	
	@RequestMapping("/feeds/experimental/sitemaps/sitemap.xml")
	public String siteMapForBrand(HttpServletRequest request, HttpServletResponse response, @RequestParam(value=FORMAT_PARAM) String format) throws IOException {
//		List<Brand> brands = queryExecutor.dehydratedBrandsMatching(queryBuilder.build(request));
//		response.setStatus(HttpServletResponse.SC_OK);
//		outputter.outputBrands(brands, format, response.getOutputStream());
		return null;
	}
	
	@RequestMapping("/feeds/experimental/sitemaps/index.xml")
	public String siteMapFofPublisher(@RequestParam(value=HOST_PARAM, required=false) final String host, @RequestParam(value=FORMAT_PARAM, required=false) final String format,  HttpServletRequest request, HttpServletResponse response) throws IOException {
//		List<SiteMapRef> refs = Lists.newArrayList();
//		for (Publisher publisher : Publisher.values()) {
//			List<Brand> brands = queryExecutor.dehydratedBrandsMatching(queryBuilder.build(request, ImmutableList.<AtomicQuery>of(Attributes.DESCRIPTION_PUBLISHER.createQuery(Operators.EQUALS, ImmutableList.of(publisher))), Selection.limitedTo(1)));
//			if (!brands.isEmpty()) {
//				refs.add(new SiteMapRef("http://" + hostOrDefault(host) +  "/feeds/experimental/sitemaps/sitemap.xml?publisher=" + publisher.key() + "&format=" + format , null));
//			}
//		}
//		response.setStatus(HttpServletResponse.SC_OK);
//		indexOutputter.output(refs, response.getOutputStream());
		return null;
	}

	private String hostOrDefault(String host) {
		return host == null ? defaultHost : host;
	}
}
