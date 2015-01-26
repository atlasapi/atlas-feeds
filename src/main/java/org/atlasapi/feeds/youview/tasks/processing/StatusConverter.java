package org.atlasapi.feeds.youview.tasks.processing;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.youview.payload.Converter;
import org.atlasapi.feeds.youview.tasks.Status;

import com.google.common.base.Throwables;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionReportType;


public class StatusConverter implements Converter<String, Status> {

    @Override
    public Status convert(String input) {
        try {
            JAXBContext context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StatusReport report = (StatusReport) unmarshaller.unmarshal(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            TransactionReportType txnReport = report.getTransactionReport().get(0);

            switch (txnReport.getState()) {
            case ACCEPTED:
                return Status.ACCEPTED;
            case COMMITTED:
                return Status.COMMITTED;
            case COMMITTING:
                return Status.COMMITTING;
            case FAILED:
                return Status.FAILED;
            case PUBLISHED:
                return Status.PUBLISHED;
            case PUBLISHING:
                return Status.PUBLISHING;
            case QUARANTINED:
                return Status.QUARANTINED;
            case VALIDATING:
                return Status.VALIDATING;
            default:
                return Status.UNKNOWN;
            }
        } catch (JAXBException e) {
            throw Throwables.propagate(e);
        }
    }
}
