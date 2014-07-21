package org.atlasapi.feeds.radioplayer.upload;

public enum FileType {
    PI,
    OD,
    SI;
    
    public static FileType fromString(String fileType) {
        for (FileType typeOption : FileType.values()) {
            if (typeOption.name().equals(fileType)) {
                return typeOption;
            }
        }
        return null;
    }
}
