package org.atlasapi.feeds.radioplayer.compilers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Matcher;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.feeds.radioplayer.RadioPlayerIDMappings;
import org.atlasapi.feeds.radioplayer.RadioPlayerServiceIdentifier;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerXMLOutputter;
import org.atlasapi.media.entity.Item;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class RadioPlayerProgrammeInformationFeedCompiler extends RadioPlayerFeedCompiler {

	public RadioPlayerProgrammeInformationFeedCompiler(String filenamePattern, RadioPlayerXMLOutputter outputter){
		super(filenamePattern, outputter);
	}
	
	public ContentQuery queryFor(DateTime day, String broadcastOn) {
		Iterable<AtomicQuery> queryAtoms = ImmutableSet.of((AtomicQuery)
				Attributes.BROADCAST_ON.createQuery(Operators.EQUALS, ImmutableList.of(broadcastOn)),
				Attributes.BROADCAST_TRANSMISSION_TIME.createQuery(Operators.AFTER, ImmutableList.of(day)),
				Attributes.BROADCAST_TRANSMISSION_TIME.createQuery(Operators.BEFORE, ImmutableList.of(day.plusDays(1)))
		);
		
		return new ContentQuery(queryAtoms);
	}
	
	@Override
	public void compileFeedFor(Matcher matcher, KnownTypeQueryExecutor queryExecutor, OutputStream out) throws IOException {
		DateTime day = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZone.UTC).parseDateTime(matcher.group(1));
		
		String stationId  = matcher.group(2);
		RadioPlayerServiceIdentifier identifier = RadioPlayerIDMappings.all.get(stationId);
		
		List<Item> items = queryExecutor.executeItemQuery(queryFor(day, identifier.getBroadcastUri()));
		outputter.output(day, identifier, items, out);
	}
	
}
