package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import tva.metadata._2010.GroupInformationType;

import com.google.common.base.Optional;

public interface GroupInformationGenerator {
    GroupInformationType generate(Film film);
    GroupInformationType generate(Episode episode, Optional<Series> series, Optional<Brand> brand);
    GroupInformationType generate(Series series, Optional<Brand> brand, Item firstChild);
    GroupInformationType generate(Brand brand, Item firstChild);
}
