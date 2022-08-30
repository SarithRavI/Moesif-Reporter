package moesif.analytics;

import com.moesif.api.MoesifAPIClient;
import com.moesif.api.controllers.APIController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricCreationException;

import org.wso2.am.analytics.publisher.reporter.*;


import java.util.Map;

public class MoesifReporter implements MetricReporter {
    private MoesifAPIClient client;
    private final APIController api;

    private static final Logger log = LoggerFactory.getLogger(MoesifReporter.class);

    public MoesifReporter(Map<String, String> properties) {
        String ID = "eyJhcHAiOiI2NjA6MzU2IiwidmVyIjoiMi4wIiwib3JnIjoiMjkxOjMyMSIsImlhdCI6MTY1OTMxMjAwMH0.ftR87qJRhnc-GwXdTIJZv5zQ2s08BnUU5UxRLARksZg";
        client = new MoesifAPIClient(ID);

        api = APIController.getInstance();
        log.info("Successfully initialized");
    }

    @Override
    public CounterMetric createCounterMetric(String name, MetricSchema metricSchema) throws MetricCreationException {
        MoesifLogCounter logCounterMetric = new MoesifLogCounter(name, metricSchema,api);

        return logCounterMetric;
    }

    @Override
    public TimerMetric createTimerMetric(String s) {
        return null;
    }

    @Override
    public Map<String, String> getConfiguration() {
        return null;
    }





}
