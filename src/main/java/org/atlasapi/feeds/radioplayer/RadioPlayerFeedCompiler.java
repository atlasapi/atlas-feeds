package org.atlasapi.feeds.radioplayer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.content.criteria.operator.Operators;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerItemSorter;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerProgrammeInformationOutputter;
import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerXMLOutputter;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.joda.time.DateTime;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public abstract class RadioPlayerFeedCompiler {
    
    private final RadioPlayerXMLOutputter outputter;
    private final KnownTypeQueryExecutor executor;

    private final RadioPlayerItemSorter sorter = new RadioPlayerItemSorter();
    
    public RadioPlayerFeedCompiler(RadioPlayerXMLOutputter outputter, KnownTypeQueryExecutor executor) {
        this.outputter = outputter;
        this.executor = executor;
    }
    
    private static Map<String, RadioPlayerFeedCompiler> compilerMap;
    
    public static void init(KnownTypeQueryExecutor queryExecutor) {
        compilerMap = ImmutableMap.of(
                "PI",new RadioPlayerProgrammeInformationFeedCompiler(queryExecutor),
                "OD",new RadioPlayerOnDemandFeedCompiler(queryExecutor));
    }
    
    public static RadioPlayerFeedCompiler valueOf(String type) {
        return compilerMap.get(type);
    }
	
    private static class RadioPlayerProgrammeInformationFeedCompiler extends RadioPlayerFeedCompiler {
        public RadioPlayerProgrammeInformationFeedCompiler(KnownTypeQueryExecutor executor) {
            super(new RadioPlayerProgrammeInformationOutputter(), executor);
        }

        @Override
        public ContentQuery queryFor(DateTime date, String serviceUri) {
            Iterable<AtomicQuery> queryAtoms = ImmutableSet.of((AtomicQuery)
                    Attributes.DESCRIPTION_PUBLISHER.createQuery(Operators.EQUALS, ImmutableList.of(Publisher.BBC)),
                    Attributes.BROADCAST_ON.createQuery(Operators.EQUALS, ImmutableList.of(serviceUri)),
                    Attributes.BROADCAST_TRANSMISSION_TIME.createQuery(Operators.AFTER, ImmutableList.of(date)),
                    Attributes.BROADCAST_TRANSMISSION_TIME.createQuery(Operators.BEFORE, ImmutableList.of(date.plusDays(1)))
            );

            return new ContentQuery(queryAtoms);
        }
    }
    
    private static class RadioPlayerOnDemandFeedCompiler extends RadioPlayerFeedCompiler {
        public RadioPlayerOnDemandFeedCompiler(KnownTypeQueryExecutor executor) {
            super(null, executor);
        }

        @Override
        public ContentQuery queryFor(DateTime broadcastOn, String serviceUri) {
            Iterable<AtomicQuery> queryAtoms = ImmutableSet.of((AtomicQuery)
                    Attributes.BROADCAST_ON.createQuery(Operators.EQUALS, ImmutableList.of(serviceUri)),
                    Attributes.LOCATION_AVAILABLE.createQuery(Operators.EQUALS, ImmutableList.of(Boolean.TRUE))
            );
            return new ContentQuery(queryAtoms);
        }
    }

	public abstract ContentQuery queryFor(DateTime date, String serviceUri);
	
	public RadioPlayerXMLOutputter getOutputter(){
		if(outputter == null) {
			throw new UnsupportedOperationException(this.toString() + " feeds are not currently supported");
		}
		return outputter;
	}

	public void compileFeedFor(DateTime date, RadioPlayerService service, OutputStream out) throws IOException {
		if(outputter != null) {
			String serviceUri = service.getServiceUri();
			outputter.output(date, service, sorter.sortAndTransform(executor.schedule(queryFor(date, serviceUri)).getItemsFromOnlyChannel(), serviceUri, date), out);
		}
	}
}
