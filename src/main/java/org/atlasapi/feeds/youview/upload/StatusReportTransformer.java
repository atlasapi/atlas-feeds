package org.atlasapi.feeds.youview.upload;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.metabroadcast.common.http.HttpException;
import com.metabroadcast.common.http.HttpResponsePrologue;
import com.metabroadcast.common.http.HttpResponseTransformer;
import com.youview.refdata.schemas.youviewstatusreport._2010_12_07.StatusReport;


public class StatusReportTransformer implements HttpResponseTransformer<StatusReport> {
    
    @Override
    public StatusReport transform(HttpResponsePrologue prologue, InputStream body)
            throws HttpException, Exception {
        JAXBContext context = JAXBContext.newInstance("com.youview.refdata.schemas.youviewstatusreport._2010_12_07");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (StatusReport) unmarshaller.unmarshal(body);
    }
}
