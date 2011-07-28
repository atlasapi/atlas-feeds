package org.atlasapi.feeds.lakeview;

import java.util.Collection;
import java.util.Map;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Series;

import com.google.common.base.Objects;

public class LakeviewContentGroup {

    private final Brand brand;
    private final Map<Series, Collection<Episode>> contents;

    public LakeviewContentGroup(Brand brand, Map<Series, Collection<Episode>> contents) {
        this.brand = brand;
        this.contents = contents;
    }

    public Brand brand() {
        return brand;
    }

    public Map<Series, Collection<Episode>> contents() {
        return contents;
    }
    
    @Override
    public boolean equals(Object that) {
        if(this == that) {
            return true;
        }
        if (that instanceof LakeviewContentGroup) {
            LakeviewContentGroup other = (LakeviewContentGroup) that;
            return brand.equals(other.brand) && contents.equals(other.contents);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(brand, contents);
    }
    
}
