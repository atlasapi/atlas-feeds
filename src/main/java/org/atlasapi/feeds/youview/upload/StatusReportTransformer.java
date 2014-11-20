package org.atlasapi.feeds.youview.upload;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.youview.transactions.TransactionStatus;

import tva.mpeg7._2008.TextualType;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public class StatusReportTransformer implements HttpResponseTransformer<TransactionStatus> {
    
    @Override
    public TransactionStatus transform(HttpResponsePrologue prologue, InputStream body)
            throws HttpException, Exception {
        JAXBContext context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StatusReport statusReport = (StatusReport) unmarshaller.unmarshal(body);
        return convertToTransactionStatus(statusReport);
    }

    private TransactionStatus convertToTransactionStatus(
            StatusReport report) {
        // TODO these will throw if there isn't only a single elem. Is this correct?
        TransactionReportType txnReport = Iterables.getOnlyElement(report.getTransactionReport());
        // gets first remark. Have seen both 0 and 1 remarks. TODO will there ever be more than 1 remark?
        List<TextualType> remarks = txnReport.getRemark();
        String message = "";
        if (remarks.size() > 0) {
            message = remarks.get(0).getValue();
        }
        
        // TODO refactor this - can be done better
        // I suspect for deletes the fragment update reports may be null
        Iterable<FragmentReportType> fragmentReports;
        if (txnReport.getFragmentDeleteReport() != null) {
            fragmentReports = Iterables.concat(
                    txnReport.getFragmentUpdateReport(), 
                    Lists.newArrayList(txnReport.getFragmentDeleteReport())
            );
        } else {
            fragmentReports = txnReport.getFragmentUpdateReport();
        }
        
        return new TransactionStatus(txnReport.getState(), message, fragmentReports);
    }
}
