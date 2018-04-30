package org.atlasapi.feeds.youview.persistence;

import java.util.Optional;

import com.mongodb.WriteResult;

public interface YouViewPayloadHashStore {

    void saveHash(HashType payloadType, String imi, String hash);

    WriteResult removeHash(HashType type, String elementId);

    Optional<String> getHash(HashType payloadType, String imi);
}
