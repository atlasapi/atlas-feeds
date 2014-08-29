package org.atlasapi.feeds.sitemaps;

import org.atlasapi.media.entity.Content;
import org.joda.time.DateTime;

import com.google.common.base.Function;

public class SiteMapRef {

    public static final Function<Content, SiteMapRef> transformerForBaseUri(final String baseUri) {
        return new Function<Content, SiteMapRef>() {
            @Override
            public SiteMapRef apply(Content brand) {
                return SiteMapRef.sitemapRefFrom(brand, baseUri);
            }
        };
    };
    
    public static final SiteMapRef sitemapRefFrom(Content brand, String baseUri) {
        return new SiteMapRef(String.format("%s/sitemap.xml?brand.uri=%s", baseUri, uriFor(brand)), brand.getLastUpdated());
    }
    
    private static final String uriFor(Content content) {
        if (content.getCurie() != null) {
            return content.getCurie();
        }
        return content.getCanonicalUri();
    }
    
	private final String url;
	private final DateTime lastModified;

	public SiteMapRef(String url, DateTime lastModified) {
		this.url = url;
		this.lastModified = lastModified;
	}
	
	public String getUrl() {
		return url;
	}

	public DateTime getLastModified() {
		return lastModified;
	}
}
