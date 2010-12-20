package org.atlasapi.feeds.radioplayer;

import java.io.IOException;
import java.io.OutputStream;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerProgrammeInformationOutputter;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerXMLOutputter;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public enum RadioPlayerFeedType {
	PI(new RadioPlayerProgrammeInformationOutputter()) {
		@Override
		public ContentQuery queryFor(DateTime date, String serviceUri) {
			Iterable<AtomicQuery> queryAtoms = ImmutableSet.of((AtomicQuery)
					Attributes.BROADCAST_ON.createQuery(Operators.EQUALS, ImmutableList.of(serviceUri)),
					Attributes.BROADCAST_TRANSMISSION_TIME.createQuery(Operators.AFTER, ImmutableList.of(date)),
					Attributes.BROADCAST_TRANSMISSION_TIME.createQuery(Operators.BEFORE, ImmutableList.of(date.plusDays(1)))
			);

			return new ContentQuery(queryAtoms);
		}
	},
	SI(null) {
		@Override
		public ContentQuery queryFor(DateTime date, String serviceUri) {
			return null;
		}
	},
	OD(null) {
		@Override
		public ContentQuery queryFor(DateTime broadcastOn, String serviceUri) {
			Iterable<AtomicQuery> queryAtoms = ImmutableSet.of((AtomicQuery)
					Attributes.BROADCAST_ON.createQuery(Operators.EQUALS, ImmutableList.of(serviceUri)),
					Attributes.LOCATION_AVAILABLE.createQuery(Operators.EQUALS, ImmutableList.of(Boolean.TRUE))
			);
			
			return new ContentQuery(queryAtoms);
		}
	};

	private final RadioPlayerXMLOutputter outputter;

	RadioPlayerFeedType(RadioPlayerXMLOutputter outputter) {
		this.outputter = outputter;
	}

	public abstract ContentQuery queryFor(DateTime date, String serviceUri);
	
	public RadioPlayerXMLOutputter getOutputter(){
		if(outputter == null) {
			throw new UnsupportedOperationException();
		}
		return outputter;
	}

	public void compileFeedFor(DateTime date, RadioPlayerService service, KnownTypeQueryExecutor queryExecutor, OutputStream out) throws IOException {
		if(outputter != null) {
			outputter.output(date, service, queryExecutor.executeItemQuery(queryFor(date, service.getServiceUri())), out);
		}
	}
}
