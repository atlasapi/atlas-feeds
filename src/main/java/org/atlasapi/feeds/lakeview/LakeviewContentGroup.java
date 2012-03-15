package org.atlasapi.feeds.lakeview;

import java.util.List;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Series;

import com.google.common.base.Objects;

public class LakeviewContentGroup {

    private final Brand brand;
    private final List<Series> series;
    private final List<Episode> episodes;

    public LakeviewContentGroup(Brand brand, List<Series> series, List<Episode> episodes) {
        this.brand = brand;
        this.series = series;
        this.episodes = episodes;
    }

    public Brand brand() {
        return brand;
    }
    
    public boolean isFlattened() {
        return series.isEmpty();
    }

    public List<Series> series() {
        return series;
    }

    public List<Episode> episodes() {
        return episodes;
    }
    
    @Override
    public boolean equals(Object that) {
        if(this == that) {
            return true;
        }
        if (that instanceof LakeviewContentGroup) {
            LakeviewContentGroup other = (LakeviewContentGroup) that;
            return brand.equals(other.brand);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(brand);
    }

    @Override
    public String toString() {
        return brand.toString();
    }
}
