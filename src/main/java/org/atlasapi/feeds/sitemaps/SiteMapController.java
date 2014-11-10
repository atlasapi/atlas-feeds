package org.atlasapi.feeds.sitemaps;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.metabroadcast.common.http.HttpStatusCode.BAD_REQUEST;
import static org.atlasapi.feeds.sitemaps.SiteMapRef.transformerForBaseUri;
import static org.atlasapi.persistence.content.ContentCategory.CHILD_ITEM;
import static org.atlasapi.persistence.content.ContentCategory.CONTAINER;
import static org.atlasapi.persistence.content.ContentCategory.PROGRAMME_GROUP;
import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.application.query.ApiKeyNotFoundException;
import org.atlasapi.application.query.InvalidIpForApiKeyException;
import org.atlasapi.application.query.RevokedApiKeyException;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.SeriesRef;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.query.content.parser.ApplicationConfigurationIncludingQueryBuilder;
import org.mortbay.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.webapp.http.CacheHeaderWriter;

@Controller
public class SiteMapController {

    private static Logger log = LoggerFactory.getLogger(SiteMapController.class);
    
    private static final String HOST_PARAM = "host";
    private static final String BASE_URI_PARAM = "baseUri";
    private static final String PUBLISHER_PARAM = "publisher";

    private final ContentLister lister;
    private final String defaultHost;
    private final ApplicationConfigurationIncludingQueryBuilder queryBuilder;
    private final KnownTypeQueryExecutor queryExecutor;

    private final SiteMapOutputter outputter;
    private final SiteMapIndexOutputter indexOutputter = new SiteMapIndexOutputter();
    private final CacheHeaderWriter cacheHeaderWriter = CacheHeaderWriter.neverCache();
    
    public SiteMapController(KnownTypeQueryExecutor queryExecutor, 
            ApplicationConfigurationIncludingQueryBuilder queryBuilder, 
            ContentLister contentLister, 
            Map<Publisher, SiteMapUriGenerator> siteMapUriGenerators,
            String defaultHost, Long serviceId) {
        this.queryExecutor = queryExecutor;
        this.queryBuilder = queryBuilder;
        this.lister = contentLister;
        this.defaultHost = defaultHost;
        this.outputter = new SiteMapOutputter(siteMapUriGenerators, new DefaultSiteMapUriGenerator(), serviceId);
    }

