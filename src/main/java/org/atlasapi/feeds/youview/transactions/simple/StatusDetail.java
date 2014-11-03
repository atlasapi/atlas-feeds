package org.atlasapi.feeds.youview.transactions.simple;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class StatusDetail {

    private TransactionStateType status;
    private String message;
    private List<FragmentReportType> fragmentReports;
    
    public StatusDetail() {
    }
    
    public TransactionStateType status() {
        return status;
    }
    
    public void setStatus(TransactionStateType status) {
        this.status = status;
    }

    public String message() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public List<FragmentReportType> fragmentReports() {
        return fragmentReports;
    }
    
    public void setFragmentReports(Iterable<FragmentReportType> fragmentReports) {
        this.fragmentReports = ImmutableList.copyOf(fragmentReports);
    }
}
