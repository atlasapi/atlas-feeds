package org.atlasapi.feeds.youview;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class YouviewMetricsController extends io.prometheus.client.exporter.MetricsServlet {

    private YouviewMetricsController(io.prometheus.client.CollectorRegistry registry) {
        super(registry);
    }

    public static YouviewMetricsController create(io.prometheus.client.CollectorRegistry registry) {
        return new YouviewMetricsController(registry);
    }

    @RequestMapping(value = "/system/prometheusMetrics", method = RequestMethod.GET)
    public void getPrometheusMetrics(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException, ServletException {
        doGet(req, resp);
    }
}
