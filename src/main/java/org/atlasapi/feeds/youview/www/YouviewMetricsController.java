package org.atlasapi.feeds.youview.www;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class YouviewMetricsController extends MetricsServlet {

    private YouviewMetricsController(CollectorRegistry registry) {
        super(registry);
    }

    public static YouviewMetricsController create(CollectorRegistry registry) {
        return new YouviewMetricsController(registry);
    }

    @RequestMapping(value = "/system/prometheusMetrics", method = RequestMethod.GET)
    public void getPrometheusMetrics(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        doGet(req, resp);
    }
}
