package com.example.myapplication;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The IncidentDataManager class is responsible for managing and processing various types of traffic incident data.
 * It stores and provides access to incident, accident, and event data, and offers functionality to
 * retrieve nearby traffic items based on geographical coordinates.
 */
public class IncidentDataManager {
    /**
     * Tag for logging purposes. Used to identify log messages from this class.
     */
    private static final String TAG = "IncidentDataManager";

    /**
     * List to store traffic incident data.
     * Each Map in the list represents a single incident, with keys for various attributes of the incident.
     */
    private List<Map<String, Object>> incidentData;

    /**
     * List to store traffic accident data.
     * Each Map in the list represents a single accident, with keys for various attributes of the accident.
     */
    private List<Map<String, Object>> accidentData;

    /**
     * List to store traffic event data.
     * Each Map in the list represents a single event, with keys for various attributes of the event.
     */
    private List<Map<String, Object>> eventData;



    /**
     * Constructs a new IncidentDataManager.
     *
     * This constructor initializes empty lists for each type of traffic data:
     * incidents, accidents, and events. These lists will be populated with
     * data as it becomes available.
     */
    public IncidentDataManager() {
        incidentData = new ArrayList<>();
        accidentData = new ArrayList<>();
        eventData = new ArrayList<>();
    }

    /**
     * Sets the incident data and logs the number of items.
     *
     * @param data A List of Map objects containing incident data.
     */
    public void setIncidentData(List<Map<String, Object>> data) {
        this.incidentData = data;
        Log.d(TAG, "Set incident data: " + data.size() + " items");
    }

    /**
     * Sets the accident data and logs the number of items.
     *
     * @param data A List of Map objects containing accident data.
     */
    public void setAccidentData(List<Map<String, Object>> data) {
        this.accidentData = data;
        Log.d(TAG, "Set accident data: " + data.size() + " items");
    }

    /**
     * Sets the event data and logs the number of items.
     *
     * @param data A List of Map objects containing event data.
     */
    public void setEventData(List<Map<String, Object>> data) {
        this.eventData = data;
        Log.d(TAG, "Set event data: " + data.size() + " items");
    }

    /**
     * Retrieves traffic items within a specified radius of a given location.
     *
     * @param latitude The latitude of the center point.
     * @param longitude The longitude of the center point.
     * @param radius The radius in meters to search within.
     * @return A List of Map objects representing nearby traffic items.
     */
    public List<Map<String, Object>> getNearbyTrafficItems(double latitude, double longitude, double radius) {
        List<Map<String, Object>> nearbyItems = new ArrayList<>();
        List<Map<String, Object>> allTrafficData = new ArrayList<>();
        allTrafficData.addAll(incidentData);
        allTrafficData.addAll(accidentData);
        allTrafficData.addAll(eventData);

        Log.d(TAG, "Total items in allTrafficData: " + allTrafficData.size());

        for (Map<String, Object> item : allTrafficData) {
            try {
                double itemLat = 0;
                double itemLng = 0;

                // Extract latitude and longitude from the item
                if (item.containsKey("point")) {
                    Map<String, Object> point = (Map<String, Object>) item.get("point");
                    if (point != null) {
                        itemLat = getDoubleValue(point.get("latitude"));
                        itemLng = getDoubleValue(point.get("longitude"));
                    }
                } else if (item.containsKey("latitude") && item.containsKey("longitude")) {
                    itemLat = getDoubleValue(item.get("latitude"));
                    itemLng = getDoubleValue(item.get("longitude"));
                }

                // Skip items with invalid coordinates
                if (itemLat == 0 && itemLng == 0) {
                    Log.w(TAG, "Invalid or missing coordinates for item: " + item);
                    continue;
                }

                // Calculate distance between the given point and the item
                float[] results = new float[1];
                android.location.Location.distanceBetween(latitude, longitude, itemLat, itemLng, results);

                // Add item to nearbyItems if it's within the specified radius
                if (results[0] <= radius) {
                    nearbyItems.add(item);
                    Log.d(TAG, "Added nearby item: " + item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing item: " + item, e);
            }
        }

        Log.d(TAG, "Found " + nearbyItems.size() + " nearby items");
        return nearbyItems;
    }

    /**
     * Helper method to safely convert an Object to a double value.
     * @param value The object to convert
     * @return The double value, or 0 if conversion fails
     */
    private double getDoubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing double value: " + value, e);
            }
        }
        return 0;
    }
}