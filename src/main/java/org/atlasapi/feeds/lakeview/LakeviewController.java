package org.atlasapi.feeds.lakeview;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.entity.Publisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LakeviewController {
    
    private final XmlFeedOutputter feedOutputter;
    private final LakeviewContentFetcher contentFetcher;
    private final LakeviewFeedCompiler feedCompiler;
    
    public LakeviewController(LakeviewContentFetcher contentFetcher, LakeviewFeedCompiler feedCompiler, XmlFeedOutputter feedOutputter) {
        this.contentFetcher = contentFetcher;
        this.feedCompiler = feedCompiler;
        this.feedOutputter = feedOutputter;
    }

    @RequestMapping("/feeds/lakeview/c4.xml")
    public void producateOutput(HttpServletResponse response) throws IOException {
        
        feedOutputter.outputTo(feedCompiler.compile(contentFetcher.fetchContent(Publisher.C4_PMLSD_P06)), response.getOutputStream());
        
    }
    
}
