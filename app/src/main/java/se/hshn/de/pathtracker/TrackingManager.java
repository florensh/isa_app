package se.hshn.de.pathtracker;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;

/**
 * Created by florens on 10.01.17.
 */

public class TrackingManager extends Observable {

    private static TrackingManager manager = new TrackingManager();

    private final ReceivingDataState RECEIVING_DATA_STATE = new ReceivingDataState();
    private final SearchForStoreState SEARCHING_STORES_STATE = new SearchForStoreState();
    private final NoStoresInBackend NO_STORES_IN_BACKEND = new NoStoresInBackend();
    private final WaitingForTracking WAITING_FOR_TRACKING = new WaitingForTracking();
    private final TrackingState TRACKING_STATE = new TrackingState();
    private final float TOLERANCE = 0.0001f;
    public AppState state;
    public Location currentLocation = null;
    public Store currentStore = null;
    public String userId = null;
    private List<Store> storeList = null;
    private String sessionId = null;

    private Long storeVisitedAt = null;

    private TrackingManager() {
        this.state = RECEIVING_DATA_STATE;
        this.state.doOnStart();

    }

    public static TrackingManager getInstance() {
        return manager;
    }

    Store resolveStore(Location location) {
        for (Store s : storeList) {
            if (s.getLat() >= location.getLatitude() - TOLERANCE && s.getLat() <= location.getLatitude() + TOLERANCE
                    && s.getLon() >= location.getLongitude() - TOLERANCE && s.getLon() <= location.getLongitude() + TOLERANCE) {
                return s;
            }
        }
        return null;
    }

    public void setUserid(String macAddress) {
        this.userId = macAddress;
    }

    public void setLocation(Location location) {
        this.state.handleGpsUpdate(location);

    }

    interface AppState {
        void handleGpsUpdate(Location location);

        String getName();

        void doOnStart();
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, List<Store>> {
        private static final String API_URL = "https://calm-coast-80282.herokuapp.com";

        @Override
        protected List<Store> doInBackground(Void... params) {
            try {
                String baseUrl = API_URL;

                FormHttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();

                HttpMessageConverter stringHttpMessageConverternew = new StringHttpMessageConverter();

                List<HttpMessageConverter<?>> messageConverters = new LinkedList<HttpMessageConverter<?>>();

                messageConverters.add(formHttpMessageConverter);
                messageConverters.add(stringHttpMessageConverternew);
                messageConverters.add(new MappingJackson2HttpMessageConverter());
                MultiValueMap map = new LinkedMultiValueMap();
                map.add("j_username", "user");
                map.add("j_password", "user");
                // map.add("Content-Type","application/x-www-form-urlencoded");


                String authURL = baseUrl + "/api/authentication";
                RestTemplate restTemplate = new RestTemplate();

                restTemplate.setMessageConverters(messageConverters);
                HttpHeaders requestHeaders = new HttpHeaders();
                requestHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                HttpEntity<MultiValueMap> entity = new HttpEntity<MultiValueMap>(map,
                        requestHeaders);

                ResponseEntity result = restTemplate.exchange(authURL, HttpMethod.POST, entity, String.class);
                HttpHeaders respHeaders = result.getHeaders();
                System.out.println(respHeaders.toString());

                System.out.println(result.getStatusCode());

                String cookies = respHeaders.getFirst("Set-Cookie");
                sessionId = Arrays.asList(cookies.split(";")).get(0);
                System.out.println(sessionId);


                HttpHeaders headers = new HttpHeaders();
                headers.add("Cookie", sessionId);

                HttpEntity<MultiValueMap> storesEntiy = new HttpEntity<MultiValueMap>(headers);

                ResponseEntity res = restTemplate.exchange(baseUrl + "/api/stores", HttpMethod.GET, storesEntiy, Store[].class);

                Store[] stores = (Store[]) res.getBody();

                //test
/*                Measurement m = new Measurement();
                m.setTimestamp(System.currentTimeMillis());
                m.setLat(12.99d);
                Set<Measurement> set = new HashSet<Measurement>();
                set.add(m);

                MeasurementDataset dataset = new MeasurementDataset();
                ObjectMapper mapper = new ObjectMapper();
                dataset.setMeasurements(mapper.writeValueAsString(set));

                HttpEntity measurementDatasetEntiy = new HttpEntity(dataset, headers);
                ResponseEntity<MeasurementDataset> out = restTemplate.exchange(baseUrl + "/api/measurement-datasets", HttpMethod.POST, measurementDatasetEntiy
                        , MeasurementDataset.class);

                System.out.println(out.getBody().getId());*/

                //end test

                return Arrays.asList(stores);
            } catch (Exception e) {
                Log.e("MainActivity", e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(List<Store> stores) {
            if (state instanceof ReceivingDataState) {


                ReceivingDataState rState = (ReceivingDataState) state;
                rState.receivedData(stores);
            }

        }

    }

    abstract class AbstractAppState implements AppState {

        public void changeStatus(AppState nextState) {
            state = nextState;
            nextState.doOnStart();
            setChanged();
            notifyObservers();
        }

        public void doOnStart() {

        }

        @Override
        public void handleGpsUpdate(Location location) {
            currentLocation = location;
            setChanged();
            notifyObservers();

        }
    }

    class ReceivingDataState extends AbstractAppState implements AppState {

        @Override
        public String getName() {
            return "Receiving data";
        }

        @Override
        public void doOnStart() {
            super.doOnStart();
            new HttpRequestTask().execute();
        }

        public void receivedData(List<Store> data) {
            System.out.println("Got " + data.size() + " stores from backend!");

            if (data != null && !data.isEmpty()) {
                storeList = data;
                changeStatus(SEARCHING_STORES_STATE);
            } else {
                changeStatus(NO_STORES_IN_BACKEND);
            }


        }
    }

    class NoStoresInBackend extends AbstractAppState implements AppState {

        @Override
        public String getName() {
            return "No Stores in Backend";
        }
    }

    class SearchForStoreState extends AbstractAppState implements AppState {


        @Override
        public void doOnStart() {
            super.doOnStart();
            storeVisitedAt = null;
            if (currentLocation != null && System.currentTimeMillis() < currentLocation.getTime() + 10000) {
                findStore(currentLocation);
            }
        }


        @Override
        public void handleGpsUpdate(Location location) {
            super.handleGpsUpdate(location);
            findStore(location);

        }

        private synchronized void findStore(Location location) {
            Store s = resolveStore(location);
            if (s != null) {
                currentStore = s;
                storeVisitedAt = location.getTime();
                changeStatus(WAITING_FOR_TRACKING);

            }

        }

        @Override
        public String getName() {
            return "Searching for stores";
        }

    }

    class WaitingForTracking extends AbstractAppState implements AppState {

        @Override
        public void handleGpsUpdate(Location location) {
            super.handleGpsUpdate(location);

            Store s = resolveStore(location);
            if (s != null && s.getId().equals(currentStore.getId())) {
                if (location.getTime() > storeVisitedAt + 5000 && location.getAccuracy() < 10.0f) {
                    changeStatus(TRACKING_STATE);
                }
            } else {
                changeStatus(SEARCHING_STORES_STATE);
            }

        }

        @Override
        public String getName() {
            return "Waiting for tracking";
        }
    }

    class TrackingState extends AbstractAppState implements AppState {

        @Override
        public String getName() {
            return "Tracking path";
        }
    }


}
