package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TrafficDataRepository is responsible for fetching, parsing, and managing traffic data.
 * It acts as an intermediary between the data source (API) and the application's data management.
 */
public class TrafficDataRepository {
    /** Tag for logging purposes. Used to identify log messages from this class. */
    private static final String TAG = "TrafficDataRepository";

    /** Context object used for accessing application resources and services. */
    private final Context context;

    /** IncidentDataManager instance used for storing and managing parsed traffic incident data. */
    private final IncidentDataManager incidentDataManager;

    /** Gson instance used for parsing JSON data into Java objects. */
    private final Gson gson;

    /**
     * Constructs a new TrafficDataRepository.
     *
     * This constructor initializes the repository with the necessary context and
     * IncidentDataManager. It also creates a new Gson instance for JSON parsing.
     *
     * @param context The application context, used for accessing application resources
     *                and services.
     * @param incidentDataManager The IncidentDataManager instance used for storing and
     *                            managing parsed traffic incident data.
     */
    public TrafficDataRepository(Context context, IncidentDataManager incidentDataManager) {
        this.context = context;
        this.incidentDataManager = incidentDataManager;
        this.gson = new Gson();
    }

    /**
     * Callback interface for asynchronous traffic data fetching.
     */
    public interface TrafficDataCallback {
        /**
         * Called when data is successfully fetched.
         *
         * @param data The fetched data as a List of Map objects.
         */
        void onDataFetched(List<Map<String, Object>> data);
        /**
         * Called when an error occurs during data fetching.
         *
         * @param error The error message.
         */
        void onError(String error);
    }

    /**
     * Fetches traffic data for all datasets (incident, accident, event).
     *
     * @param callback The callback to handle the result
     */
    public void fetchTrafficData(TrafficDataCallback callback) {
        List<Map<String, Object>> allData = new ArrayList<>();
        fetchDataset("traffic/incident", new TrafficDataCallback() {
            @Override
            public void onDataFetched(List<Map<String, Object>> data) {
                allData.addAll(data);
                incidentDataManager.setIncidentData(data);
                fetchDataset("traffic/accident", new TrafficDataCallback() {
                    @Override
                    public void onDataFetched(List<Map<String, Object>> data) {
                        allData.addAll(data);
                        incidentDataManager.setAccidentData(data);
                        fetchDataset("traffic/event", new TrafficDataCallback() {
                            @Override
                            public void onDataFetched(List<Map<String, Object>> data) {
                                allData.addAll(data);
                                incidentDataManager.setEventData(data);
                                callback.onDataFetched(allData);
                            }
                            @Override
                            public void onError(String error) {
                                callback.onError(error);
                            }
                        });
                    }
                    @Override
                    public void onError(String error) {
                        callback.onError(error);
                    }
                });
            }
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Fetches data for a specific dataset.
     *
     * @param dataset The dataset to fetch
     * @param callback The callback to handle the result
     */
    private void fetchDataset(String dataset, TrafficDataCallback callback) {
        NetTravelDataAPI.getData(context, dataset, new NetTravelDataAPI.DataFetchCallback() {
            @Override
            public void onSuccess(String jsonData) {
                List<Map<String, Object>> data = parseJsonData(jsonData);
                Log.d(TAG, "Parsed data size for " + dataset + ": " + data.size());
                callback.onDataFetched(data);
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Error fetching data for " + dataset + ": " + errorMessage);
                callback.onError(errorMessage);
            }
        });
    }

    /**
     * Parses JSON data into a List of Map objects.
     *
     * @param jsonData The JSON string to parse
     * @return A List of Map objects representing the parsed data
     */
    private List<Map<String, Object>> parseJsonData(String jsonData) {
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> data = gson.fromJson(jsonData, type);
        Log.d(TAG, "Parsed JSON data: " + data.size() + " items");
        return data;
    }

    /**
     * Retrieves nearby traffic items based on geographical coordinates.
     *
     * @param latitude The latitude of the center point
     * @param longitude The longitude of the center point
     * @param radius The radius in meters to search within
     * @return A list of nearby traffic items
     */
    public List<Map<String, Object>> getNearbyTrafficItems(double latitude, double longitude, double radius) {
        return incidentDataManager.getNearbyTrafficItems(latitude, longitude, radius);
    }
}