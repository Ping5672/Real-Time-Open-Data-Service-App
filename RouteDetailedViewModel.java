package com.example.myapplication;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RouteDetailedViewModel handles the business logic for route calculations and incident data retrieval.
 * It communicates with Google Maps API for directions and TrafficDataRepository for incident data.
 */
public class RouteDetailedViewModel extends AndroidViewModel {

    /** Tag for logging purposes. */
    private static final String TAG = "RouteDetailedViewModel";

    /** LiveData to hold the list of LatLng points representing the route path. */
    private final MutableLiveData<List<LatLng>> routePath = new MutableLiveData<>();

    /** LiveData to hold the estimated time for the route. */
    private final MutableLiveData<String> estimatedTime = new MutableLiveData<>();

    /** LiveData to hold any error messages during route calculation or data retrieval. */
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    /** LiveData to hold the list of incident data along the route. */
    private final MutableLiveData<List<Map<String, Object>>> incidentData = new MutableLiveData<>();

    /** Volley RequestQueue for making network requests. */
    private final RequestQueue requestQueue;

    /** Repository for fetching traffic data. */
    private final TrafficDataRepository trafficDataRepository;

    /**
     * Constructs a new RouteDetailedViewModel.
     *
     * This constructor initializes the ViewModel with a Volley RequestQueue for making
     * network requests and a TrafficDataRepository for fetching traffic data. It also
     * sets up MutableLiveData objects for route path, estimated time, error messages,
     * and incident data.
     *
     * @param application The application that this ViewModel is attached to,
     *                    providing access to application-level resources.
     * @param trafficDataRepository The repository for fetching traffic data,
     *                              used to load incident information along the route.
     */
    public RouteDetailedViewModel(Application application, TrafficDataRepository trafficDataRepository) {
        super(application);
        this.requestQueue = Volley.newRequestQueue(application);
        this.trafficDataRepository = trafficDataRepository;
    }

    /**
     * Returns a LiveData object containing the list of LatLng points representing the route path.
     *
     * @return LiveData<List<LatLng>> The route path.
     */
    public LiveData<List<LatLng>> getRoutePath() {
        return routePath;
    }

    /**
     * Returns a LiveData object containing the estimated time for the route.
     *
     * @return LiveData<String> The estimated time.
     */
    public LiveData<String> getEstimatedTime() {
        return estimatedTime;
    }

    /**
     * Returns a LiveData object containing any error messages.
     *
     * @return LiveData<String> The error message.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Returns a LiveData object containing the list of incident data.
     *
     * @return LiveData<List<Map<String, Object>>> The incident data.
     */
    public LiveData<List<Map<String, Object>>> getIncidentData() {
        return incidentData;
    }

    /**
     * Calculates directions between two points using Google Maps API.
     *
     * @param sourceLatLng The starting point.
     * @param destinationLatLng The destination point.
     */
    public void calculateDirections(LatLng sourceLatLng, LatLng destinationLatLng) {
        String url = buildDirectionsUrl(sourceLatLng, destinationLatLng);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        parseDirectionsResponse(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        errorMessage.postValue("Error parsing directions");
                    }
                }, error -> errorMessage.postValue("Error fetching directions"));

        requestQueue.add(jsonObjectRequest);
    }

    /**
     * Builds the URL for the Google Maps Directions API request.
     *
     * @param sourceLatLng The starting point.
     * @param destinationLatLng The destination point.
     * @return The URL string for the API request.
     */
    private String buildDirectionsUrl(LatLng sourceLatLng, LatLng destinationLatLng) {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=" + sourceLatLng.latitude + "," + sourceLatLng.longitude +
                "&destination=" + destinationLatLng.latitude + "," + destinationLatLng.longitude +
                "&key=" + getApplication().getString(R.string.my_map_api_key);
    }

    /**
     * Parses the JSON response from the Directions API.
     * Extracts the route path and estimated time.
     *
     * @param response The JSON response from the API.
     * @throws JSONException If there's an error parsing the JSON.
     */
    private void parseDirectionsResponse(JSONObject response) throws JSONException {
        JSONArray routes = response.getJSONArray("routes");
        JSONObject route = routes.getJSONObject(0);
        JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
        String encodedPath = overviewPolyline.getString("points");
        List<LatLng> path = decodePolyline(encodedPath);
        routePath.postValue(path);

        JSONArray legs = route.getJSONArray("legs");
        JSONObject leg = legs.getJSONObject(0);
        JSONObject duration = leg.getJSONObject("duration");
        String time = duration.getString("text");
        estimatedTime.postValue(time);
    }

    /**
     * Loads incident data from the TrafficDataRepository.
     */
    public void loadIncidentData() {
        trafficDataRepository.fetchTrafficData(new TrafficDataRepository.TrafficDataCallback() {
            @Override
            public void onDataFetched(List<Map<String, Object>> data) {
                Log.d(TAG, "Loaded traffic data: " + data.size() + " total items");
                if (data.isEmpty()) {
                    Log.w(TAG, "No traffic data loaded");
                }
                incidentData.postValue(data);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error fetching traffic data: " + error);
                errorMessage.postValue("Error loading traffic data: " + error);
            }
        });
    }

    /**
     * Decodes an encoded polyline string into a list of LatLng points.
     * This method uses the polyline encoding algorithm used by Google Maps.
     *
     * @param encoded The encoded polyline string
     * @return A list of LatLng points representing the decoded polyline
     */
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            // Decode latitude
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            // Decode longitude
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            // Create LatLng object and add to list
            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}