/*
 * Copyright (c) 2021 NoLimits Enterprises brock@radenso.com
 *
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

import com.nolimits.ds1library.DS1Service;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents an alert: a DS1 alert, a crowd-sourced report or an aircraft
 * state vector.
 */
public class Alert {
  private static final String TAG = "ALERT";

  public static final int ALERT_CLASS_RADAR = 0;
  public static final int ALERT_CLASS_LASER = 1;
  public static final int ALERT_CLASS_SPEED_CAM = 2;
  public static final int ALERT_CLASS_RED_LIGHT_CAM = 3;
  public static final int ALERT_CLASS_USER_MARK = 4;
  public static final int ALERT_CLASS_LOCKOUT = 5;
  public static final int ALERT_CLASS_REPORT = 6;
  public static final int ALERT_CLASS_AIRCRAFT = 7;
  public static final int ALERT_BAND_X = 0;
  public static final int ALERT_BAND_K = 1;
  public static final int ALERT_BAND_KA = 2;
  public static final int ALERT_BAND_POP_K = 3;
  public static final int ALERT_BAND_MRCD = 4;
  public static final int ALERT_BAND_MRCT = 5;
  public static final int ALERT_BAND_GT3 = 6;
  public static final int ALERT_BAND_GT4 = 7;
  public static final int ALERT_DIRECTION_FRONT = 0;
  public static final int ALERT_DIRECTION_SIDE = 1;
  public static final int ALERT_DIRECTION_BACK = 2;

  public int alertClass = 0;
  public int direction = 0;
  public int band = 0;
  public float intensity = 0.0f;
  public float frequency = 0.0f;
  public boolean muted;

  public String type = "";
  public String subType = "";
  public String city = "";
  public String street = "";
  public double longitude;
  public double latitude;
  public int thumbsUp = 0;
  public float distance;
  public int bearing = 0;

  public String transponder;
  public String callSign;
  public boolean onGround;
  public float altitude;
  public String owner;
  public String manufacturer;

  public int announced;
  public float announceDistance = 0.0f;
  public int announceBearing = 0;
  public int priority;

  public Alert() {
  }

  /**
   * Construct an alert from a DS1 alert.
   */
  public static Alert fromDS1Alert(DS1Service.RD_Alert ds1Alert) {
    Alert alert = new Alert();
    Log.i(TAG, String.format(
      "fromDS1Alert id %s type %s freq %f muted %b intensity %d raw_value %d",
      ds1Alert.alert_id, ds1Alert.type, ds1Alert.freq, ds1Alert.muted, ds1Alert.rssi, ds1Alert.raw_value));

    switch(ds1Alert.alert_dir) {
      case ALERT_DIR_FRONT -> alert.direction = ALERT_DIRECTION_FRONT;
      case ALERT_DIR_SIDE -> alert.direction = ALERT_DIRECTION_SIDE;
      default -> alert.direction = ALERT_DIRECTION_BACK;
    }

    alert.intensity = (ds1Alert.rssi + 2) * 10;
    alert.frequency = ds1Alert.freq;
    alert.muted = ds1Alert.muted;

    if(ds1Alert.type.compareTo("X") == 0) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 0;
    }
    else if(0 == ds1Alert.type.compareTo("K")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 1;
    }
    else if(0 == ds1Alert.type.compareTo("KA")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 2;
    }
    else if(0 == ds1Alert.type.compareTo("Laser")) {
      alert.alertClass = ALERT_CLASS_LASER;
    }
    else if(0 == ds1Alert.type.compareTo("POP")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 3;
    }
    else if(0 == ds1Alert.type.compareTo("MRCD")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 4;
    }
    else if(0 == ds1Alert.type.compareTo("MRCT")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 5;
    }
    else if(0 == ds1Alert.type.compareTo("GT3")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 6;
    }
    else if(0 == ds1Alert.type.compareTo("GT4")) {
      alert.alertClass = ALERT_CLASS_RADAR;
      alert.band = 7;
    }