    @RequestMapping("/feeds/sitemaps/index.xml")
    public String siteMapFofPublisher(@RequestParam(value = PUBLISHER_PARAM) final String publisher, 
            @RequestParam(value = HOST_PARAM, required = false) final String hostParam, 
            @RequestParam(value = BASE_URI_PARAM, required = false) final String baseUriParam, 
            HttpServletRequest request, HttpServletResponse response) throws IOException {
        
        Maybe<Publisher> possiblePublisher = Publisher.fromKey(publisher);
        if(possiblePublisher.isNothing()) {
            response.sendError(BAD_REQUEST.code(), "Unknown publisher " + publisher);
            return null;
        }
        
        if (hostParam != null && baseUriParam != null) {
            response.sendError(BAD_REQUEST.code(), "Cannot specify both host and base URI");
            return null;
        }
        
        ContentQuery query;
        try {
            query = queryBuilder.build(new SitemapHackHttpRequest(request));
        } catch (ApiKeyNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (RevokedApiKeyException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (InvalidIpForApiKeyException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
       
        Set<Publisher> includedPublishers = query.getConfiguration().getEnabledSources();
        
        Iterable<SiteMapRef> sitemapRefs;
        String baseUri = baseUriParam == null ? baseUriForHost(hostParam) : baseUriParam;
        
        if (includedPublishers.contains(possiblePublisher.requireValue())) {
            sitemapRefs = sitemapRefForQuery(query, baseUri, possiblePublisher.requireValue());
        } else {
            sitemapRefs = ImmutableList.<SiteMapRef> of();
        }

        response.setStatus(HttpServletResponse.SC_OK);
        cacheHeaderWriter.writeHeaders(request, response);
        
        indexOutputter.output(sitemapRefs, response.getOutputStream());
        
        return null;
    }

    public Iterable<SiteMapRef> sitemapRefForQuery(ContentQuery query, final String baseUri, Publisher publisher) {
        final ImmutableSet.Builder<String> brands = ImmutableSet.builder();
        Iterator<Content> contents = Iterators.filter(
                lister.listContent(
                        defaultCriteria()
                        .forPublisher(publisher)
                        .forContent(CHILD_ITEM, CONTAINER, PROGRAMME_GROUP)
                        .build()), 
                Content.class);
        while (contents.hasNext()) {
            Content content = contents.next();
            if (content instanceof Item) {
                Item item = (Item) content;
                if((item.getThumbnail() != null 
                        && outputter.hasRequiredAttributesForOutput(item)
                        && ( outputter.itemLocation(item).isPresent()
                                || hasValidClip(item)))) {
                    brands.add(item.getContainer().getUri());
                }
            } else if (content instanceof Series) {
                Series series = (Series) content;
                if (!series.getClips().isEmpty()) {
                    brands.add(series.getParent().getUri());
                }
            } else if (content instanceof Brand) {
                Brand brand = (Brand) content;
                if (hasValidClip(brand)) {
                    brands.add(brand.getCanonicalUri());
                }
            } else {
                log.warn("Ignoring content of unsupported type: " + content.getClass().getCanonicalName());
            }
        }

        Iterable<SiteMapRef> sitemapRefs = transform(resolve(brands.build(), query), transformerForBaseUri(baseUri));
        return sitemapRefs;
    }

    private boolean hasValidClip(Content content) {
        for (Clip clip : content.getClips()) {
            if (outputter.clipLocation(clip).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private Iterable<Content> resolve(Iterable<String> brands, ContentQuery query) {
        return filter(concat(queryExecutor.executeUriQuery(brands, query).values()), Content.class);
    }

    @RequestMapping("/feeds/sitemaps/sitemap.xml")
    public String siteMapForBrand(HttpServletRequest request, HttpServletResponse response, 
            @RequestParam("brand.uri") String brandUri) throws IOException {
        try {
            final ContentQuery query = queryBuilder.build(new SitemapHackHttpRequest(request, brandUri));
            Iterable<Container> brands = Iterables.filter(resolve(URI_SPLITTER.split(brandUri),query), Container.class);
        
            Map<ParentRef, Container> parentLookup = Maps.<ParentRef,Container>uniqueIndex(brands, ParentRef.T0_PARENT_REF);
            Iterable<Content> contents = Iterables.concat(Iterables.transform(brands, new Function<Container, Iterable<Content>>() {
                @Override
                public Iterable<Content> apply(Container input) {
                    return resolve(childItemsFor((Brand)input), query);
                }
            }));
        
            response.setStatus(HttpServletResponse.SC_OK);
            cacheHeaderWriter.writeHeaders(request, response);
            outputter.output(parentLookup, contents, response.getOutputStream());
            return null;
        } catch (ApiKeyNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (RevokedApiKeyException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        } catch (InvalidIpForApiKeyException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
    }

    protected Iterable<String> childItemsFor(Brand brand) {
        return Iterables.concat(
                Iterables.transform(brand.getChildRefs(), ChildRef.TO_URI),
                Iterables.transform(brand.getSeriesRefs(), SeriesRef.TO_URI)
               );
    }

    private static final Splitter URI_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

    private String baseUriForHost(String host) {
        return String.format("http://%s/feeds/sitemaps", host == null ? defaultHost : host);
    }
    
    private static class SitemapHackHttpRequest extends HttpServletRequestWrapper {
        private final Map<String, String[]> params;

        @SuppressWarnings("unchecked")
        public SitemapHackHttpRequest(HttpServletRequest request, String brandUri) {
            super(request);
            params = Maps.newHashMap(request.getParameterMap());
            if (brandUri != null) {
                params.put("uri", new String[] { brandUri });
            }
            params.remove("brand.uri");
            params.remove("baseUri");
        }
        
        public SitemapHackHttpRequest(HttpServletRequest request) {
            this(request, null);
        }

        public String getParameter(String name) {
            String[] values = this.params.get(name);
            if (values != null && values.length > 0) {
                return values[0];
            }
            return null;
        }

        @SuppressWarnings("rawtypes")
        public Map getParameterMap() {
            return ImmutableMap.copyOf(this.params);
        }

        @SuppressWarnings("rawtypes")
        public Enumeration getParameterNames() {
            return Collections.enumeration(this.params.keySet());
        }

        public String[] getParameterValues(String name) {
            return this.params.get(name);
        }
    }
}
