package org.atlasapi.feeds.interlinking.www;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.feeds.interlinking.DelegatingPlaylistToInterlinkAdapter;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeedAdapter;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.metabroadcast.common.media.MimeType;

@Controller
public class InterlinkController {

	private final ContentResolver resolver;
	private final InterlinkFeedOutputter outputter = new InterlinkFeedOutputter();
	private final PlaylistToInterlinkFeed adapter;

	public InterlinkController(ContentResolver resolver, Map<Publisher, PlaylistToInterlinkFeed> delegates) {
		this.resolver = resolver;
		this.adapter = new DelegatingPlaylistToInterlinkAdapter(delegates, new PlaylistToInterlinkFeedAdapter());
	}
	
	@RequestMapping("/feeds/bbc-interlinking")
	public void showFeed(HttpServletResponse response, @RequestParam String uri) throws IOException {
		response.setContentType(MimeType.APPLICATION_ATOM_XML.toString());
		response.setStatus(HttpServletResponse.SC_OK);
		outputter.output(adapter.fromPlaylist((Playlist) resolver.findByUri(uri)), response.getOutputStream());
	}
}
