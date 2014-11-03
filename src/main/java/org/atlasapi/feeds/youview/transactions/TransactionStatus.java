package org.atlasapi.feeds.youview.transactions;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

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
        if (fragmentReports == null) {
            this.fragmentReports = Optional.absent();
        } else {
            this.fragmentReports = Optional.<List<FragmentReportType>>of(
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
}
