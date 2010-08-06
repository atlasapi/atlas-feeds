package org.atlasapi.feeds.interlinking;

import org.atlasapi.feeds.interlinking.www.InterlinkController;
import org.atlasapi.persistence.content.ContentResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InterlinkingModule {

	private @Autowired ContentResolver resolver;

	public @Bean InterlinkController feedController() {
		return new InterlinkController(resolver);
	}
}
