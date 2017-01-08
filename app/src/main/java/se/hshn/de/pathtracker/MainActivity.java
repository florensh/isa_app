package se.hshn.de.pathtracker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpAuthentication;
import org.springframework.http.HttpBasicAuthentication;
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
import java.util.Set;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private final ReceivingDataState RECEIVING_DATA_STATE = new ReceivingDataState();
    private final SearchForStoreState SEARCHING_STORES_STATE = new SearchForStoreState();
    private final NoStoresInBackend NO_STORES_IN_BACKEND = new NoStoresInBackend();
    private final WaitingForTracking WAITING_FOR_TRACKING  = new WaitingForTracking();
    private final TrackingState TRACKING_STATE = new TrackingState();
    private AppState state;
    private Location currentLocation = null;

    private TextView statusTextView;
    private TextView gpsTextView;
    private TextView storeTextView;
    private TextView currentPathTextView;


    private final float TOLERANCE = 0.0001f;

    private List<Store> storeList = null;
    private String sessionId = null;

    private Store currentStore = null;
    private String userId = null;
    private Long storeVisitedAt = null;

    private LocationManager locationManager;
    private String provider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        this.statusTextView = (TextView) findViewById(R.id.status);
        this.gpsTextView = (TextView) findViewById(R.id.gps);
        this.storeTextView = (TextView) findViewById(R.id.store);
        this.currentPathTextView = (TextView) findViewById(R.id.path);

        this.state = RECEIVING_DATA_STATE;
        this.state.doOnStart();

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        this.userId = wInfo.getMacAddress();


        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }



        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        getSystemService(LOCATION_SERVICE);
        boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);


        // check if enabled and if not send user to the GSP settings
        // Better solution would be to display a dialog and suggesting to
        // go to the settings
        if (!enabled) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }


        // Define the criteria how to select the locatioin provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);


        Location location = locationManager.getLastKnownLocation(provider);
        // Initialize the location fields
        if (location != null) {
            System.out.println("Provider " + provider + " has been selected.");
            onLocationChanged(location);
        } else {
            gpsTextView.setText("Location not available");
        }



    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("new location");
        this.state.handleGpsUpdate(location);
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    /* Remove the locationlistener updates when Activity is paused */
    @Override
    protected void onPause() {
        super.onPause();

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return  ;
        }

        locationManager.removeUpdates(this);
    }




    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    Store resolveStore(Location location){
        for(Store s : storeList){
            if(s.getLat()>= location.getLatitude()-TOLERANCE && s.getLat() <= location.getLatitude() + TOLERANCE
                    && s.getLon() >= location.getLongitude() - TOLERANCE && s.getLon() <= location.getLongitude()+ TOLERANCE){
                return s;
            }
        }
        return null;
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
                String baseUrl = "https://calm-coast-80282.herokuapp.com";

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


                String authURL = baseUrl+"/api/authentication";
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

                ResponseEntity res = restTemplate.exchange(baseUrl+"/api/stores", HttpMethod.GET, storesEntiy, Store[].class);

                Store[] stores = (Store[])res.getBody();




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
            if(state instanceof ReceivingDataState){


                ReceivingDataState rState = (ReceivingDataState) state;
                rState.receivedData(stores);
            }

        }

    }

    abstract class AbstractAppState implements AppState{
        public void changeStatus(AppState nextState){
            state = nextState;
            nextState.doOnStart();
        }

        public void doOnStart(){
            statusTextView.setText(this.getName());
        }

        @Override
        public void handleGpsUpdate(Location location) {
            currentLocation = location;
            gpsTextView.setText("lat: " + location.getLatitude() + " lon: " + location.getLongitude() + " (" + location.getAccuracy()+")");

        }
    }

    class ReceivingDataState extends AbstractAppState implements AppState{

        @Override
        public String getName() {
            return "Receiving data";
        }

        @Override
        public void doOnStart() {
            super.doOnStart();
            new HttpRequestTask().execute();
        }

        public void receivedData(List<Store> data){
            System.out.println("Got " + data.size() + " stores from backend!");

            if(data != null &&   !data.isEmpty()){
                storeList = data;
                changeStatus(SEARCHING_STORES_STATE);
            }else{
                changeStatus(NO_STORES_IN_BACKEND);
            }


        }
    }

    class NoStoresInBackend extends AbstractAppState implements AppState{



        @Override
        public String getName() {
            return "No Stores in Backend";
        }
    }

    class SearchForStoreState extends AbstractAppState implements AppState{




        @Override
        public void doOnStart() {
            super.doOnStart();
            storeVisitedAt = null;
            if(currentLocation != null && System.currentTimeMillis() < currentLocation.getTime() + 10000){
                findStore(currentLocation);
            }
        }


        @Override
        public void handleGpsUpdate(Location location) {
            findStore(location);

        }

        private synchronized void findStore(Location location) {
            Store s = resolveStore(location);
            if (s != null) {
                currentStore = s;
                storeVisitedAt = location.getTime();
                storeTextView.setText(s.getName());
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
            if(s != null && s.getId().equals(currentStore.getId())){
                if(location.getTime() > storeVisitedAt + 5000 && location.getAccuracy() < 10.0f){
                    changeStatus(TRACKING_STATE);
                }
            } else{
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
