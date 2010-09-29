package org.atlasapi.feeds.sitemaps;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.StringAttributeQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;

@Controller
public class SiteMapController {

    private final KnownTypeQueryExecutor queryExecutor;
    private final SiteMapOutputter outputter = new SiteMapOutputter();
    private final SiteMapIndexOutputter indexOutputter = new SiteMapIndexOutputter();

    public SiteMapController(KnownTypeQueryExecutor queryExecutor) {
		this.queryExecutor = queryExecutor;
	}
	
	@RequestMapping("/sitemaps/{curie}/sitemap.xml")
	public String siteMapForBrand(@PathVariable("curie") String uri, HttpServletResponse response) throws IOException {
		List<Brand> brands = queryExecutor.executeBrandQuery(new ContentQuery(new StringAttributeQuery(Attributes.BRAND_URI, Operators.EQUALS, ImmutableList.of(uri))));
		if (brands.isEmpty()) {
			return notFound(response);
		}
		Brand brand = Iterables.getOnlyElement(brands);
		response.setStatus(HttpServletResponse.SC_OK);
		outputter.output(brand.getItems(), response.getOutputStream());
		return null;
	}
	
	@RequestMapping("/sitemapindexes/{publisher}/sitemapindex.xml")
	public String siteMapFofPublisher(@PathVariable("publisher") String publisherString, HttpServletResponse response) throws IOException {
		Maybe<Publisher> publisher = Publisher.fromKey(publisherString);
		if (publisher.isNothing()) {
			return notFound(response);
		}
		List<Brand> brands = queryExecutor.executeBrandQuery(new ContentQuery(new StringAttributeQuery(Attributes.BRAND_PUBLISHER, Operators.EQUALS, ImmutableList.of(publisher.requireValue().key()))));
		response.setStatus(HttpServletResponse.SC_OK);
		
		Iterable<SiteMapRef> refs = Iterables.transform(brands, new Function<Brand, SiteMapRef>() {

			@Override
			public SiteMapRef apply(Brand brand) {
				return new SiteMapRef("http://atlasapi.org/sitemap/" + brand.getCurie() + "/sitemap.xml", brand.getLastUpdated());
			}
		});
		response.setStatus(HttpServletResponse.SC_OK);
		indexOutputter.output(refs, response.getOutputStream());
		return null;
	}

	private String notFound(HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		response.setContentLength(0);
		return null;
	}
}
