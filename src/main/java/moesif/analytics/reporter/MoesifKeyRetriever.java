package moesif.analytics.reporter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    public static void callListResource() throws IOException {
        URL obj = new URL(MoesifMicroserviceConstants.LIST_URL);
        String auth = "";
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
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

    public static String callDetailResource(String orgID) throws IOException {
        StringBuffer response = new StringBuffer();
        String url = MoesifMicroserviceConstants.DETAIL_URL + "?" + MoesifMicroserviceConstants.QUERY_PARAM + "=" +
                orgID;
        URL obj = new URL(url);
        String auth = "";
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
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

    public synchronized static void initOrRefreshOrgIDMoesifKeyMap() {
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

    public synchronized static String getMoesifKey(String orgID) {
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

    private static synchronized void updateMoesifKey(String response) {
        Gson gson = new Gson();
        String json = response;
        TypeToken<MoesifKeyEntry> collectionType = new TypeToken<MoesifKeyEntry>() {
        };
        MoesifKeyEntry newKey = gson.fromJson(json, collectionType);
        orgID_moesifKeyMap.put(newKey.getOrganization_id(), newKey.getMoesif_key());

    }

    private static synchronized  void updateMap(String response) {
        Gson gson = new Gson();
        String json = response;
        TypeToken<Collection<MoesifKeyEntry>> collectionType = new TypeToken<Collection<MoesifKeyEntry>>() {
        };
        Collection<MoesifKeyEntry> newKeys = gson.fromJson(json, collectionType);

        MoesifKeyEntry[] newKeysArr = (MoesifKeyEntry[]) newKeys.toArray();
        for (MoesifKeyEntry entry : newKeysArr) {
            orgID_moesifKeyMap.put(entry.getOrganization_id(), entry.getMoesif_key());
        }
    }
}
