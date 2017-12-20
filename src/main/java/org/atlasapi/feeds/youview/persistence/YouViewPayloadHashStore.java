package org.atlasapi.feeds.youview.persistence;

import java.util.Optional;

public interface YouViewPayloadHashStore {

    void saveHash(HashType payloadType, String imi, String hash);

    Optional<String> getHash(HashType payloadType, String imi);
}
