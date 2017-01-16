package se.hshn.de.pathtracker;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Observable;
import java.util.Set;

/**
 * Created by florens on 10.01.17.
 */

public class TrackingManager extends Observable {

    private static TrackingManager manager = new TrackingManager();
    private static final String API_URL = "https://calm-coast-80282.herokuapp.com";
    private final ReceivingDataState RECEIVING_DATA_STATE = new ReceivingDataState();
    private final SearchForStoreState SEARCHING_STORES_STATE = new SearchForStoreState();
    private final NoStoresInBackend NO_STORES_IN_BACKEND = new NoStoresInBackend();
    private final WaitingForTracking WAITING_FOR_TRACKING = new WaitingForTracking();
    private final TrackingState TRACKING_STATE = new TrackingState();
    private final PostProcessState POST_PROCESS_STATE = new PostProcessState();
    private final float TOLERANCE = 0.0003f;
    public AppState state;
    public Location currentLocation = null;
    public Store currentStore = null;
    public String userId = null;
    public int steps = 0;
    private List<Store> storeList = null;
    private String sessionId = null;


    StepDetector stepDetector;
    MeasurementDataset dataSet;
    StepEstimator stepEstimator;
    List<Measurement> measurements;




    private SensorManager sMgr;

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

    public void setSensorManager(SensorManager manager) {
        this.sMgr = manager;
    }

    interface AppState {
        void handleGpsUpdate(Location location);

        String getName();

        void doOnStart();
    }


    private class HttpRequestTask extends AsyncTask<Void, Void, List<Store>> {

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

                ResponseEntity result = null;

                boolean gotResponse = false;

                while (!gotResponse) {
                    try {
                        result = restTemplate.exchange(authURL, HttpMethod.POST, entity, String.class);
                        gotResponse = true;

                    } catch (Exception e) {
                        Thread.sleep(3000);
                    }

                }


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

                return Arrays.asList(stores);
            } catch (Exception e) {
                Log.e("TrackingManager", e.getMessage(), e);
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
            if (currentLocation != null
                    //&& System.currentTimeMillis() < currentLocation.getTime() + 10000
                    ) {
                findStore(currentLocation);
            }
        }


        @Override
        public void handleGpsUpdate(Location location) {
            super.handleGpsUpdate(location);
            findStore(location);

        }

        private synchronized void findStore(Location location) {
            System.out.println("searching with location");
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
        public void doOnStart() {
            super.doOnStart();
            changeStatus(TRACKING_STATE);
        }

        @Override
        public void handleGpsUpdate(Location location) {
            super.handleGpsUpdate(location);

            Store s = resolveStore(location);
            if (s != null && s.getId().equals(currentStore.getId())) {
                if (location.getTime() > storeVisitedAt + 5000 && location.getAccuracy() <= 10.0f) {
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

    class TrackingState extends AbstractAppState implements AppState, StepListener {



        @Override
        public String getName() {
            return "Tracking path";
        }


/*        @Override
        public void handleGpsUpdate(Location location) {
            super.handleGpsUpdate(location);
            Store s = resolveStore(location);
            if(s == null || !s.getName().equals(currentStore)){
                stopAndSend();
                changeStatus(SEARCHING_STORES_STATE);
            }
        }*/

        @Override
        public void doOnStart() {
            super.doOnStart();

            steps = 0;
            stepDetector = new StepDetector();
            stepDetector.setSensitivity(15f);
            stepDetector.addStepListener(this);
            dataSet = new MeasurementDataset();
            stepEstimator = new StepEstimator();
            measurements = new ArrayList<Measurement>();

            Sensor acc, rot, mag;

            acc = sMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mag = sMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            rot = sMgr.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            sMgr.registerListener(stepDetector, acc, SensorManager.SENSOR_DELAY_FASTEST);
            // sMgr.registerListener(stepEstimator, acc, SensorManager.SENSOR_DELAY_FASTEST);
            sMgr.registerListener(stepEstimator, mag, SensorManager.SENSOR_DELAY_FASTEST);
            sMgr.registerListener(stepEstimator, rot, SensorManager.SENSOR_DELAY_FASTEST);

        }

        private void stopAndSend() {
            stepDetector.removeAllListener();
            changeStatus(POST_PROCESS_STATE);


        }


        @Override
        public void onStep() {
            steps++;
            setChanged();
            notifyObservers();
            measurements.add(stepEstimator.getStep(currentLocation));
            if(steps >= 20){
                stopAndSend();
            }
        }

        @Override
        public void passValue() {

        }






    }

    class PostProcessState extends AbstractAppState implements AppState {

        private class SendToBackendTask extends AsyncTask<Object, Object, Void> {


            @Override
            protected Void doInBackground(Object... params) {
                try {
                    String baseUrl = API_URL;

                    HttpMessageConverter stringHttpMessageConverternew = new StringHttpMessageConverter();

                    List<HttpMessageConverter<?>> messageConverters = new LinkedList<HttpMessageConverter<?>>();


                    messageConverters.add(stringHttpMessageConverternew);
                    messageConverters.add(new MappingJackson2HttpMessageConverter());

                    RestTemplate restTemplate = new RestTemplate();

                    restTemplate.setMessageConverters(messageConverters);

                    HttpHeaders headers = new HttpHeaders();
                    headers.add("Cookie", sessionId);

                    ObjectMapper mapper = new ObjectMapper();
                    dataSet.setMeasurements(mapper.writeValueAsString(measurements));

                    HttpEntity measurementDatasetEntiy = new HttpEntity(dataSet, headers);
                    ResponseEntity<MeasurementDataset> out = restTemplate.exchange(baseUrl + "/api/measurement-datasets", HttpMethod.POST, measurementDatasetEntiy
                            , MeasurementDataset.class);

                    System.out.println(out.getBody().getId());

                } catch (Exception e) {
                    Log.e("TrackingManager", e.getMessage(), e);
                }

                return null;
            }


        }

        @Override
        public String getName() {
            return "Sending to backend";
        }

        @Override
        public void doOnStart() {
            super.doOnStart();
            DatasetStoreService.store(measurements);
            //new SendToBackendTask().execute();
        }
    }


}
