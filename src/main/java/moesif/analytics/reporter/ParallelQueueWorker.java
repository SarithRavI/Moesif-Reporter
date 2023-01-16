package moesif.analytics.reporter;

import com.google.gson.Gson;
import com.moesif.api.MoesifAPIClient;
import com.moesif.api.controllers.APIController;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import moesif.analytics.keyRetriever.MoesifKeyRetriever;
import moesif.analytics.reporter.utils.MoesifConstants;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;

public class ParallelQueueWorker implements Runnable{
    private BlockingQueue<MetricEventBuilder> eventQueue;
    private MoesifKeyRetriever keyRetriever;

    public ParallelQueueWorker(BlockingQueue<MetricEventBuilder> queue,MoesifKeyRetriever keyRetriever) {
        this.eventQueue = queue;
        this.keyRetriever = keyRetriever;
    }
    public void run() {

        while (true) {
            MetricEventBuilder eventBuilder;
            try {
                eventBuilder = eventQueue.take();
                if (eventBuilder != null) {
                    Map<String, Object> eventMap = eventBuilder.build();
                    publish(eventMap);
                }
            } catch (MetricReportingException e) {
//                log.error("Builder instance is not duly filled. Event building failed", e);
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
//                log.error("Analytics event sending failed. Event will be dropped", e);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

        }
    }
    public void publish(Map<String, Object> event) throws Throwable, MetricReportingException {
        // if the orgID_moesifKeyMap is empty refresh it.
        ConcurrentHashMap<String,String> orgID_moesifKeyMap = keyRetriever.getMoesifKeyMap();
        if(orgID_moesifKeyMap.isEmpty()){
            keyRetriever.initOrRefreshOrgIDMoesifKeyMap();
        }

        // get the org id from the event
        String org_id = (String) event.get(MoesifConstants.ORGANIZATION_ID);
        // fetch the moesif key from orgID_moesifKeyMap
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

        // init moesif client
        MoesifAPIClient client = new MoesifAPIClient(moesif_key);
        APIController api = client.getAPI();
        api.createEvent(buildEventResponse(event));

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
