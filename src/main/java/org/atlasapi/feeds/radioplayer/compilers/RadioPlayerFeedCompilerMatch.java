package org.atlasapi.feeds.radioplayer.compilers;

import java.util.regex.Matcher;

public class RadioPlayerFeedCompilerMatch {

	private final Matcher matcher;
	private final RadioPlayerFeedCompiler compiler;
	
	public RadioPlayerFeedCompilerMatch(Matcher matcher, RadioPlayerFeedCompiler compiler) {
		this.matcher = matcher;
		this.compiler = compiler;
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public RadioPlayerFeedCompiler getCompiler() {
		return compiler;
	}
}
