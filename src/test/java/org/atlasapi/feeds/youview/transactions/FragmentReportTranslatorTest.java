package org.atlasapi.feeds.youview.transactions;

import static org.atlasapi.feeds.youview.transactions.persistence.FragmentReportTranslator.fromDBObject;
import static org.atlasapi.feeds.youview.transactions.persistence.FragmentReportTranslator.toDBObject;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import tva.mpeg7._2008.TextualType;

import com.google.common.collect.Iterables;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.ControlledMessageType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.FragmentReportType;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.SeverityType;


public class FragmentReportTranslatorTest {
    
    @Test
    public void testTranslationToAndFromDBObject() {
        FragmentReportType report = createFragmentReport();
        
        FragmentReportType translated = fromDBObject(toDBObject(report));
        
        assertEquals(report.getFragmentId(), translated.getFragmentId());
        assertEquals(report.getRecordId(), translated.getRecordId());
        assertEquals(report.isSuccess(), translated.isSuccess());
        
        TextualType remark = Iterables.getOnlyElement(report.getRemark());
        TextualType translatedRemark = Iterables.getOnlyElement(translated.getRemark());
        assertEquals(remark.getValue(), translatedRemark.getValue());
        
        ControlledMessageType message = Iterables.getOnlyElement(report.getMessage());
        ControlledMessageType translatedMessage = Iterables.getOnlyElement(translated.getMessage());
        assertEquals(message.getComment().getValue(), translatedMessage.getComment().getValue());
        assertEquals(message.getReasonCode(), translatedMessage.getReasonCode());
        assertEquals(message.getSeverity(), translatedMessage.getSeverity());
        assertEquals(message.getLocation(), translatedMessage.getLocation());
    }

    private FragmentReportType createFragmentReport() {
        FragmentReportType report = new FragmentReportType();
        
        report.setSuccess(false);
        report.setFragmentId("crid://bbc.co.uk/987654321");
        report.setFragmentId("67390552-￼0a89-447c-a216-3b9d4507b3dc");
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
