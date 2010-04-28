/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.beans;

import java.io.ByteArrayOutputStream;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.uriplay.beans.OembedTranslator.OutputFactory;
import org.uriplay.feeds.OembedOutput;
import org.uriplay.media.entity.Encoding;
import org.uriplay.media.entity.Item;
import org.uriplay.media.entity.Location;
import org.uriplay.media.entity.Version;

import com.google.common.collect.Sets;

/**
 * Unit test for {@link OembedTranslator}.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class OembedTranslatorTest extends MockObjectTestCase {
	
	Set<Object> graph = Sets.<Object>newHashSet();
	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	
	OutputFactory outputFactory = mock(OutputFactory.class);
	OembedOutput oembedOutput = mock(OembedOutput.class);
	
	public void testCreatesFeed() throws Exception {
		
		checking(new Expectations() {{ 
			one(outputFactory).createOutput(); 
		}});
		
		new OembedTranslator(outputFactory).writeTo(graph, outputStream);
	}
	
	public void testWritesFeedToStream() throws Exception {
		
		checking(new Expectations() {{ 
			allowing(outputFactory).createOutput(); will(returnValue(oembedOutput));
			one(oembedOutput).writeTo(outputStream);
		}});
		
		new OembedTranslator(outputFactory).writeTo(graph, outputStream);
	}
	
	public void testSetsOembedFieldsFromItemData() throws Exception {
		
		Item item = new Item();
		item.setTitle("Test Title");
		item.setPublisher("youtube.com");
		item.setCanonicalUri("http://example.com");
		
		Version version = new Version();
		item.addVersion(version);
		
		Encoding encoding = new Encoding();
		encoding.setVideoHorizontalSize(640);
		encoding.setVideoVerticalSize(480);
		version.addManifestedAs(encoding);
		
		Location location = new Location();
		location.setEmbedCode("<embed src=\"a\" />");
		encoding.addAvailableAt(location);
		
		graph.add(item);
		
		checking(new Expectations() {{ 
			allowing(outputFactory).createOutput(); will(returnValue(oembedOutput));
			allowing(oembedOutput).writeTo(outputStream);
	
			one(oembedOutput).setTitle("Test Title");
			one(oembedOutput).setProviderUrl("youtube.com");
			one(oembedOutput).setWidth(640);
			one(oembedOutput).setHeight(480);
			one(oembedOutput).setType("video");
			one(oembedOutput).setEmbedCode("<embed src=\\\"a\\\" />");
		}});
		
		new OembedTranslator(outputFactory).writeTo(graph, outputStream);
	}

}
