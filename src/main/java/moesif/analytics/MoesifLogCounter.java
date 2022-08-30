package moesif.analytics;

import com.moesif.api.controllers.APIController;
import com.moesif.api.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;
import org.wso2.am.analytics.publisher.reporter.cloud.DefaultFaultMetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.cloud.DefaultResponseMetricEventBuilder;
import org.wso2.am.analytics.publisher.util.Constants;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MoesifLogCounter implements CounterMetric {



    private static final Logger log = LoggerFactory.getLogger(MoesifLogCounter.class);
    private String name;
    private MetricSchema schema;


    private APIController api;

    public MoesifLogCounter(String name, MetricSchema schema, APIController  api) {
        this.name = name;
        this.schema = schema;
        this.api = api;


    }

    @Override
    public int incrementCount(MetricEventBuilder metricEventBuilder) throws MetricReportingException {
        Map<String, Object> event = metricEventBuilder.build();
        try {
            publish(event);
        } catch (Throwable e) {
            throw new RuntimeException("Moesif: Not publishing event");
        }
        return 0;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public MetricSchema getSchema() {
        return this.schema;
    }

    @Override
    public MetricEventBuilder getEventBuilder() {
        switch (schema) {
            case RESPONSE:
                return new DefaultResponseMetricEventBuilder();
            case ERROR:
                return new DefaultFaultMetricEventBuilder();
            default:
                // will not happen
                return null;
        }
    }

    public void publish(Map<String,Object> event) throws Throwable {


        switch(schema) {
            case RESPONSE:
                api.createEvent(buildEventResponse(event));
            case ERROR:
                api.createEvent(buildEventFault(event));
            default:
                //Shouldn't happen
        }
    }

    public EventModel buildEventResponse(Map<String , Object>  data) throws IOException, MetricReportingException {

        // Generate the event
        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent", (String) data.get(Constants.USER_AGENT_HEADER));



        Map<String, String> rspHeaders = new HashMap<String, String>();



        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date()) // See if you can parse request time stamp to date obj
                .uri((String) data.get(Constants.DESTINATION))
                .verb((String) data.get(Constants.API_METHOD))
                .apiVersion((String) data.get(Constants.API_VERSION))
                .ipAddress((String) data.get(Constants.USER_IP))
                .headers(reqHeaders)

                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status((int) data.get(Constants.TARGET_RESPONSE_CODE))
                .headers(rspHeaders)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .companyId((String) data.get(Constants.ORGANIZATION_ID))
                .build();

        return eventModel;
    }
    public EventModel buildEventFault(Map<String , Object>  data) throws IOException, MetricReportingException {
// Generate the event
        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent", Constants.USER_AGENT);



        Map<String, String> rspHeaders = new HashMap<String, String>();


        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date()) // See if you can parse request time stamp to date obj
                .uri((String) data.get(Constants.DESTINATION))
                .verb((String) data.get(Constants.API_METHOD))
                .apiVersion((String) data.get(Constants.API_VERSION))
                .ipAddress((String) data.get(Constants.USER_IP))
                .headers(reqHeaders)
                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status((int) data.get(Constants.TARGET_RESPONSE_CODE))
                .headers(rspHeaders)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .companyId((String) data.get(Constants.ORGANIZATION_ID))
                .build();

        return eventModel;
    }
}
