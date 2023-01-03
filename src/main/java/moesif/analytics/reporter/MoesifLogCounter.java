package moesif.analytics.reporter;

import com.google.gson.Gson;
import com.moesif.api.controllers.APIController;
import com.moesif.api.models.*;
import moesif.analytics.reporter.utils.MoesifConstants;
import moesif.analytics.reporter.utils.UUIDCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.GenericInputValidator;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MoesifLogCounter implements CounterMetric {



    private static final Logger log = LoggerFactory.getLogger(MoesifLogCounter.class);
    private String name;
    private MetricSchema schema;
    private final Gson gson;

    private APIController api;

    // uses to create a UUID (userID) from the userIP
    static final String UUIDNameSpace = MoesifConstants.NAMESPACE_URL;
    private UUIDCreator uuidCreator;

    private final Map<String,String> properties;
    public MoesifLogCounter(String name, MetricSchema schema, APIController  api,Map<String, String> properties) {
        this.name = name;
        this.schema = schema;
        this.api = api;
        this.gson = new Gson();
        this.uuidCreator = new UUIDCreator();
        this.properties = properties;
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
                return new MoesifResponseMetricEventBuilder(GenericInputValidator.getInstance().getEventProperties(MetricSchema.RESPONSE));
            case ERROR:
                return new MoesifResponseMetricEventBuilder(GenericInputValidator.getInstance().getEventProperties(MetricSchema.ERROR));
            default:
                // will not happen
                return null;
        }
    }

    public void publish(Map<String,Object> event) throws Throwable {

        switch(schema) {
            case RESPONSE:
//                ArrayList<EventModel> events = new ArrayList<>();
//                events.add(buildEventResponse(event));
//                api.createEventsBatchAsync(events,null);
                api.createEvent(buildEventResponse(event));
                break;
            case ERROR:
                api.createEvent(buildEventFault(event));
                break;
        }
    }

    public EventModel buildEventResponse(Map<String , Object>  data) throws IOException, MetricReportingException {
        String jsonString = gson.toJson(data);
        String reqBody =  jsonString.replaceAll("[\r\n]", "");

        //      Preprocessing data
        final URL uri = new URL((String) data.get(MoesifConstants.DESTINATION));
        final String hostName = uri.getHost();

        final String userIP = (String) data.get(MoesifConstants.USER_IP);

        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent", (String) data.getOrDefault(MoesifConstants.USER_AGENT_HEADER,MoesifConstants.UNKNOWN_VALUE));
        reqHeaders.put("Content-Type", MoesifConstants.MOESIF_CONTENT_TYPE_HEADER);
        reqHeaders.put("Host",hostName);

//        reqHeaders.put("Connection", "Keep-Alive");
//        reqHeaders.put("Content-Length", "126");
//        reqHeaders.put("Accept-Encoding", "gzip");

//         Following headers are applicable for every request.
//          accept
//          connection

//         Following headers are applicable if payload is passed along the req obj.
//          content-length
//          content-encoding

        Map<String, String> rspHeaders = new HashMap<String, String>();

        rspHeaders.put("Vary", "Accept-Encoding");
        rspHeaders.put("Pragma", "no-cache");
        rspHeaders.put("Expires", "-1");
        rspHeaders.put("Content-Type", "application/json; charset=utf-8");
        rspHeaders.put("Cache-Control","no-cache");


        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date()) // See if you can parse request time stamp to date obj
                .uri(uri.toString())
                .verb((String) data.get(MoesifConstants.API_METHOD))
                .apiVersion((String) data.get(MoesifConstants.API_VERSION))
                .ipAddress(userIP)
                .headers(reqHeaders)
                .body(reqBody)
                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status((int) data.get(MoesifConstants.TARGET_RESPONSE_CODE))
                .headers(rspHeaders)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .userId((String) data.get("userName"))
                .companyId((String) data.get(MoesifConstants.ORGANIZATION_ID))
                .build();

        return eventModel;
    }
    public EventModel buildEventFault(Map<String , Object>  data) throws IOException, MetricReportingException {
// Generate the event
        String jsonString = gson.toJson(data);
        String reqBody =  jsonString.replaceAll("[\r\n]", "");

        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent", (String) data.get(MoesifConstants.USER_AGENT_HEADER) + " fault");

        Map<String, String> rspHeaders = new HashMap<String, String>();

        final String userIP = (String) data.get(MoesifConstants.USER_IP);

        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date()) // See if you can parse request time stamp to date obj
                .uri((String) data.get(MoesifConstants.DESTINATION))
                .verb((String) data.get(MoesifConstants.API_METHOD))
                .apiVersion((String) data.get(MoesifConstants.API_VERSION))
                .ipAddress(userIP)
                .headers(reqHeaders)
                .body(reqBody)
                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status((int) data.get(MoesifConstants.TARGET_RESPONSE_CODE))
                .headers(rspHeaders)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .userId(this.uuidCreator.getUUIDStrFromName(MoesifLogCounter.UUIDNameSpace,
                                                            userIP))
                .companyId((String) data.get(MoesifConstants.ORGANIZATION_ID))
                .build();

        return eventModel;
    }
}