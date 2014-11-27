package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.base.Optional;


public class BbcGenreTree {

    private final String topLevelGenre;
    private final Optional<String> secondLevelGenre;
    private final Optional<String> thirdLevelGenre;
    
    public static Builder builder(String topLevelGenre) {
        return new Builder(topLevelGenre);
    }
    
    private BbcGenreTree(String topLevelGenre, Optional<String> secondLevelGenre,
            Optional<String> thirdLevelGenre) {
        this.topLevelGenre = checkNotNull(topLevelGenre);
        this.secondLevelGenre = checkNotNull(secondLevelGenre);
        this.thirdLevelGenre = checkNotNull(thirdLevelGenre);
    }

    public String topLevelGenre() {
        return topLevelGenre;
    }
    
    public Optional<String> secondLevelGenre() {
        return secondLevelGenre;
    }
    
    public Optional<String> thirdLevelGenre() {
        return thirdLevelGenre;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(topLevelGenre, secondLevelGenre, thirdLevelGenre);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(BbcGenreTree.class)
                .add("topLevelGenre", topLevelGenre)
                .add("secondLevelGenre", secondLevelGenre)
                .add("thirdLevelGenre", thirdLevelGenre)
                .toString();
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (!(that instanceof BbcGenreTree)) {
            return false;
        }
        
        BbcGenreTree other = (BbcGenreTree) that;
        return topLevelGenre.equals(other.topLevelGenre)
                && secondLevelGenre.equals(other.secondLevelGenre)
                && thirdLevelGenre.equals(other.thirdLevelGenre);
    }

    public static class Builder {
        
        private final String topLevelGenre;
        private Optional<String> secondLevelGenre = Optional.absent();
        private Optional<String> thirdLevelGenre = Optional.absent();
        
        private Builder(String topLevelGenre) {
            this.topLevelGenre = topLevelGenre;
        }

        public BbcGenreTree build() {
            return new BbcGenreTree(topLevelGenre, secondLevelGenre, thirdLevelGenre);
        }

        public Builder withSecondLevelGenre(String secondLevelGenre) {
            this.secondLevelGenre = Optional.fromNullable(secondLevelGenre);
            return this;
        }
        
        public Builder withThirdLevelGenre(String thirdLevelGenre) {
            this.thirdLevelGenre = Optional.fromNullable(thirdLevelGenre);
            return this;
        }
    }
}
