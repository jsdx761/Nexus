/*
 * Copyright (c) 2023 jsdx761
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.jsd.x761.nexus;

import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * An asynchronous task that fetches crowdsourced reports from a server.
 */
public class ReportsFetchTask implements Runnable {
  private static final String TAG = "REPORTS_FETCH_TASK";

  private final String mSourceURL;
  private final Location mLocation;

  public ReportsFetchTask(String sourceURL, Location location) {
    // The source URL is configured from the app preferences
    mSourceURL = sourceURL;
    mLocation = location;
  }

  @Override
  public void run() {
    Log.i(TAG, String.format("run lat %f lng %f", (float)mLocation.getLatitude(), (float)mLocation.getLongitude()));

    JSONObject json = null;
    if(Configuration.DEBUG_INJECT_TEST_REPORTS) {
      // Support using test reports to help debugging without having to
      // connect to an actual server everytime
      try {
        Log.i(TAG, "using test reports");
        json = new JSONObject(Configuration.DEBUG_TEST_REPORTS);
      }
      catch(JSONException e) {
        Log.e(TAG, "JSONException reading reports", e);
        onDone(null);
        return;
      }
    }
    else {
      // Connect to the configured server and fetch crowdsourced reports
      // within the configured max distance
      HttpURLConnection connection = null;
      BufferedReader reader = null;
      try {
        float distance = Geospatial.toMeters(Configuration.REPORTS_MAX_DISTANCE);
        Location bottom = Geospatial.getDestination(mLocation, distance, 180f);
        Location left = Geospatial.getDestination(mLocation, distance, 270f);
        Location top = Geospatial.getDestination(mLocation, distance, 0f);
        Location right = Geospatial.getDestination(mLocation, distance, 90f);
        URL url = new URL(String.format(
          "%s/rtserver/web/TGeoRSS?bottom=%f&left=%f&top=%f&right=%f&ma=200&mj=200&mu=20&types=alerts",
          mSourceURL,
          (float)bottom.getLatitude(),
          (float)left.getLongitude(),
          (float)top.getLatitude(),
          (float)right.getLongitude()));

        if(Configuration.DEBUG) {
          // Double check that the computed area uses the correct distance
          float topDistance = Geospatial.toMiles(Geospatial.getDistance(mLocation, top));
          float leftDistance = Geospatial.toMiles(Geospatial.getDistance(mLocation, left));
          Log.i(TAG, String.format("report area distance top %f left %f", topDistance, leftDistance));
        }

        // Connect to the server and fetch the reports in JSON form
        Log.i(TAG, String.format("URL.openConnection %s", url.toExternalForm()));
        connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();

        InputStream inputStream = connection.getInputStream();
        StringBuilder buffer = new StringBuilder();
        if(inputStream == null) {
          onDone(null);
          return;
        }

        reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while((line = reader.readLine()) != null) {
          buffer.append(line).append("\n");
        }

        if(buffer.length() == 0) {
          onDone(null);
          return;
        }

        String jsonString = buffer.toString();
        json = new JSONObject(jsonString);
      }
      catch(IOException | JSONException e) {
        Log.e(TAG, "Exception reading JSON from URL", e);
        onDone(null);
        return;
      }
      finally {
        if(connection != null) {
          connection.disconnect();
        }
        if(reader != null) {
          try {
            reader.close();
          }
          catch(IOException e) {
            Log.e(TAG, "IOException closing reader", e);
          }
        }
      }
    }

    List<Alert> reports = new ArrayList<>();
    try {
      JSONArray jsonReports = json.optJSONArray("alerts");
      if(jsonReports != null) {
        for(int i = 0; i < jsonReports.length(); i++) {
          JSONObject jsonReport = jsonReports.getJSONObject(i);
          try {
            // Only keep reports of relevant types
            String type = jsonReport.getString("type");
            Log.i(TAG, String.format("report type %s", type));
            if("POLICE".equals(type) || "ACCIDENT".equals(type)) {
              Alert report = Alert.fromReport(mLocation, jsonReport);
              reports.add(report);
            }
          }
          catch(JSONException e) {
            Log.e(TAG, "JSONException processing report", e);
          }
        }
      }
    }
    catch(JSONException e) {
      Log.e(TAG, "JSONException processing reports", e);
      onDone(null);
      return;
    }
    onDone(reports);
  }

  protected void onDone(List<Alert> reports) {
    if(reports != null) {
      Log.i(TAG, String.format("onDone %d reports", reports.size()));
    }
    else {
      Log.i(TAG, "onDone null reports");
    }
  }
}
