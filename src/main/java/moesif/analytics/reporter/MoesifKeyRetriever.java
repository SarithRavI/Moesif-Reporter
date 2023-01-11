package moesif.analytics.reporter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import moesif.analytics.reporter.utils.MoesifKeyEntry;
import moesif.analytics.reporter.utils.MoesifMicroserviceConstants;

public class MoesifKeyRetriever {
    static ConcurrentMap<String, String> orgID_moesifKeyMap;

    public MoesifKeyRetriever() {
        orgID_moesifKeyMap = new ConcurrentHashMap();
        initOrRefreshOrgIDMoesifKeyMap();
    }

    public static void callListResource() throws IOException{
        URL obj = new URL(MoesifMicroserviceConstants.LIST_URL);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
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
            throw new IOException("Getting "+responseCode+" from the microservice");
        }
    }
    public static String callDetailResource(String orgID) throws IOException{
        StringBuffer response = new StringBuffer();
        String url = MoesifMicroserviceConstants.DETAIL_URL + "?" + MoesifMicroserviceConstants.QUERY_PARAM + "=" +
                orgID;
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", MoesifMicroserviceConstants.CONTENT_TYPE);
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

    public static void initOrRefreshOrgIDMoesifKeyMap() {
        try {
            callListResource();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
            // TODO: Add retry mechanism
        }
    }

    public static String getMoesifKey(String orgID) {
        String response;
        try {
            response = callDetailResource(orgID);
        }
        catch (IOException ex){
            System.out.println(ex.getMessage());
            // TODO: Add retry mechanism
            response = null;
        }
        return response;
    }

    private synchronized static void updateMoesifKey(String response) {
        Gson gson = new Gson();
        String json = response;
        TypeToken<MoesifKeyEntry> collectionType = new TypeToken(){};
        MoesifKeyEntry newKey = gson.fromJson(json, collectionType);
        orgID_moesifKeyMap.put(newKey.getOrganization_id(), newKey.getMoesif_key());

    }
    private synchronized static void updateMap(String response){
        Gson gson = new Gson();
        String json = response;
        TypeToken<Collection<MoesifKeyEntry>> collectionType = new TypeToken(){};
        Collection<MoesifKeyEntry> newKeys = gson.fromJson(json, collectionType);

        MoesifKeyEntry[] newKeysArr = (MoesifKeyEntry[]) newKeys.toArray();
        for(MoesifKeyEntry entry : newKeysArr){
            orgID_moesifKeyMap.put(entry.getOrganization_id(), entry.getMoesif_key());
        }
    }
}
