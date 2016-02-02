package org.atlasapi.feeds.xmltv;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class XmlTvBstGmtHintTest {

    private static final DateTimeZone TIMEZONE = DateTimeZone.forOffsetHours(0);
    private static final DateTime BST_GMT_CHANGEOVER = getBstGmtChangeoverDateTime();

    private static DateTime getBstGmtChangeoverDateTime() {
    	DateTime dt = new DateTime();
    	Integer yr = dt.getYear();
    	dt = new DateTime(yr, 10, 31, 1, 0, 0, TIMEZONE);
    	if (dt.getDayOfWeek() != DateTimeConstants.SUNDAY) {
    	    dt = dt.minusDays(dt.getDayOfWeek());
    	}

    	return dt;
    }

    private static XmlTvBroadcastItem buildXmlTvBroadcastItem(String brandTitle, String containerTitle, String seriesTitle, String episodeTitle, String desc, DateTime start, DateTime finish) {
		Brand brand = new Brand("http://www.bbc.co.uk/programmes/b006m9mf", "bbc:b006m9mf", Publisher.BBC);
		brand.setTitle(brandTitle);

		Container container = new Container("http://www.bbc.co.uk/programmes/b00f4d9c", "bbc:b00f4d9c", Publisher.BBC);
		container.setTitle(containerTitle);

		Episode episode = new Episode("http://www.bbc.co.uk/programmes/b00f4d9c", "bbc:b00f4d9c", Publisher.BBC);
        episode.setTitle(episodeTitle);
        episode.setDescription(desc);
		episode.setBlackAndWhite(false);
		episode.setSeriesNumber(1);
        episode.setEpisodeNumber(1);
        episode.setPartNumber(1);
        episode.setSpecial(false);
		episode.setContainer(brand);
		episode.setContainer(container);

        Broadcast broadcast = new Broadcast("http://www.bbc.co.uk/services/bbcone", start, finish);
        broadcast.setAudioDescribed(false);
        broadcast.setHighDefinition(false);
        broadcast.setLive(false);
        broadcast.setNewEpisode(false);
        broadcast.setNewSeries(false);
        broadcast.setPremiere(false);
        broadcast.setRepeat(true);
        broadcast.setSigned(false);
        broadcast.setSubtitled(false);
        broadcast.setSurround(false);
        broadcast.setWidescreen(false);
        broadcast.setPremiere(false);

        Version version = new Version();
        version.addBroadcast(broadcast);
        episode.addVersion(version);

        Series series = new Series("seriesUri", "seriesCurie", Publisher.BBC);
		series.setTitle(seriesTitle);
        episode.setSeries(series);
		series.setChildRefs(ImmutableList.of(episode.childRef()));

        XmlTvBroadcastItem xmltvItem = new XmlTvBroadcastItem(episode, version, broadcast);
        xmltvItem.withContainer(container);
        xmltvItem.withSeries(series);

        return xmltvItem;
    }

    private static XmlTvBroadcastItem buildUnambiguousBstItem() {
    	return buildXmlTvBroadcastItem(
    			"XMLTV Tests (Brand)",
    			"XMLTV Tests (Container)",
    			"XMLTV Tests (Series)",
    			"Unambiguous BST programme",
    			"This programme is broadcast at 00:00 localtime (BST, unambiguous) before the BST->GMT changeover. No hint required.",
    			BST_GMT_CHANGEOVER.minusHours(2),
    			BST_GMT_CHANGEOVER.minusHours(1));
    }

    private static XmlTvBroadcastItem buildAmbiguousBstItem(){
    	return buildXmlTvBroadcastItem(
    			"XMLTV Tests (Brand)",
    			"XMLTV Tests (Container)",
    			"XMLTV Tests (Series)",
    			"Ambiguous BST programme",
    			"This programme is broadcast at 01:00 localtime (BST, ambiguous) before the BST->GMT changeover. BST Hint required.",
    			BST_GMT_CHANGEOVER.minusHours(1),
    			BST_GMT_CHANGEOVER);
    }

    private static XmlTvBroadcastItem buildAmbiguousGmtItem(){
    	return buildXmlTvBroadcastItem(
    			"XMLTV Tests (Brand)",
    			"XMLTV Tests (Container)",
    			"XMLTV Tests (Series)",
    			"Ambiguous GMT programme",
    			"This programme is broadcast at 01:00 localtime (GMT, ambiguous) after the BST->GMT changeover. GMT hint required.",
    			BST_GMT_CHANGEOVER,
    			BST_GMT_CHANGEOVER.plusHours(1));
    }

    private static XmlTvBroadcastItem buildUnambiguousGmtItem(){
    	return buildXmlTvBroadcastItem(
    			"XMLTV Tests (Brand)",
    			"XMLTV Tests (Container)",
    			"XMLTV Tests (Series)",
    			"Unambiguous GMT programme",
    			"This programme is broadcast at 02:00 localtime (GMT, unambiguous) after the BST->GMT changeover. No hint required.",
    			BST_GMT_CHANGEOVER.plusHours(1),
    			BST_GMT_CHANGEOVER.plusHours(2));
    }

    private Boolean isTZHintInXmlTvOutput(XmlTvBroadcastItem broadcastItem, String hint) throws Exception {
        ArrayList<XmlTvBroadcastItem> broadcastItems = new ArrayList<XmlTvBroadcastItem>();
        broadcastItems.add(broadcastItem);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlTvFeedOutputter outputter = new XmlTvFeedOutputter();
        outputter.output(broadcastItems, baos);
        String outputString = baos.toString();
        String prefix = Pattern.quote(hint);
        String regex = XmlTvModule.FEED_PREABMLE + "\r\n" + prefix;

        return Pattern.compile(regex).matcher(outputString).find();
    }

    @Test
    public void testMissingBstHint() throws Exception {
        XmlTvBroadcastItem item = buildUnambiguousBstItem();
        assertFalse(isTZHintInXmlTvOutput(item, "(BST) "));
    }

    @Test
    public void testPrependingBstHint() throws Exception {
        XmlTvBroadcastItem item = buildAmbiguousBstItem();
        assertTrue(isTZHintInXmlTvOutput(item, "(BST) "));
    }

    @Test
    public void testPrependingGmtHint() throws Exception {
        XmlTvBroadcastItem item = buildAmbiguousGmtItem();
        assertTrue(isTZHintInXmlTvOutput(item, "(GMT) "));
    }

    @Test
    public void testMissingGmtHint() throws Exception {
        XmlTvBroadcastItem item = buildUnambiguousGmtItem();
        assertFalse(isTZHintInXmlTvOutput(item, "(GMT) "));
    }
}