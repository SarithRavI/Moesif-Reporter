package moesif.analytics.client;

import com.google.gson.Gson;
import com.moesif.api.MoesifAPIClient;
import com.moesif.api.controllers.APIController;
import com.moesif.api.http.client.APICallBack;
import com.moesif.api.http.client.HttpContext;
import com.moesif.api.http.response.HttpResponse;
import com.moesif.api.models.EventBuilder;
import com.moesif.api.models.EventModel;
import com.moesif.api.models.EventRequestBuilder;
import com.moesif.api.models.EventRequestModel;
import com.moesif.api.models.EventResponseBuilder;
import com.moesif.api.models.EventResponseModel;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import moesif.analytics.keyRetriever.MoesifKeyRetriever;
import moesif.analytics.reporter.utils.MoesifConstants;
import moesif.analytics.reporter.utils.MoesifMicroserviceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;

public class MoesifClient {
    private final Logger log = LoggerFactory.getLogger(MoesifClient.class);
    private final MoesifKeyRetriever keyRetriever;
    public MoesifClient(MoesifKeyRetriever keyRetriever){
        this.keyRetriever = keyRetriever;
    }

    private  void doRetry(String org_id, Map<String,Object> event){
        Integer currentAttempt = ClientContextHolder.publishAttempts.get();

        if(currentAttempt > 0){
            if (currentAttempt == MoesifMicroserviceConstants.NUM_RETRY_ATTEMPTS_PUBLISH){
                // on failure remove the respective moesif key from the map.
                // this will result in a call to the microservice to retrieve the key.
                // This is enough to happen only at the first attempt.
                keyRetriever.removeMoesifKeyFromMap(org_id);
            }
            currentAttempt-=1;
            ClientContextHolder.publishAttempts.set(currentAttempt);
            try {
                Thread.sleep(MoesifMicroserviceConstants.TIME_TO_WAIT_PUBLISH);
                publish(event);
            } catch (MetricReportingException e) {
                log.error("Failing retry attempt at Moesif client",e);
            } catch (InterruptedException e) {
                log.error("Failing retry attempt at Moesif client",e);
            }
        }
        else if(currentAttempt == 0){
            log.error("Failed all retrying attempts. Event will be dropped");
        }
    }
    public void publish(Map<String, Object> event) throws MetricReportingException {
        ConcurrentHashMap<String, String> orgID_moesifKeyMap = keyRetriever.getMoesifKeyMap();
        if (orgID_moesifKeyMap.isEmpty()) {
            keyRetriever.initOrRefreshOrgIDMoesifKeyMap();
        }

        String org_id = (String) event.get(MoesifConstants.ORGANIZATION_ID);
        String moesif_key;
        if (orgID_moesifKeyMap.containsKey(org_id)) {
            moesif_key = orgID_moesifKeyMap.get(org_id);
        } else {
            moesif_key = keyRetriever.getMoesifKey(org_id);
            if (moesif_key == null) {
                throw new MetricReportingException(
                        "Corresponding Moesif key for organization " + org_id + " can't be found.");
            }
        }

        // init moesif api client
        MoesifAPIClient client = keyRetriever.getMoesifClient(moesif_key);
        APIController api = client.getAPI();

        APICallBack<HttpResponse> callBack = new APICallBack<>() {
            public void onSuccess(HttpContext context, HttpResponse response) {
                int statusCode = context.getResponse().getStatusCode();
                if (statusCode ==200){
                    log.debug("Event successfully published.");
                }
                else if(statusCode>=400 && statusCode<500){
                    log.error("Event publishing failed. Moesif returned "+statusCode);
                }
                else{
                    log.error("Event publishing failed.Retrying.");
                    doRetry(org_id,event);
                }
            }

            public void onFailure(HttpContext context, Throwable error) {
                int statusCode = context.getResponse().getStatusCode();

                if(statusCode>=400 && statusCode<500){
                    log.error("Event publishing failed. Moesif returned "+statusCode);
                }
                else if (error != null){
                    log.error("Event publishing failed."+ error.getMessage() );
                }
                else{
                    log.error("Event publishing failed.Retrying.");
                    doRetry(org_id,event);
                }

            }
        };
        try {
            api.createEventAsync(buildEventResponse(event), callBack);
        }
        catch(IOException e){
            log.error("Analytics event sending failed. Event will be dropped", e);
        }

    }

    public EventModel buildEventResponse(Map<String, Object> data) throws IOException, MetricReportingException {
        Gson gson = new Gson();
        String jsonString = gson.toJson(data);
        String reqBody = jsonString.replaceAll("[\r\n]", "");

        //      Preprocessing data
        final URL uri = new URL((String) data.get(MoesifConstants.DESTINATION));
        final String hostName = uri.getHost();

        final String userIP = (String) data.get(MoesifConstants.USER_IP);

        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent",
                (String) data.getOrDefault(MoesifConstants.USER_AGENT_HEADER, MoesifConstants.UNKNOWN_VALUE));
        reqHeaders.put("Content-Type", MoesifConstants.MOESIF_CONTENT_TYPE_HEADER);
        reqHeaders.put("Host", hostName);

        Map<String, String> rspHeaders = new HashMap<String, String>();

        rspHeaders.put("Vary", "Accept-Encoding");
        rspHeaders.put("Pragma", "no-cache");
        rspHeaders.put("Expires", "-1");
        rspHeaders.put("Content-Type", "application/json; charset=utf-8");
        rspHeaders.put("Cache-Control", "no-cache");


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

}
