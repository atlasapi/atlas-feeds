package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.query.content.parser.ApplicationConfigurationIncludingQueryBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

@Controller
public class SiteMapController {

    private static final String HOST_PARAM = "host";
	
    private final KnownTypeQueryExecutor queryExecutor;
    private final SiteMapOutputter outputter = new SiteMapOutputter();
    private final SiteMapIndexOutputter indexOutputter = new SiteMapIndexOutputter();
    private final String defaultHost;
    private final ApplicationConfigurationIncludingQueryBuilder queryBuilder;

    public SiteMapController(KnownTypeQueryExecutor queryExecutor, ApplicationConfigurationIncludingQueryBuilder queryBuilder, String defaultHost) {
		this.queryExecutor = queryExecutor;
        this.queryBuilder = queryBuilder;
		this.defaultHost = defaultHost;
	}
	
	@RequestMapping("/feeds/sitemaps/sitemap.xml")
	public String siteMapForBrand(HttpServletRequest request, HttpServletResponse response) throws IOException {
		List<Item> brands = queryExecutor.executeItemQuery(queryBuilder.build(request, Item.class));
		response.setStatus(HttpServletResponse.SC_OK);
		outputter.output(brands, response.getOutputStream());
		return null;
	}
	
	@RequestMapping("/feeds/sitemaps/index.xml")
	public String siteMapFofPublisher(@RequestParam(value=HOST_PARAM, required=false) final String host, HttpServletRequest request, HttpServletResponse response) throws IOException {
		ContentQuery query = queryBuilder.build(request, Brand.class);
		
        ContentQuery requestQuery = new ContentQuery(Iterables.concat(
		        query.operands(),
		        ImmutableList.<AtomicQuery>of(Attributes.LOCATION_TRANSPORT_TYPE.createQuery(Operators.EQUALS, ImmutableList.of(TransportType.LINK)))
		)).copyWithApplicationConfiguration(query.getConfiguration());
		
        List<? extends Content> brands = queryExecutor.executeBrandQuery(requestQuery);
		
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
