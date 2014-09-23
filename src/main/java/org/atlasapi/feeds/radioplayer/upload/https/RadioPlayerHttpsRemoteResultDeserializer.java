package org.atlasapi.feeds.radioplayer.upload.https;

import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.FAILURE;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.SUCCESS;
import static org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType.UNKNOWN;

import java.lang.reflect.Type;
import java.util.Map;

import org.atlasapi.feeds.radioplayer.upload.queue.RemoteCheckResult;
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


public class RadioPlayerHttpsRemoteResultDeserializer implements JsonDeserializer<RemoteCheckResult> {

    private static final Map<String, FileUploadResultType> RESULT_TYPE_MAPPING = ImmutableMap.<String, FileUploadResultType>builder()
            .put("accepted", UNKNOWN)
            .put("processed", SUCCESS)
            .put("failed", FAILURE)
            // TODO not sure if this is the correct string
            .put("timedout", FAILURE)
            .build();
    
    @Override
    public RemoteCheckResult deserialize(JsonElement json, Type type,
            JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObj = json.getAsJsonObject();
        return convertToResultType(jsonObj);
    }

    private RemoteCheckResult convertToResultType(JsonObject jsonObj) {
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
                return FAILURE.equals(input);
            }
        }))) {
            return RemoteCheckResult.failure(jsonObj.toString());
        }
        if (!Iterables.isEmpty(Iterables.filter(statuses, new Predicate<FileUploadResultType>() {
            @Override
            public boolean apply(FileUploadResultType input) {
                return UNKNOWN.equals(input);
            }
        }))) {
            return RemoteCheckResult.unknown(jsonObj.toString());
        }

        return RemoteCheckResult.success(jsonObj.toString());
    }

    // TODO deal with unknown result types?
    private FileUploadResultType getTypeFromStatus(JsonObject status) {
        FileUploadResultType result = RESULT_TYPE_MAPPING.get(status.get("status").getAsString());
        return result == null ? UNKNOWN : result; 
    }

}