    // Determine the alert priority, laser first, KA band, K band, then
    // other radar alerts and anything else after that
    if(alert.alertClass == ALERT_CLASS_LASER) {
      alert.priority = 0;
    }
    else if(alert.alertClass == ALERT_CLASS_RADAR) {
      if(alert.band == ALERT_BAND_KA) {
        alert.priority = 1;
      }
      else if(alert.band == ALERT_BAND_K || alert.band == ALERT_BAND_POP_K) {
        alert.priority = 2;
      }
      else {
        alert.priority = 3;
      }
    }
    else {
      alert.priority = 4;
    }
    return alert;
  }

  /**
   * Construct an alert from a JSON object representing a crowd-sourced report.
   */
  public static Alert fromReport(Location location, JSONObject jsonReport) {
    Log.i(TAG, "fromReport jsonReport");

    Alert alert = new Alert();
    alert.alertClass = ALERT_CLASS_REPORT;
    try {
      alert.type = jsonReport.getString("type");
    }
    catch(JSONException e) {
    }
    try {
      alert.subType = jsonReport.getString("subtype");
    }
    catch(JSONException e) {
    }
    try {
      alert.city = jsonReport.getString("city");
      // Remove state from the city as it's not useful
      alert.city = alert.city.replaceAll("^(.*)(, [A-Z][A-Z])$", "$1");
    }
    catch(JSONException e) {
    }
    try {
      alert.street = jsonReport.getString("street");
    }
    catch(JSONException e) {
    }
    try {
      alert.thumbsUp = jsonReport.getInt("nThumbsUp");
    }
    catch(JSONException e) {
    }
    try {
      alert.longitude = jsonReport.getJSONObject("location").getDouble("x");
      alert.latitude = jsonReport.getJSONObject("location").getDouble("y");
    }
    catch(JSONException e) {
    }
    Log.i(TAG, String.format("report type %s subtype %s city %s street %s", alert.type, alert.subType, alert.city, alert.street));
    Log.i(TAG, String.format("report location lat %f lng %f", (float)alert.latitude, (float)alert.longitude));

    Location target = new Location(location);
    target.setLongitude(alert.longitude);
    target.setLatitude(alert.latitude);

    // Compute the distance between the vehicle and the report
    float meters = Geospatial.getDistance(location, target);
    alert.distance = Geospatial.toMiles(meters);
    Log.i(TAG, String.format("vehicle location lat %f lng %f", (float)location.getLatitude(), (float)location.getLongitude()));
    Log.i(TAG, String.format("report distance %f", alert.distance));

    // Determine the bearing to the report relative to the vehicle bearing
    float bearingToTarget = Geospatial.getBearing(location, target);
    Log.i(TAG, String.format("report bearing %f", bearingToTarget));
    float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
    Log.i(TAG, String.format("vehicle bearing %f", bearing));
    float relativeBearing = Geospatial.getRelativeBearing(bearing, bearingToTarget);
    Log.i(TAG, String.format("report relative bearing %f", relativeBearing));
    alert.bearing = Geospatial.toHour(relativeBearing);
    Log.i(TAG, String.format("report relative bearing %d o'clock", alert.bearing));

    // Determine the announcement priority, just use the distance for now
    alert.priority = Math.round(meters);
    return alert;
  }

  /**
   * Construct a new alert from an existing report alert and a potentially
   * different location.
   */
  public static Alert fromReport(Location location, Alert report) {
    Log.i(TAG, "fromReport report");

    Alert alert = new Alert();
    alert.alertClass = ALERT_CLASS_REPORT;
    alert.type = report.type;
    alert.subType = report.subType;
    alert.city = report.city;
    alert.street = report.street;
    alert.thumbsUp = report.thumbsUp;
    alert.longitude = report.longitude;
    alert.latitude = report.latitude;

    Location target = new Location(location);
    target.setLongitude(alert.longitude);
    target.setLatitude(alert.latitude);
    Log.i(TAG, String.format("report type %s subtype %s city %s street %s", alert.type, alert.subType, alert.city, alert.street));
    Log.i(TAG, String.format("report location lat %f lng %f", (float)alert.latitude, (float)alert.longitude));

    // Compute the distance between the vehicle and the report
    float meters = Geospatial.getDistance(location, target);
    alert.distance = Geospatial.toMiles(meters);
    Log.i(TAG, String.format("vehicle location lat %f lng %f", (float)location.getLatitude(), (float)location.getLongitude()));
    Log.i(TAG, String.format("report distance %f", alert.distance));

    // Determine the bearing to the report relative to the vehicle bearing
    float bearingToTarget = Geospatial.getBearing(location, target);
    Log.i(TAG, String.format("report bearing %f", bearingToTarget));
    float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
    Log.i(TAG, String.format("vehicle bearing %f", bearing));
    float relativeBearing = Geospatial.getRelativeBearing(bearing, bearingToTarget);
    Log.i(TAG, String.format("report relative bearing %f", relativeBearing));
    alert.bearing = Geospatial.toHour(relativeBearing);
    Log.i(TAG, String.format("report relative bearing %d o'clock", alert.bearing));

    // Determine the announcement priority, just use the distance for now
    alert.priority = Math.round(meters);
    return alert;
  }

  public boolean isDuplicateReport(Alert t2) {
    // A report of the same type within a small distance is considered a
    // duplicate report
    if(alertClass == t2.alertClass && type.equals(t2.type)) {
      Location location = new Location("");
      location.setLatitude(latitude);
      location.setLongitude(longitude);
      Location target = new Location("");
      target.setLatitude(t2.latitude);
      target.setLongitude(t2.longitude);
      float distance = Geospatial.toMiles(Geospatial.getDistance(location, target));
      return distance <= Configuration.REPORTS_DUPLICATE_DISTANCE;
    }
    return false;
  }

  public boolean isSameReport(Alert t2) {
    return alertClass == t2.alertClass && city.equals(t2.city) && street.equals(t2.street) && latitude == t2.latitude && longitude == t2.longitude;
  }

  public boolean shouldAnnounceReport() {
    if(announced == 0) {
      return true;
    }
    if(announceDistance - distance >= Configuration.REPORTS_REMINDER_DISTANCE) {
      return true;
    }
    int bearingDelta = Geospatial.getRelativeBearing(announceBearing, bearing);
    return bearingDelta >= Configuration.REPORTS_REMINDER_BEARING && bearingDelta <= 12 - Configuration.REPORTS_REMINDER_BEARING;
  }

  /**
   * Construct an alert from a JSON object representing an aircraft state vector.
   */
  public static Alert fromAircraft(Location location, JSONArray jsonAircraft, String[] aircraftInfo) {
    Log.i(TAG, "fromAircraft jsonAircraft");

    Alert alert = new Alert();
    alert.alertClass = ALERT_CLASS_AIRCRAFT;
    try {
      alert.transponder = jsonAircraft.getString(0);
    }
    catch(JSONException e) {
    }
    try {
      alert.callSign = jsonAircraft.getString(1);
    }
    catch(JSONException e) {
    }
    try {
      alert.onGround = jsonAircraft.getBoolean(8);
    }
    catch(JSONException e) {
    }
    Log.i(TAG, String.format("aircraft transponder %s callSign %s onGround %b", alert.transponder, alert.callSign, alert.onGround));

    alert.owner = "";
    alert.type = "Aircraft";
    if(aircraftInfo != null) {
      Log.i(TAG, String.format("aircraft info %s", String.join(",", aircraftInfo)));
      alert.owner = aircraftInfo[3];

      String icaoDescription = aircraftInfo[2];
      if(icaoDescription.matches("^[LSAT]..$")) {
        alert.type = "Airplane";
        if(icaoDescription.matches("^..E$")) {
          alert.type = "Drone";
        }
      }
      else if(icaoDescription.matches("^[HG]..$")) {
        alert.type = "Helicopter";
        if(icaoDescription.matches("^..E$")) {
          alert.type = "Drone";
        }
      }

      alert.manufacturer = aircraftInfo[1];
    }
    Log.i(TAG, String.format("aircraft type %s", alert.type));
    Log.i(TAG, String.format("aircraft owner %s", alert.owner));

    try {
      alert.longitude = jsonAircraft.getDouble(5);
      alert.latitude = jsonAircraft.getDouble(6);
    }
    catch(JSONException e) {
    }
    try {
      alert.altitude = (float)jsonAircraft.getDouble(13);
    }
    catch(JSONException e1) {
      try {
        alert.altitude = (float)jsonAircraft.getDouble(7);
      }
      catch(JSONException e2) {
      }
    }
    Log.i(TAG, String.format("aircraft location lat %f lng %f altitude %f", (float)alert.latitude, (float)alert.longitude, alert.altitude));
    Log.i(TAG, String.format("vehicle location lat %f lng %f", (float)location.getLatitude(), (float)location.getLongitude()));

    Location target = new Location(location);
    target.setLongitude(alert.longitude);
    target.setLatitude(alert.latitude);

    // Compute the distance between the vehicle and the report
    float meters = Geospatial.getDistance(location, target);
    alert.distance = Geospatial.toMiles(meters);
    Log.i(TAG, String.format("aircraft distance %f", alert.distance));

    // Determine the bearing to the report relative to the vehicle bearing
    float bearingToTarget = Geospatial.getBearing(location, target);
    Log.i(TAG, String.format("aircraft bearing %f", bearingToTarget));
    float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
    Log.i(TAG, String.format("vehicle bearing %f", bearing));
    float relativeBearing = Geospatial.getRelativeBearing(bearing, bearingToTarget);
    Log.i(TAG, String.format("aircraft relative bearing %f", relativeBearing));
    alert.bearing = Geospatial.toHour(relativeBearing);
    Log.i(TAG, String.format("aircraft relative bearing %d o'clock", alert.bearing));

    // Determine the announcement priority, just use the distance for now
    alert.priority = Math.round(meters);
    return alert;
  }

  /**
   * Construct an alert from a JSON object representing an aircraft state vector.
   */
  public static Alert fromAircraft(Location location, Alert aircraft) {
    Log.i(TAG, "fromAircraft aircraft");

    Alert alert = new Alert();
    alert.alertClass = ALERT_CLASS_AIRCRAFT;
    alert.transponder = aircraft.transponder;
    alert.callSign = aircraft.callSign;
    alert.onGround = aircraft.onGround;
    Log.i(TAG, String.format("aircraft transponder %s callSign %s onGround %b", alert.transponder, alert.callSign, alert.onGround));

    alert.owner = aircraft.owner;
    alert.type = aircraft.type;
    alert.manufacturer = aircraft.manufacturer;
    Log.i(TAG, String.format("aircraft type %s", alert.type));
    Log.i(TAG, String.format("aircraft owner %s", alert.owner));

    alert.longitude = aircraft.longitude;
    alert.latitude = aircraft.latitude;
    alert.altitude = aircraft.altitude;
    Log.i(TAG, String.format("aircraft location lat %f lng %f altitude %f", (float)alert.latitude, (float)alert.longitude, alert.altitude));
    Log.i(TAG, String.format("vehicle location lat %f lng %f", (float)location.getLatitude(), (float)location.getLongitude()));

    Location target = new Location(location);
    target.setLongitude(alert.longitude);
    target.setLatitude(alert.latitude);

    // Compute the distance between the vehicle and the report
    float meters = Geospatial.getDistance(location, target);
    alert.distance = Geospatial.toMiles(meters);
    Log.i(TAG, String.format("aircraft distance %f", alert.distance));

    // Determine the bearing to the report relative to the vehicle bearing
    float bearingToTarget = Geospatial.getBearing(location, target);
    Log.i(TAG, String.format("aircraft bearing %f", bearingToTarget));
    float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
    Log.i(TAG, String.format("vehicle bearing %f", bearing));
    float relativeBearing = Geospatial.getRelativeBearing(bearing, bearingToTarget);
    Log.i(TAG, String.format("aircraft relative bearing %f", relativeBearing));
    alert.bearing = Geospatial.toHour(relativeBearing);
    Log.i(TAG, String.format("aircraft relative bearing %d o'clock", alert.bearing));

    // Determine the announcement priority, just use the distance for now
    alert.priority = Math.round(meters);
    return alert;
  }

  public boolean isSameAircraft(Alert t2) {
    return alertClass == t2.alertClass && transponder.equals(t2.transponder);
  }

  public boolean shouldAnnounceAircraft() {
    if(announced == 0) {
      return true;
    }
    if(announceDistance - distance >= Configuration.AIRCRAFTS_REMINDER_DISTANCE) {
      return true;
    }
    int bearingDelta = Geospatial.getRelativeBearing(announceBearing, bearing);
    return bearingDelta >= Configuration.AIRCRAFTS_REMINDER_BEARING && bearingDelta <= 12 - Configuration.AIRCRAFTS_REMINDER_BEARING;
  }

  public String toDebugString() {
    switch(alertClass) {
      case Alert.ALERT_CLASS_REPORT:
        return "report";
      case Alert.ALERT_CLASS_AIRCRAFT:
        return "aircraft";
      default:
        return "alert";
    }
  }

}
