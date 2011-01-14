package org.atlasapi.feeds.radioplayer;

import static com.metabroadcast.common.base.Maybe.HAS_VALUE;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

@Controller
public class RadioPlayerController {

	private final KnownTypeQueryExecutor queryExecutor;

	public RadioPlayerController(KnownTypeQueryExecutor queryExecutor) {
		this.queryExecutor = queryExecutor;
	}

	@RequestMapping("feeds/ukradioplayer/{filename}.xml")
	public void xmlForFilename(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {

		RadioPlayerFilenameMatcher matcher = RadioPlayerFilenameMatcher.on(filename);

		if (matcher.matches() && Iterables.all(ImmutableSet.of(matcher.date(), matcher.service(), matcher.type()), HAS_VALUE)) {

			RadioPlayerFeedType feedType = matcher.type().requireValue();
			try {
				feedType.compileFeedFor(matcher.date().requireValue(), matcher.service().requireValue(), queryExecutor, response.getOutputStream());
			} catch (Exception e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}

		} else {
			if (matcher.service().isNothing()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unkown Service");
			} else {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unrecognised filename pattern");
			}

		}
	}

}
