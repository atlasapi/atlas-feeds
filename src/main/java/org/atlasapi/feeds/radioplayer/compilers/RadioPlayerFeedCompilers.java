package org.atlasapi.feeds.radioplayer.compilers;

import java.util.List;
import java.util.regex.Matcher;

import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerProgrammeInformationOutputter;

import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.base.Maybe;

public class RadioPlayerFeedCompilers {
	
	public static List<RadioPlayerProgrammeInformationFeedCompiler> compilers = ImmutableList.of(
		new RadioPlayerProgrammeInformationFeedCompiler("([0-9]{8})_([0-9A-Za-z\\_]+)_PI", new RadioPlayerProgrammeInformationOutputter())/*,
		new RadioPlayerProgrammeInformationFeedCompiler("([0-9]{8})_([0-9A-Za-z\\_]+)_OD", new RadioPlayerProgrammeInformationOutputter())*/
	);

	public static Maybe<RadioPlayerFeedCompilerMatch> findByFilename(String filename) {
		for(RadioPlayerFeedCompiler compiler : compilers){
			Matcher matcher = compiler.getPattern().matcher(filename);
			if (matcher.matches()) {
				return Maybe.just(new RadioPlayerFeedCompilerMatch(matcher, compiler));
			}
		}
		return Maybe.nothing();
	}
	
}
