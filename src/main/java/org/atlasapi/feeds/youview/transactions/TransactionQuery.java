package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.metabroadcast.common.query.Selection;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class TransactionQuery {

    private final Selection selection;
    private final Publisher publisher;
    private final Optional<String> contentUri;
    private final Optional<String> transactionId;
    private final Optional<TransactionStateType> transactionStatus;
    
    public static Builder builder(Selection selection, Publisher publisher) {
        return new Builder(selection, publisher);
    }
    
    public TransactionQuery(Selection selection, Publisher publisher, Optional<String> contentUri, 
            Optional<String> transactionId, Optional<TransactionStateType> transactionStatus) {
        this.selection = checkNotNull(selection);
        this.publisher = checkNotNull(publisher);
        this.contentUri = checkNotNull(contentUri);
        this.transactionId = checkNotNull(transactionId);
        this.transactionStatus = checkNotNull(transactionStatus);
    }
    
    public Selection selection() {
        return selection;
    }
    
    public Publisher publisher() {
        return publisher;
    }
    
    public Optional<String> contentUri() {
        return contentUri;
    }
    
    public Optional<String> transactionId() {
        return transactionId;
    }
    
    public Optional<TransactionStateType> transactionStatus() {
        return transactionStatus;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(TransactionQuery.class)
                .add("selection", selection)
                .add("publisher", publisher)
                .add("contentUri", contentUri)
                .add("transactionId", transactionId)
                .add("transactionStatus", transactionStatus)
                .toString();
    }
    
    public static final class Builder {
        
        private final Selection selection;
        private final Publisher publisher;
        private Optional<String> contentUri = Optional.absent();
        private Optional<String> transactionId = Optional.absent();
        private Optional<TransactionStateType> transactionStatus = Optional.absent();

        private Builder(Selection selection, Publisher publisher) {
            this.selection = selection;
            this.publisher = publisher;
        }
        
        public TransactionQuery build() {
            return new TransactionQuery(selection, publisher, contentUri, transactionId, transactionStatus);
        }
        
        public Builder withContentUri(String contentUri) {
            this.contentUri = Optional.fromNullable(contentUri);
            return this;
        }
        
        public Builder withTransactionId(String transactionId) {
            this.transactionId = Optional.fromNullable(transactionId);
            return this;
        }
        
        public Builder withTransactionStatus(TransactionStateType transactionStatus) {
            this.transactionStatus = Optional.fromNullable(transactionStatus);
            return this;
        }
    }
}
