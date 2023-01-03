package moesif.analytics.reporter;

import com.moesif.api.MoesifAPIClient;
import com.moesif.api.controllers.APIController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricCreationException;
import org.wso2.am.analytics.publisher.reporter.AbstractMetricReporter;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;
import org.wso2.am.analytics.publisher.reporter.TimerMetric;

import java.util.Map;

public class MoesifReporter extends AbstractMetricReporter {
    private MoesifAPIClient client;
    private final APIController api;

    private static final Logger log = LoggerFactory.getLogger(MoesifReporter.class);
    private final Map<String,String> properties;
    public MoesifReporter(Map<String, String> properties) throws MetricCreationException {
        super(properties);
        String ID = "eyJhcHAiOiIxMDUxOjI1NSIsInZlciI6IjIuMCIsIm9yZyI6IjM1MToyNzciLCJpYXQiOjE2NjcyNjA4MDB9.rPbXw_lU6E5-5Ws-DG2uhiIEIecTMBtkQpJpSBATt5o";
        client = new MoesifAPIClient(ID);
        api = APIController.getInstance();
        log.info("Successfully initialized");
        this.properties = properties;
    }

    @Override
    protected void validateConfigProperties(Map<String, String> map) throws MetricCreationException {

    }

    @Override
    public CounterMetric createCounter(String name, MetricSchema metricSchema) throws MetricCreationException {
        MoesifLogCounter logCounterMetric = new MoesifLogCounter(name, metricSchema,api,this.properties);

        return logCounterMetric;
    }

    @Override
    protected TimerMetric createTimer(String s) {
        return null;
    }


}