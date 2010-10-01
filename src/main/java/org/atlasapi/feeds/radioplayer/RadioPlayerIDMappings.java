package org.atlasapi.feeds.radioplayer;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class RadioPlayerIDMappings {

	public static Map<String,RadioPlayerServiceIdentifier> byServiceId() {
		Builder<String,RadioPlayerServiceIdentifier> playerIdToBroadcastOnMapBuilder = ImmutableMap.builder();
		
		playerIdToBroadcastOnMapBuilder.put("e1_ce15_c222_0", new RadioPlayerServiceIdentifier(502, "radio2", "http://www.bbc.co.uk/services/radio2","e1.ce15.c222.0"));
		
		return playerIdToBroadcastOnMapBuilder.build();
	}
	
	public static Map<String, RadioPlayerServiceIdentifier> byServiceId = byServiceId();
	
	
	public static Map<String,RadioPlayerServiceIdentifier> byRadioplayerId() {
		Builder<String,RadioPlayerServiceIdentifier> playerIdToBroadcastOnMapBuilder = ImmutableMap.builder();
		
		playerIdToBroadcastOnMapBuilder.put("502", new RadioPlayerServiceIdentifier(502, "radio2", "http://www.bbc.co.uk/services/radio2","e1.ce15.c222.0"));
		
		return playerIdToBroadcastOnMapBuilder.build();
	}
	
	public static Map<String, RadioPlayerServiceIdentifier> byRadioplayerId = byRadioplayerId();
	
	
	public static Map<String, RadioPlayerServiceIdentifier> all() {
		Builder<String,RadioPlayerServiceIdentifier> playerIdToBroadcastOnMapBuilder = ImmutableMap.builder();
		
		playerIdToBroadcastOnMapBuilder.putAll(byServiceId).putAll(byRadioplayerId);
		
		return playerIdToBroadcastOnMapBuilder.build();
	}
	
	public static Map<String, RadioPlayerServiceIdentifier> all = all();
	
}
