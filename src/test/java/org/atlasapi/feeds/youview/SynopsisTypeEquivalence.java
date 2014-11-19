package org.atlasapi.feeds.youview;

import tva.metadata._2010.SynopsisType;

import com.google.common.base.Equivalence;

public class SynopsisTypeEquivalence extends Equivalence<SynopsisType> {

    @Override
    protected boolean doEquivalent(SynopsisType a, SynopsisType b) {
        return a.getValue().equals(b.getValue())
            && a.getLength().equals(b.getLength());
    }

    @Override
    protected int doHash(SynopsisType t) {
        return 0;
    }
}