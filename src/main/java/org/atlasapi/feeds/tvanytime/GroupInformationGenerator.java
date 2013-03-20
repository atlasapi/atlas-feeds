package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Series;

import tva.metadata._2010.GroupInformationType;

public interface GroupInformationGenerator {
    public GroupInformationType generate(Film film);
    public GroupInformationType generate(Episode episode);
    public GroupInformationType generate(Series series, Episode episode);
    public GroupInformationType generate(Brand brand, Episode episode);
}
