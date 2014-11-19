package org.atlasapi.feeds.youview;

import tva.mpeg7._2008.NameComponentType;

import com.google.common.base.Equivalence;

public class NameComponentTypeEquivalence extends Equivalence<NameComponentType> {

    @Override
    protected boolean doEquivalent(NameComponentType a, NameComponentType b) {
        return a.getValue().equals(b.getValue());
    }

    @Override
    protected int doHash(NameComponentType t) {
        return 0;
    }
}