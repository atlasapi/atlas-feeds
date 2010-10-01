package org.atlasapi.feeds.radioplayer;

import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RadioPlayerModule {

	private @Autowired @Qualifier("mongoDbQueryExcutorThatFiltersUriQueries") KnownTypeQueryExecutor queryExecutor;

	public @Bean RadioPlayerController radioPlayerController() {
		return new RadioPlayerController(queryExecutor);
	}
	
}
