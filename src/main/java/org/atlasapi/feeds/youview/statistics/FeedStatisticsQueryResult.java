package org.atlasapi.feeds.youview.statistics;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.atlasapi.feeds.youview.statistics.simple.FeedStatistics;
import org.atlasapi.media.vocabulary.PLAY_SIMPLE_XML;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

@XmlRootElement(namespace=PLAY_SIMPLE_XML.NS, name="statistics")
@XmlType(name="statistics", namespace=PLAY_SIMPLE_XML.NS)
public class FeedStatisticsQueryResult {
    
    private List<FeedStatistics> feedStats = Lists.newArrayList();

    public void add(FeedStatistics feedStats) {
        this.feedStats.add(feedStats);
    }

    @XmlElements({ 
        @XmlElement(name = "statistics", type = FeedStatistics.class, namespace=PLAY_SIMPLE_XML.NS)
    })
    public List<FeedStatistics> getFeedStatistics() {
        return feedStats;
    }
    
    public void setTransactions(Iterable<FeedStatistics> feedStats) {
        this.feedStats = Lists.newArrayList(feedStats);
    }
    
    public boolean isEmpty() {
        return feedStats.isEmpty();
    }

    @Override
    public int hashCode() {
        return feedStats.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this instanceof FeedStatisticsQueryResult) {
            FeedStatisticsQueryResult other = (FeedStatisticsQueryResult) obj;
            return feedStats.equals(other.feedStats);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(feedStats)
                .toString();
    }
}
