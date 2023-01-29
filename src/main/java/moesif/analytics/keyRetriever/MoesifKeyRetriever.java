package moesif.analytics.keyRetriever;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.moesif.api.MoesifAPIClient;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import moesif.analytics.exception.APICallingException;
import moesif.analytics.reporter.utils.MoesifKeyEntry;
import moesif.analytics.reporter.utils.MoesifMicroserviceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoesifKeyRetriever {
    private static final Logger log = LoggerFactory.getLogger(MoesifKeyRetriever.class);
    private static MoesifKeyRetriever moesifKeyRetriever;
    private ConcurrentHashMap<String, String> orgID_moesifKeyMap;

    private ConcurrentHashMap<String, MoesifAPIClient> moesifKey_moesifClientMap;
    private String gaAuthUsername;
    private char[] gaAuthPwd;

    private MoesifKeyRetriever(String authUsername, String authPwd) {

        this.gaAuthUsername = authUsername;
        this.gaAuthPwd = authPwd.toCharArray();
        orgID_moesifKeyMap = new ConcurrentHashMap();
        moesifKey_moesifClientMap = new ConcurrentHashMap();
    }

    public static synchronized MoesifKeyRetriever getInstance(String authUsername, String authPwd) {
        if (moesifKeyRetriever == null) {
            return new MoesifKeyRetriever(authUsername, authPwd);
        }
        return moesifKeyRetriever;
    }

    public void initOrRefreshOrgIDMoesifKeyMap() {
        int attempts = MoesifMicroserviceConstants.NUM_RETRY_ATTEMPTS;
        try {
            callListResource();
        } catch (IOException | APICallingException ex) {
            // TODO: Separate retry logic to a separate class.
            log.error("First attempt failed,retrying.", ex.getMessage());
            while (attempts > 0) {
                attempts--;
                try {
                    Thread.sleep(MoesifMicroserviceConstants.TIME_TO_WAIT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    callListResource();
                } catch (IOException | APICallingException e) {
                    log.error("Retry attempt failed and got: " + e.getMessage());
                }
            }
        }
    }

    public String getMoesifKey(String orgID) {
        String response;
        int attempts = MoesifMicroserviceConstants.NUM_RETRY_ATTEMPTS;
        try {
            response = callDetailResource(orgID);
        } catch (IOException | APICallingException ex) {
            // TODO: Separate retry logic to a separate class.
            log.error("First attempt failed,retrying.", ex.getMessage());
            while (attempts > 0) {
                attempts--;
                try {
                    Thread.sleep(MoesifMicroserviceConstants.TIME_TO_WAIT);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                try {
                    response = callDetailResource(orgID);
                    return response;
                } catch (IOException | APICallingException e) {
                    log.error("Retry attempt failed and got: " + e.getMessage());
                }
            }
            response = null;
        }
        return response;
    }

    // Delete moesif key from the internal map.
    public void removeMoesifKeyFromMap(String orgID) {
        String moesifKey =orgID_moesifKeyMap.remove(orgID);
        moesifKey_moesifClientMap.remove(moesifKey);
    }

    public void callListResource() throws IOException, APICallingException {
        URL obj;
        try {
            obj = new URL(MoesifMicroserviceConstants.LIST_URL);
        } catch (MalformedURLException ex) {
            log.error("Event will be dropped. Getting "+ex);
            return;
        }
        String auth = gaAuthUsername + ":" + gaAuthPwd.toString();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + encodedAuth;

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", authHeaderValue);
        con.setRequestProperty("Content-Type", MoesifMicroserviceConstants.CONTENT_TYPE);
        con.setReadTimeout(MoesifMicroserviceConstants.REQUEST_READ_TIMEOUT);
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
        } else if (responseCode != 400 && responseCode != 401 && responseCode != 403 && responseCode != 404 &&
                responseCode != 409) {
            con.disconnect();
            throw new APICallingException("Getting " + responseCode + " from the microservice and retrying.");
        }
        con.disconnect();

    }

    public String callDetailResource(String orgID) throws IOException, APICallingException {
        StringBuffer response = new StringBuffer();
        String url = MoesifMicroserviceConstants.DETAIL_URL + "?" + MoesifMicroserviceConstants.QUERY_PARAM + "=" +
                orgID;
        URL obj;
        try {
            obj = new URL(url);
        } catch (MalformedURLException ex) {
            log.error("Event will be dropped. Getting "+ex);
            return null;
        }
        String auth = gaAuthUsername + ":" + gaAuthPwd.toString();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeaderValue = "Basic " + encodedAuth;

        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", MoesifMicroserviceConstants.CONTENT_TYPE);
        con.setRequestProperty("Authorization", authHeaderValue);
        con.setReadTimeout(MoesifMicroserviceConstants.REQUEST_READ_TIMEOUT);
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            updateMoesifKey(response.toString());
        } else if (responseCode != 400 && responseCode != 401 && responseCode != 403 && responseCode != 404 &&
                responseCode != 409) {
            con.disconnect();
            throw new APICallingException("Getting " + responseCode + " from the microservice and retrying.");
        }
        con.disconnect();
        return response.toString();
    }

    private void updateMoesifKey(String response) {
        Gson gson = new Gson();
        String json = response;
        MoesifKeyEntry newKey = gson.fromJson(json, MoesifKeyEntry.class);
        String orgID = newKey.getOrganization_id();
        String moesifKey = newKey.getMoesif_key();
        orgID_moesifKeyMap.put(orgID, moesifKey);
        moesifKey_moesifClientMap.put(moesifKey, new MoesifAPIClient(moesifKey));
    }

    private void updateMap(String response) {
        Gson gson = new Gson();
        String json = response;
        Type collectionType = new TypeToken<Collection<MoesifKeyEntry>>() {
        }.getType();
        Collection<MoesifKeyEntry> newKeys = gson.fromJson(json, collectionType);

        for (MoesifKeyEntry entry : newKeys) {
            String orgID = entry.getOrganization_id();
            String moesifKey = entry.getMoesif_key();
            orgID_moesifKeyMap.put(orgID, moesifKey);
            moesifKey_moesifClientMap.put(moesifKey, new MoesifAPIClient(moesifKey));
        }
    }

    public ConcurrentHashMap<String, String> getMoesifKeyMap() {
        return orgID_moesifKeyMap;
    }

    public MoesifAPIClient getMoesifClient(String moesifKey){
        return  moesifKey_moesifClientMap.get(moesifKey);
    }
}
