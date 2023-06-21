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
 * Represents a threat: a DS1 alert, a crowd-sourced report or an aircraft
 * report.
 */
public class Threat {
  private static final String TAG = "THREAT";

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

  public int threatClass = 0;

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
  public int hour = 0;

  public String transponder;
  public String callSign;
  public boolean onGround;
  public float altitude;
  public String owner;
  public String manufacturer;

  public int announced = 0;
  public int priority;

  public Threat() {
  }

  /**
   * Construct a threat from a DS1 alert.
   */
  public static Threat fromDS1Alert(DS1Service.RD_Alert ds1Alert) {
    Threat threat = new Threat();
    Log.i(TAG, String.format(
      "fromDS1Alert id %s type %s freq %f muted %b intensity %d raw_value %d",
      ds1Alert.alert_id, ds1Alert.type, ds1Alert.freq, ds1Alert.muted, ds1Alert.rssi, ds1Alert.raw_value));

    switch(ds1Alert.alert_dir) {
      case ALERT_DIR_FRONT -> threat.direction = ALERT_DIRECTION_FRONT;
      case ALERT_DIR_SIDE -> threat.direction = ALERT_DIRECTION_SIDE;
      default -> threat.direction = ALERT_DIRECTION_BACK;
    }

    threat.intensity = (ds1Alert.rssi + 2) * 10;
    threat.frequency = ds1Alert.freq;
    threat.muted = ds1Alert.muted;

    if(ds1Alert.type.compareTo("X") == 0) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 0;
    }
    else if(0 == ds1Alert.type.compareTo("K")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 1;
    }
    else if(0 == ds1Alert.type.compareTo("KA")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 2;
    }
    else if(0 == ds1Alert.type.compareTo("Laser")) {
      threat.threatClass = ALERT_CLASS_LASER;
    }
    else if(0 == ds1Alert.type.compareTo("POP")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 3;
    }
    else if(0 == ds1Alert.type.compareTo("MRCD")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 4;
    }
    else if(0 == ds1Alert.type.compareTo("MRCT")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 5;
    }
    else if(0 == ds1Alert.type.compareTo("GT3")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 6;
    }
    else if(0 == ds1Alert.type.compareTo("GT4")) {
      threat.threatClass = ALERT_CLASS_RADAR;
      threat.band = 7;
    }

