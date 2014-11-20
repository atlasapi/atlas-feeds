package org.atlasapi.feeds.youview.transactions;

import static org.atlasapi.feeds.youview.transactions.persistence.TransactionStatusTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.persistence.TransactionStatusTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import tva.mpeg7._2008.TextualType;

import com.google.common.base.Equivalence;
import com.google.common.collect.ImmutableSet;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.SeverityType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.TransactionStateType;


public class TransactionStatusTranslatorTest {
    
    @Test
    public void testTranslationToAndFromDBObjectWhenStatusHasNoReports() {
        
        TransactionStatus status = new TransactionStatus(TransactionStateType.ACCEPTED, "a message");
        
        TransactionStatus translated = fromDBObject(toDBObject(status));
        
        assertEquals(status.status(), translated.status());
        assertEquals(status.message(), translated.message());
        assertEquals(status.fragmentReports(), translated.fragmentReports());
    }
    
    @Test
    public void testTranslationToAndFromDBObjectWhenStatusHasFragmentReports() {
        
        Iterable<FragmentReportType> fragmentReports = ImmutableSet.of(createFragmentReport());
        TransactionStatus status = new TransactionStatus(TransactionStateType.FAILED, "your XML sucks", fragmentReports);
        
        TransactionStatus translated = fromDBObject(toDBObject(status));
        
        assertEquals(status.status(), translated.status());
        assertEquals(status.message(), translated.message());
        
        List<FragmentReportType> reports = status.fragmentReports().get();
        List<FragmentReportType> translatedReports = translated.fragmentReports().get();
        
        Equivalence.equals().equivalent(reports, translatedReports);
    }

    private FragmentReportType createFragmentReport() {
        FragmentReportType report = new FragmentReportType();
        
        report.setSuccess(false);
        report.setFragmentId("crid://bbc.co.uk/987654321");
        TextualType remark = new TextualType();
        remark.setValue("Unknown fragment");
        report.getRemark().add(remark);
        ControlledMessageType message = new ControlledMessageType();
        message.setReasonCode("http://refdata.youview.com/mpeg7cs/YouViewMetadataIngestStatusCS/2010-￼09-23#transactional");
        message.setSeverity(SeverityType.ERROR);
        message.setLocation("/TVAMain/ProgramDescription/ProgramInformationTable");
        TextualType comment = new TextualType();
        comment.setValue("Unknown fragment.");
        message.setComment(comment);
        report.getMessage().add(message);

        return report;
    }
}
