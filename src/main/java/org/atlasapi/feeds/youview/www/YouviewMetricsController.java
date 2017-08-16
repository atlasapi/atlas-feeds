package org.atlasapi.feeds.youview.www;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

<<<<<<< HEAD
=======
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.exporter.MetricsServlet;
>>>>>>> a384150... Added new metrics for YV task alerts
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
<<<<<<< HEAD
public class YouviewMetricsController extends io.prometheus.client.exporter.MetricsServlet {

    private YouviewMetricsController(io.prometheus.client.CollectorRegistry registry) {
        super(registry);
    }

    public static YouviewMetricsController create(io.prometheus.client.CollectorRegistry registry) {
=======
public class YouviewMetricsController extends MetricsServlet {

    private YouviewMetricsController(CollectorRegistry registry) {
        super(registry);
    }

    public static YouviewMetricsController create(CollectorRegistry registry) {
>>>>>>> a384150... Added new metrics for YV task alerts
        return new YouviewMetricsController(registry);
    }

    @RequestMapping(value = "/system/prometheusMetrics", method = RequestMethod.GET)
<<<<<<< HEAD
    public void getPrometheusMetrics(
            HttpServletRequest req,
            HttpServletResponse resp
    ) throws IOException, ServletException {
=======
    public void getPrometheusMetrics(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
>>>>>>> a384150... Added new metrics for YV task alerts
        doGet(req, resp);
    }
}
