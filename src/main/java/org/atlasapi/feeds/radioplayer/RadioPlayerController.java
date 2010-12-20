package org.atlasapi.feeds.radioplayer;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;

@Controller
public class RadioPlayerController {

	private final KnownTypeQueryExecutor queryExecutor;

	public RadioPlayerController(KnownTypeQueryExecutor queryExecutor) {
		this.queryExecutor = queryExecutor;
	}

	@RequestMapping("feeds/ukradioplayer/{filename}.xml")
	public String xmlForFilename(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {

		RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);

		if (matcher.matches() && Iterables.all(ImmutableSet.of(matcher.date(), matcher.service(), matcher.type()), Maybe.HAS_VALUE)) {

			RadioPlayerFeedType feedType = matcher.type().requireValue();
			
			feedType.compileFeedFor(matcher.date().requireValue(), matcher.service().requireValue(), queryExecutor, response.getOutputStream());

		} else {
			return notFound(response);
		}

		return null;
	}

	private String notFound(HttpServletResponse response) {
		response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		response.setContentLength(0);
		return null;
	}

}
