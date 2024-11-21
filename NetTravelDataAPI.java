package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.util.Base64;
import javax.net.ssl.HttpsURLConnection;

/**
 * NetTravelDataAPI handles the communication with the NetTravelData API.
 * It provides methods to fetch traffic data and save it locally.
 */
public class NetTravelDataAPI {
  /**
   * Tag for logging purposes. Used to identify log messages from this class.
   */
  private static final String TAG = "NetTravelDataAPI";

  /**
   * Base URL for the NetTravelData API.
   */
  private static final String BASE_URL = "https://www.netraveldata.co.uk/api/v2/";

  /**
   * Username for API authentication.
   */
  private static final String USERNAME = "pinglinhsieh";

  /**
   * Password for API authentication.
   */
  private static final String PASSWORD = "MANghjkl5672";

  /**
   * Filename for storing traffic incident data locally.
   */
  private static final String INCIDENT_OUTPUT_FILE = "traffic_incident.json";

  /**
   * Filename for storing traffic accident data locally.
   */
  private static final String ACCIDENT_OUTPUT_FILE = "traffic_accident.json";

  /**
   * Filename for storing traffic event data locally.
   */
  private static final String EVENT_OUTPUT_FILE = "traffic_event.json";

  public interface DataFetchCallback {
    /**
     * Called when data is successfully fetched.
     *
     * @param data The fetched data as a String.
     */
    void onSuccess(String data);
    /**
     * Called when an error occurs during data fetching.
     *
     * @param errorMessage The error message.
     */
    void onError(String errorMessage);
  }

  /**
   * Fetches data from the API for a given dataset.
   * @param context The application context
   * @param dataset The dataset to fetch (e.g., "traffic/incident")
   * @param callback The callback to handle the result
   */
  public static void getData(Context context, String dataset, DataFetchCallback callback) {
    new Thread(() -> {
      HttpsURLConnection connection = null;
      try {
        connection = setupConnection(dataset);
        String responseData = fetchData(connection);
        Log.d(TAG, "Fetched data: " + responseData.substring(0, Math.min(responseData.length(), 100)) + "..."); // Log first 100 chars
        saveDataToFile(context, responseData, dataset);
        callback.onSuccess(responseData);
      } catch (Exception e) {
        Log.e(TAG, "Error fetching data", e);
        callback.onError(e.getMessage());
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
      }
    }).start();
  }

  /**
   * Sets up the HTTPS connection with proper authentication.
   * @param dataset The dataset to fetch
   * @return The configured HttpsURLConnection
   * @throws IOException If an I/O error occurs
   */
  private static HttpsURLConnection setupConnection(String dataset) throws IOException {
    URL url = new URL(BASE_URL + dataset);
    HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    String encodedCredentials = Base64.encodeToString(
            (USERNAME + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8),
            Base64.NO_WRAP
    );
    connection.setRequestProperty("Authorization", "Basic " + encodedCredentials);
    return connection;
  }

  /**
   * Fetches data from the established connection.
   * @param connection The HttpsURLConnection to fetch data from
   * @return The fetched data as a String
   * @throws IOException If an I/O error occurs
   */
  private static String fetchData(HttpsURLConnection connection) throws IOException {
    int responseCode = connection.getResponseCode();
    Log.d(TAG, "Response Code: " + responseCode);
    if (responseCode != HttpsURLConnection.HTTP_OK) {
      throw new IOException("HTTP error code: " + responseCode);
    }

    StringBuilder response = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        response.append(line);
      }
    }
    return response.toString();
  }

  /**
   * Saves the fetched data to a local file.
   * @param context The application context
   * @param data The data to save
   * @param dataset The dataset type (used to determine the filename)
   */
  private static void saveDataToFile(Context context, String data, String dataset) {
    String fileName;
    switch (dataset) {
      case "traffic/incident":
        fileName = INCIDENT_OUTPUT_FILE;
        break;
      case "traffic/accident":
        fileName = ACCIDENT_OUTPUT_FILE;
        break;
      case "traffic/event":
        fileName = EVENT_OUTPUT_FILE;
        break;
      default:
        fileName = "unknown_data.json";
    }

    try (FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
      fos.write(data.getBytes(StandardCharsets.UTF_8));
      Log.d(TAG, "Saved data to file: " + fileName);
    } catch (Exception e) {
      Log.e(TAG, "Error saving data to file", e);
    }
  }
}