    // Determine the threat priority, laser first, KA band, K band, then
    // other radar alerts and anything else after that
    if(threat.threatClass == ALERT_CLASS_LASER) {
      threat.priority = 0;
    }
    else if(threat.threatClass == ALERT_CLASS_RADAR) {
      if(threat.band == ALERT_BAND_KA) {
        threat.priority = 1;
      }
      else if(threat.band == ALERT_BAND_K || threat.band == ALERT_BAND_POP_K) {
        threat.priority = 2;
      }
      else {
        threat.priority = 3;
      }
    }
    else {
      threat.priority = 4;
    }
    return threat;
  }

  /**
   * Construct a threat from a JSON object representing a crowd-sourced report.
   */
  public static Threat fromReport(Location location, JSONObject jsonReport) {
    Log.i(TAG, "fromReport");

    Threat threat = new Threat();
    threat.threatClass = ALERT_CLASS_REPORT;
    try {
      threat.type = jsonReport.getString("type");
    }
    catch(JSONException e) {
    }
    try {
      threat.subType = jsonReport.getString("subtype");
    }
    catch(JSONException e) {
    }
    try {
      threat.city = jsonReport.getString("city");
      // Remove state from the city as it's not useful
      threat.city = threat.city.replaceAll("^(.*)(, [A-Z][A-Z])$", "$1");
    }
    catch(JSONException e) {
    }
    try {
      threat.street = jsonReport.getString("street");
    }
    catch(JSONException e) {
    }
    try {
      threat.thumbsUp = jsonReport.getInt("nThumbsUp");
    }
    catch(JSONException e) {
    }
    try {
      threat.longitude = jsonReport.getJSONObject("location").getDouble("x");
      threat.latitude = jsonReport.getJSONObject("location").getDouble("y");
    }
    catch(JSONException e) {
    }
    Log.i(TAG, String.format("report type %s subtype %s city %s street %s", threat.type, threat.subType, threat.city, threat.street));
    Log.i(TAG, String.format("report location lat %f lng %f", (float)threat.latitude, (float)threat.longitude));

    Location target = new Location(location);
    target.setLongitude(threat.longitude);
    target.setLatitude(threat.latitude);

    // Compute the distance between the vehicle and the report
    float meters = Geospatial.getDistance(location, target);
    threat.distance = Geospatial.toMiles(meters);
    Log.i(TAG, String.format("vehicle location lat %f lng %f", (float)location.getLatitude(), (float)location.getLongitude()));
    Log.i(TAG, String.format("report distance %f", threat.distance));

    // Determine the bearing to the report relative to the vehicle bearing
    float bearingToTarget = Geospatial.getBearing(location, target);
    Log.i(TAG, String.format("report bearing %f", bearingToTarget));
    float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
    Log.i(TAG, String.format("vehicle bearing %f", bearing));
    float relativeBearing = Geospatial.getRelativeBearing(bearing, bearingToTarget);
    Log.i(TAG, String.format("report relative bearing %f", relativeBearing));
    threat.hour = Geospatial.toHour(relativeBearing);
    Log.i(TAG, String.format("report relative bearing %d o'clock", threat.hour));

    // Determine the announcement priority, just use the distance for now
    threat.priority = Math.round(meters);
    return threat;
  }

  public boolean isDuplicateReport(Threat t2) {
    // A report of the same type within a small distance is considered a
    // duplicate report
    if(threatClass == t2.threatClass && type.equals(t2.type)) {
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

  public boolean isSameReport(Threat t2) {
    return threatClass == t2.threatClass && city.equals(t2.city) && street.equals(t2.street) && latitude == t2.latitude && longitude == t2.longitude;
  }

  public boolean isSameAircraft(Threat t2) {
    return threatClass == t2.threatClass && transponder.equals(t2.transponder);
  }

  public String toDebugString() {
    switch(threatClass) {
      case Threat.ALERT_CLASS_REPORT:
        return "report";
      case Threat.ALERT_CLASS_AIRCRAFT:
        return "aircraft";
      default:
        return "alert";
    }
  }

  /**
   * Construct a threat from a JSON object representing an aircraft state vector.
   */
  public static Threat fromAircraft(Location location, JSONArray jsonAircraft, String[] aircraftInfo) {
    Log.i(TAG, "fromReport");

    Threat threat = new Threat();
    threat.threatClass = ALERT_CLASS_AIRCRAFT;
    try {
      threat.transponder = jsonAircraft.getString(0);
    }
    catch(JSONException e) {
    }
    try {
      threat.callSign = jsonAircraft.getString(1);
    }
    catch(JSONException e) {
    }
    try {
      threat.onGround = jsonAircraft.getBoolean(8);
    }
    catch(JSONException e) {
    }
    Log.i(TAG, String.format("aircraft transponder %s callSign %s onGround %b", threat.transponder, threat.callSign, threat.onGround));

    threat.owner = "";
    threat.type = "Aircraft";
    if(aircraftInfo != null) {
      Log.i(TAG, String.format("aircraft info %s", String.join(",", aircraftInfo)));
      threat.owner = aircraftInfo[3];

      String icaoDescription = aircraftInfo[2];
      if(icaoDescription.matches("^[LSAT]..$")) {
        threat.type = "Airplane";
        if(icaoDescription.matches("^..E$")) {
          threat.type = "Drone";
        }
      }
      else if(icaoDescription.matches("^[HG]..$")) {
        threat.type = "Helicopter";
        if(icaoDescription.matches("^..E$")) {
          threat.type = "Drone";
        }
      }

      threat.manufacturer = aircraftInfo[1];
    }
    Log.i(TAG, String.format("aircraft type %s", threat.type));
    Log.i(TAG, String.format("aircraft owner %s", threat.owner));

    try {
      threat.longitude = jsonAircraft.getDouble(5);
      threat.latitude = jsonAircraft.getDouble(6);
    }
    catch(JSONException e) {
    }
    try {
      threat.altitude = (float)jsonAircraft.getDouble(13);
    }
    catch(JSONException e1) {
      try {
        threat.altitude = (float)jsonAircraft.getDouble(7);
      }
      catch(JSONException e2) {
      }
    }
    Log.i(TAG, String.format("aircraft location lat %f lng %f altitude %f", (float)threat.latitude, (float)threat.longitude, threat.altitude));
    Log.i(TAG, String.format("vehicle location lat %f lng %f", (float)location.getLatitude(), (float)location.getLongitude()));

    Location target = new Location(location);
    target.setLongitude(threat.longitude);
    target.setLatitude(threat.latitude);

    // Compute the distance between the vehicle and the report
    float meters = Geospatial.getDistance(location, target);
    threat.distance = Geospatial.toMiles(meters);
    Log.i(TAG, String.format("aircraft distance %f", threat.distance));

    // Determine the bearing to the report relative to the vehicle bearing
    float bearingToTarget = Geospatial.getBearing(location, target);
    Log.i(TAG, String.format("aircraft bearing %f", bearingToTarget));
    float bearing = location.hasBearing() ? location.getBearing() : 0.0f;
    Log.i(TAG, String.format("vehicle bearing %f", bearing));
    float relativeBearing = Geospatial.getRelativeBearing(bearing, bearingToTarget);
    Log.i(TAG, String.format("aircraft relative bearing %f", relativeBearing));
    threat.hour = Geospatial.toHour(relativeBearing);
    Log.i(TAG, String.format("aircraft relative bearing %d o'clock", threat.hour));

    // Determine the announcement priority, just use the distance for now
    threat.priority = Math.round(meters);
    return threat;
  }
}
