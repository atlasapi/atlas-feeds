package org.atlasapi.feeds.youview.transactions.simple;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.atlasapi.media.vocabulary.PLAY_SIMPLE_XML;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

@XmlRootElement(namespace=PLAY_SIMPLE_XML.NS, name="transactions")
@XmlType(name="transactions", namespace=PLAY_SIMPLE_XML.NS)
public class TransactionQueryResult {
    
    private List<Transaction> transactions = Lists.newArrayList();

    public void add(Transaction transaction) {
        transactions.add(transaction);
    }

    @XmlElements({ 
        @XmlElement(name = "transaction", type = Transaction.class, namespace=PLAY_SIMPLE_XML.NS)
    })
    public List<Transaction> getTransactions() {
        return transactions;
    }
    
    public void setTransactions(Iterable<Transaction> transactions) {
        this.transactions = Lists.newArrayList(transactions);
    }
    
    public boolean isEmpty() {
        return transactions.isEmpty();
    }

    @Override
    public int hashCode() {
        return transactions.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (this instanceof TransactionQueryResult) {
            TransactionQueryResult other = (TransactionQueryResult) obj;
            return transactions.equals(other.transactions);
        }
        return false;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .addValue(transactions)
                .toString();
    }
}
