package org.atlasapi.feeds.radioplayer;

import java.io.IOException;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.radioplayer.compilers.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.compilers.RadioPlayerFeedCompilerMatch;
import org.atlasapi.feeds.radioplayer.compilers.RadioPlayerFeedCompilers;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.metabroadcast.common.base.Maybe;

@Controller
public class RadioPlayerController {

	private final KnownTypeQueryExecutor queryExecutor;

	public RadioPlayerController(KnownTypeQueryExecutor queryExecutor) {
		this.queryExecutor = queryExecutor;
	}

	@RequestMapping("feeds/ukradioplayer/{filename}.xml")
	public String xmlForFilename(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {
		
		Maybe<RadioPlayerFeedCompilerMatch> possibleCompiler = RadioPlayerFeedCompilers.findByFilename(filename);
		
		if (possibleCompiler.hasValue()) {
			RadioPlayerFeedCompiler feedCompiler = possibleCompiler.requireValue().getCompiler();
			Matcher matcher = possibleCompiler.requireValue().getMatcher();
			
			feedCompiler.compileFeedFor(matcher, queryExecutor, response.getOutputStream());
			
		}else{
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return null;
		}

		return null;
	}

}
