package org.atlasapi.feeds.radioplayer.upload;

import java.lang.reflect.Type;
import java.util.Map;

import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;


public class RadioPlayerHttpsRemoteResultDeserializer implements JsonDeserializer<RadioPlayerHttpsRemoteResult> {

    private static final Map<String, FileUploadResultType> RESULT_TYPE_MAPPING = ImmutableMap.<String, FileUploadResultType>builder()
            .put("accepted", FileUploadResultType.UNKNOWN)
            .put("processed", FileUploadResultType.SUCCESS)
            .put("failed", FileUploadResultType.FAILURE)
            // TODO not sure if this is the correct string
            .put("timedout", FileUploadResultType.FAILURE)
            .build();
    
    @Override
    public RadioPlayerHttpsRemoteResult deserialize(JsonElement json, Type type,
            JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObj = json.getAsJsonObject();
        return new RadioPlayerHttpsRemoteResult(convertToResultType(jsonObj), jsonObj.toString());
    }

    private FileUploadResultType convertToResultType(JsonObject jsonObj) {
        JsonArray statusJson = jsonObj.get("downstreamStatuses").getAsJsonArray();
        Iterable<FileUploadResultType> statuses = Iterables.transform(statusJson, new Function<JsonElement, FileUploadResultType>() {
            @Override
            public FileUploadResultType apply(JsonElement input) {
                return getTypeFromStatus(input.getAsJsonObject());
            }
        });
        
        if (!Iterables.isEmpty(Iterables.filter(statuses, new Predicate<FileUploadResultType>() {
            @Override
            public boolean apply(FileUploadResultType input) {
                return FileUploadResultType.FAILURE.equals(input);
            }
        }))) {
            return FileUploadResultType.FAILURE;
        }
        if (!Iterables.isEmpty(Iterables.filter(statuses, new Predicate<FileUploadResultType>() {
            @Override
            public boolean apply(FileUploadResultType input) {
                return FileUploadResultType.UNKNOWN.equals(input);
            }
        }))) {
            return FileUploadResultType.UNKNOWN;
        }

        return FileUploadResultType.SUCCESS;
    }

    // TODO deal with unknown result types?
    private FileUploadResultType getTypeFromStatus(JsonObject status) {
        return RESULT_TYPE_MAPPING.get(status.get("status").getAsString());
    }

}
