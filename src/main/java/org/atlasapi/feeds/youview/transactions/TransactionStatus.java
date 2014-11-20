package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public final class TransactionStatus {

    private final TransactionStateType status;
    private final String message;
    private final Optional<List<FragmentReportType>> fragmentReports;
    
    public TransactionStatus(TransactionStateType status, String message) {
        this(status, message, null);
    }
    
    public TransactionStatus(TransactionStateType status, String message,
            Iterable<FragmentReportType> fragmentReports) {
        this.status = checkNotNull(status);
        this.message = checkNotNull(message);
        this.fragmentReports = fromNullable(fragmentReports);
    }

    private static Optional<List<FragmentReportType>> fromNullable(
            Iterable<FragmentReportType> fragmentReports) {
        
        if (fragmentReports == null) {
            return Optional.absent();
        } else {
            return Optional.<List<FragmentReportType>>of(
                    ImmutableList.copyOf(fragmentReports)
            );
        }
    }
    
    public TransactionStateType status() {
        return status;
    }
    
    public String message() {
        return message;
    }
    
    public Optional<List<FragmentReportType>> fragmentReports() {
        return fragmentReports;
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(TransactionStatus.class)
                .add("status", status)
                .add("message", message)
                .add("fragmentReports", fragmentReports)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(status, message, fragmentReports);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof TransactionStatus) {
            TransactionStatus other = (TransactionStatus) that;
            return status.equals(other.status)
                    && message.equals(other.message)
                    && fragmentReports.equals(other.fragmentReports);
        }
        
        return false;
    }
}
