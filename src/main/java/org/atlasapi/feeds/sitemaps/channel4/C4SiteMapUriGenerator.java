package org.atlasapi.feeds.sitemaps.channel4;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.feeds.sitemaps.SiteMapUriGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Series;

import com.google.common.base.Optional;
import com.google.common.base.Supplier;


public class C4SiteMapUriGenerator implements SiteMapUriGenerator {

    private static final Pattern FOUR_OD_ID_FROM_URI_PATTERN = Pattern.compile("^.*4od#(\\d+)$");
            
    private static final String BRIGHTCOVE_URI_FORMAT 
        = "http://c.brightcove.com/services/viewer/federated_f9/?isVid=1&domain=embed&playerID=%s&publisherID=%s&videoID=%s";
    
    private static final String FLASH_URI_FORMAT
        = "http://www.channel4.com/static/programmes-bips-flash/%s/4odplayer_bips.swf?preSelectAsset=%s";
    
    private final String brightcovePublisherId;
    private final String brightcovePlayerId;
    private final Supplier<String> flashPlayerVersionSupplier;

    public C4SiteMapUriGenerator(String brightcovePublisherId, String brightcovePlayerId, 
            Supplier<String> flashPlayerVersionSupplier) {
        this.flashPlayerVersionSupplier = checkNotNull(flashPlayerVersionSupplier);
        this.brightcovePlayerId = checkNotNull(brightcovePlayerId);
        this.brightcovePublisherId = checkNotNull(brightcovePublisherId);
    }
    
    @Override
    public Optional<String> playerPageUriForContent(Content content, Location location) {
        return Optional.fromNullable(location.getUri());
    }
    
    @Override
    public Optional<String> videoUriForContent(Content content, Location location) {
        Matcher matcher = FOUR_OD_ID_FROM_URI_PATTERN.matcher(location.getUri());
        
        if (matcher.matches()) {
            return Optional.of(
                    String.format(
                        FLASH_URI_FORMAT, 
                        flashPlayerVersionSupplier.get(),
                        matcher.group(1))
                   );
        }
        return Optional.absent();
    }
 
    @Override
    public Optional<String> videoUriForClip(Clip clip, Location location) {
        return Optional.of(
                String.format(
                    BRIGHTCOVE_URI_FORMAT, 
                    brightcovePlayerId, 
                    brightcovePublisherId, 
                    location.getEmbedId()
                )
               );
    }
    
    @Override
    public Optional<String> playerPageUriForClip(Content content, Clip clip, Location location) {
        String topLevelUri = topLevelUri(content);
        return Optional.of(
                topLevelUri.replace("http://pmlsc.channel4.com/pmlsd/", "http://www.channel4.com/programmes/")
                + "/videos/all/" + clipUriFromTitle(clip.getTitle())
               );
    }
    
    private String clipUriFromTitle(String title) {
        return title.replaceAll("[^A-Za-z0-9 ]", "").toLowerCase().replaceAll("\\ ", "-");
    }

    /**
     * Returns the canonical URI of the top-level container
     * 
     * @param content
     */
    private String topLevelUri(Content content) {
        if (content instanceof Item) {
            Item item = (Item) content;
            return item.getContainer().getUri();
        }
        if (content instanceof Series) {
            Series series = (Series) content;
            return series.getParent().getUri();
        }
        if (content instanceof Brand) {
            return content.getCanonicalUri();
        }
        throw new IllegalArgumentException(
                    "Content of type " 
                     + content.getClass().getCanonicalName() 
                     + " not supported");
    }

   
}
