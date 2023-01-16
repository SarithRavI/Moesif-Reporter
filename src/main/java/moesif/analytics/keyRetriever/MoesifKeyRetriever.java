package moesif.analytics.keyRetriever;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import moesif.analytics.reporter.utils.MoesifKeyEntry;
import moesif.analytics.reporter.utils.MoesifMicroserviceConstants;


public class MoesifKeyRetriever {
    private ConcurrentHashMap<String, String> orgID_moesifKeyMap;
    private static MoesifKeyRetriever moesifKeyRetriever;

    private MoesifKeyRetriever() {
        orgID_moesifKeyMap = new ConcurrentHashMap();
    }

    public static synchronized MoesifKeyRetriever getInstance(){
        if(moesifKeyRetriever == null) {
            return new MoesifKeyRetriever();
        }
        return moesifKeyRetriever;
    }
    public void initOrRefreshOrgIDMoesifKeyMap() {
        int attempts = MoesifMicroserviceConstants.NUM_RETRY_ATTEMPTS;
        try {
            callListResource();
        } catch (IOException ex) {
            // TODO: Separate retry logic to a separate class.
            System.out.println(ex.getMessage());
            while(attempts >0){
                attempts--;
                try {
                    Thread.sleep(MoesifMicroserviceConstants.TIME_TO_WAIT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    callListResource();
                } catch (IOException e) {
                    System.out.println("Retried and got: "+e.getMessage());
                }
            }
        }
    }

    public String getMoesifKey(String orgID) {
        String response;
        int attempts = MoesifMicroserviceConstants.NUM_RETRY_ATTEMPTS;
        try {
            response = callDetailResource(orgID);
        } catch (IOException ex) {
            // TODO: Separate retry logic to a separate class.
            System.out.println(ex.getMessage());
            while(attempts >0){
                attempts--;
                try {
                    Thread.sleep(MoesifMicroserviceConstants.TIME_TO_WAIT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    response = callDetailResource(orgID);
                    return response;
                } catch (IOException e) {
                    System.out.println("Retried and got: "+e.getMessage());
                }
            }
            response = null;
        }
        return response;
    }
    public  void callListResource() throws IOException {
        URL obj = new URL(MoesifMicroserviceConstants.LIST_URL);
        String auth = "";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + encodedAuth;

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", authHeaderValue);
        con.setRequestProperty("Content-Type", MoesifMicroserviceConstants.CONTENT_TYPE);
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            updateMap(response.toString());
        } else {
            throw new IOException("Getting " + responseCode + " from the microservice");
        }
    }

    public  String callDetailResource(String orgID) throws IOException {
        StringBuffer response = new StringBuffer();
        String url = MoesifMicroserviceConstants.DETAIL_URL + "?" + MoesifMicroserviceConstants.QUERY_PARAM + "=" +
                orgID;
        URL obj = new URL(url);
        String auth = "";
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + encodedAuth;

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", MoesifMicroserviceConstants.CONTENT_TYPE);
        con.setRequestProperty("Authorization", authHeaderValue);
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            updateMoesifKey(response.toString());
        } else {
            throw new IOException("Getting " + responseCode + " from the microservice");
        }
        return response.toString();
    }

    private  void updateMoesifKey(String response) {
        Gson gson = new Gson();
        String json = response;
        MoesifKeyEntry newKey = gson.fromJson(json, MoesifKeyEntry.class);
        orgID_moesifKeyMap.put(newKey.getOrganization_id(), newKey.getMoesif_key());

    }

    private  void updateMap(String response) {
        Gson gson = new Gson();
        String json = response;
        Type collectionType = new TypeToken<Collection<MoesifKeyEntry>>() {}.getType();
        Collection<MoesifKeyEntry> newKeys = gson.fromJson(json, collectionType);

        for (MoesifKeyEntry entry : newKeys) {
            orgID_moesifKeyMap.put(entry.getOrganization_id(), entry.getMoesif_key());
        }
    }

    public ConcurrentHashMap<String,String> getMoesifKeyMap(){
        return orgID_moesifKeyMap;
    }
}
