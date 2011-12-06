package org.atlasapi.feeds.xmltv;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Set;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.CrewMember.Role;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.DateTimeZones;

public class XmlTvFeedOutputter {

    private final String EMPTY_FIELD = "";
    private final Joiner fieldJoiner;

    public XmlTvFeedOutputter() {
        this.fieldJoiner = Joiner.on("~");
    }

    public void output(List<XmlTvBroadcastItem> items, OutputStream stream) throws IOException {
        Writer writer = new OutputStreamWriter(stream, Charsets.UTF_8);
        writer.write(XmlTvModule.FEED_PREABMLE);
        for (XmlTvBroadcastItem broadcastItem : items) {
            writer.write('\n');
            writer.write(join(extractFields(broadcastItem)));
        }
        writer.flush();
    }

    private String join(List<String> extractedFields) {
        return fieldJoiner.join(extractedFields);
    }

    private List<String> extractFields(XmlTvBroadcastItem broadcastItem) {
        Broadcast broadcast = broadcastItem.getBroadcast();
        return ImmutableList.of(
                programmeTitle(broadcastItem),
                subTitle(broadcastItem),
                episode(broadcastItem),
                year(broadcastItem),
                director(broadcastItem),
                performers(broadcastItem),
                toString(broadcast.getPremiere()),
                toString(broadcastItem.getItem() instanceof Film),
                toString(broadcast.getRepeat()),
                toString(broadcast.getSubtitled()),
                toString(broadcast.getWidescreen()),
                toString(broadcast.getNewSeries()),
                toString(broadcast.getSigned()),
                toString(broadcastItem.getItem().getBlackAndWhite()),
                EMPTY_FIELD,//placeholder for film star rating
                EMPTY_FIELD,//placeholder for film certificate
                genre(broadcastItem.getItem().getGenres()),
                description(broadcastItem),
                EMPTY_FIELD,// RT Choice
                date(broadcast.getTransmissionTime()),
                time(broadcast.getTransmissionTime()),
                time(broadcast.getTransmissionEndTime()),
                String.valueOf(broadcast.getBroadcastDuration() / 60)
        );
    }

    private String description(XmlTvBroadcastItem broadcastItem) {
        return broadcastItem.getItem().getDescription() != null ? broadcastItem.getItem().getDescription() : EMPTY_FIELD;
    }

    private String performers(XmlTvBroadcastItem broadcastItem) {
        return Joiner.on("|").skipNulls().join(Iterables.transform(Iterables.filter(broadcastItem.getItem().getPeople(), Actor.class), new Function<Actor, String>() {
            @Override
            public String apply(Actor input) {
                return String.format("%s*%s", input.character(), input.name());
            }
        }));
    }

    private String director(XmlTvBroadcastItem broadcastItem) {
        for (CrewMember crewMember : broadcastItem.getItem().getPeople()) {
            if(Role.DIRECTOR.equals(crewMember.role())) {
                return crewMember.name();
            }
        }
        return EMPTY_FIELD;
    }

    private String year(XmlTvBroadcastItem broadcastItem) {
        return broadcastItem.getItem() instanceof Film ? String.valueOf(((Film)broadcastItem.getItem()).getYear()) : EMPTY_FIELD;
    }

    private String episode(XmlTvBroadcastItem broadcastItem) {
        final String title = broadcastItem.getItem().getTitle();
        return isEpisode(broadcastItem) && title != null && !Objects.equal(title, broadcastItem.getContainer().getTitle()) ? title : EMPTY_FIELD;
    }

    private String genre(Set<String> genres) {
        return "No Genre";
    }

    private String subTitle(XmlTvBroadcastItem broadcastItem) {
        if (isEpisode(broadcastItem)) {
            Episode episode = (Episode)broadcastItem.getItem();
            if(broadcastItem.hasSeries()) {
                return subtitleForEpisodeAndSeries(episode, broadcastItem.getSeries());
            }
            if(episode.getEpisodeNumber() != null) {
                return String.valueOf(episode.getEpisodeNumber());
            }
        }
        return EMPTY_FIELD;
    }

    private String subtitleForEpisodeAndSeries(Episode item, Series series) {
        if(series.getTotalEpisodes() != null) {
            return String.format("%s/%s, series %s", item.getEpisodeNumber(), series.getTotalEpisodes(), series.getSeriesNumber());
        }
        return String.format("%s, series %s", item.getEpisodeNumber(), series.getSeriesNumber());
    }

    private String programmeTitle(XmlTvBroadcastItem broadcastItem) {
        return isEpisode(broadcastItem) && broadcastItem.hasContainer() ? broadcastItem.getContainer().getTitle() : broadcastItem.getItem().getTitle();
    }

    private boolean isEpisode(XmlTvBroadcastItem broadcastItem) {
        return broadcastItem.getItem() instanceof Episode;
    }

    private String date(DateTime date) {
        return date.toDateTime(DateTimeZones.LONDON).toString("dd/MM/yyyy");
    }

    private String time(DateTime time) {
        return time.toDateTime(DateTimeZones.LONDON).toString("HH:mm");
    }

    private String toString(Boolean flag) {
        return Boolean.TRUE.equals(flag) ? "true" : "false";
    }
    
}
