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

/**
 * Various internal configuration properties.
 */
public class Configuration {

  public static final long SPLASH_TIMER = 2000;
  public static final String DS1_SERVICE_SCAN_NAME = "DS1@E1";
  public static final long DS1_SERVICE_SCAN_TIMER = 10000;
  public static final long DS1_SERVICE_CONNECT_WAIT_TIMER = 3000;
  public static final long DS1_SERVICE_SETUP_TIMER = 1000;
  public static final boolean ENABLE_ALERTS = true;
  public static final long ALERTS_CLEAR_TIMER = 6000;
  public static final long ABANDON_AUDIO_TIMER = 0;
  public static final int ALERTS_MAX_ANNOUNCE_COUNT = 3;
  public static final int AUDIO_ADJUST_RAISE_COUNT = 0;
  public static final float AUDIO_SPEECH_PITCH = 0.95f;
  public static final float AUDIO_SPEECH_RATE = 1.1f;
  public static final long AUDIO_EARCON_TIMER = 190;
  public static final long CURRENT_LOCATION_TIMER = 4000;
  public static final boolean USE_COMPUTED_LOCATION_BEARING = true;
  public static final float COMPUTED_BEARING_DISTANCE_THRESHOLD = 20.0f;
  public static final boolean ENABLE_REPORTS = true;
  public static final long REPORTS_INITIAL_CHECK_TIMER = 1000;
  public static final long REPORTS_CHECK_TIMER = 24000;
  public static final float REPORTS_DUPLICATE_DISTANCE = 0.2f;
  public static final boolean ENABLE_AIRCRAFTS = true;
  public static final long AIRCRAFTS_INITIAL_CHECK_TIMER = 1000;
  public static final long AIRCRAFTS_ANONYMOUS_CHECK_TIMER = 240000;
  public static final long AIRCRAFTS_AUTHENTICATED_CHECK_TIMER = 24000;

  public static final boolean DEBUG = false;
  public static final boolean DEBUG_USE_NULL_DS1_SERVICE = DEBUG;
  public static final long DEBUG_NULL_DS1_SERVICE_SCAN_TIMER = 2000;
  public static final boolean DEBUG_INJECT_TEST_ALERTS = DEBUG;
  public static final int DEBUG_TEST_ALERTS_REPEAT_COUNT = 3;
  public static final long DEBUG_TEST_ALERTS_REPEAT_TIMER = 50;
  public static final boolean DEBUG_INJECT_TEST_BACKGROUND_ALERTS = false;
  public static final long DEBUG_TEST_BACKGROUND_ALERTS_TIMER = 20000;
  public static final boolean DEBUG_REFRESH_ALERTS_ONLY = DEBUG;

  public static final String[] DEBUG_TEST_ALERTS = DEBUG_INJECT_TEST_ALERTS ? new String[]{
    "1,123,KA,10,456,34.7,F,1",
    //"1,123,K,10,456,24.1,F,1",
    //"1,123,Laser,10,456,29.8,F,1",
    //"1,123,K,10,456,29.8,F,1",
    //"1,123,X,10,456,29.8,F,1"
  } : new String[]{};

  public static final boolean DEBUG_USE_ZERO_BEARING = DEBUG;
  public static final boolean DEBUG_ANNOUNCE_VEHICLE_BEARING = false;
  public static final boolean DEBUG_INJECT_TEST_REPORTS = DEBUG;
  public static final float REPORTS_MAX_DISTANCE = DEBUG ? 50.0f : 3.0f;
  public static final float REPORTS_REMINDER_DISTANCE = DEBUG ? 7.0f : 1.0f;
  public static final int REPORTS_MAX_ANNOUNCE_COUNT = 3;

  public static final String DEBUG_TEST_REPORTS = DEBUG_INJECT_TEST_REPORTS ? """
      {
        "alerts" : [
            {
              "additionalInfo" : "",
              "city" : "East Palo Alto, CA",
              "confidence" : 0,
              "country" : "US",
              "inscale" : true,
              "isJamUnifiedAlert" : false,
              "location" : {
                  "x" : -122.141221,
                  "y" : 37.458524
              },
              "magvar" : 25,
              "nImages" : 0,
              "reliability" : 5,
              "reportRating" : 4,
              "roadType" : 6,
              "street" : "University Ave",
              "subtype" : "HAZARD_ON_ROAD_CONSTRUCTION",
              "type" : "ACCIDENT"
            },
            {
              "additionalInfo" : "",
              "city" : "San Mateo, CA",
              "confidence" : 2,
              "country" : "US",
              "inscale" : true,
              "isJamUnifiedAlert" : false,
              "location" : {
                  "x" : -122.325196,
                  "y" : 37.581049
              },
              "magvar" : 139,
              "nThumbsUp" : 4,
              "reliability" : 9,
              "reportRating" : 5,
              "roadType" : 3,
              "street" : "US-101 S",
              "subtype" : "POLICE_VISIBLE",
              "type" : "POLICE"
            },
            {
              "additionalInfo" : "",
              "city" : "San Bruno, CA",
              "confidence" : 0,
              "country" : "US",
              "inscale" : false,
              "isJamUnifiedAlert" : false,
              "location" : {
                  "x" : -122.403133,
                  "y" : 37.631098
              },
              "magvar" : 172,
              "nThumbsUp" : 1,
              "reliability" : 6,
              "reportRating" : 2,
              "roadType" : 4,
              "street" : "Exit 6A: US-101 S » San Jose / SF Intl Airport",
              "subtype" : "POLICE_VISIBLE",
              "type" : "POLICE"
            },
            {
              "additionalInfo" : "",
              "city" : "San Bruno, CA",
              "confidence" : 0,
              "country" : "US",
              "inscale" : true,
              "isJamUnifiedAlert" : false,
              "location" : {
                  "x" : -122.402707,
                  "y" : 37.630026
              },
              "magvar" : 174,
              "nThumbsUp" : 1,
              "reliability" : 7,
              "reportRating" : 5,
              "roadType" : 3,
              "street" : "US-101 S",
              "subtype" : "POLICE_VISIBLE",
              "type" : "POLICE"
            }
        ]
      }
    """ : "";

  public static final boolean DEBUG_INJECT_TEST_AIRCRAFTS = DEBUG;
  public static final float AIRCRAFTS_MAX_DISTANCE = DEBUG ? 50.0f : 10.0f;
  public static final float AIRCRAFTS_REMINDER_DISTANCE = DEBUG ? 10.0f : 2.0f;
  public static final int AIRCRAFTS_MAX_ANNOUNCE_COUNT = 3;

  // The following test transponder icao24 addresses correspond to interesting
  // aircrafts in the aircrafts database
  // a3566a
  // a54f11
  // a6165b
  public static final String DEBUG_TEST_AIRCRAFTS = DEBUG_INJECT_TEST_AIRCRAFTS ? """
      {
        "states" : [
            [
              "ab5d36",
              "EJM831  ",
              "United States",
              0,
              0,
              -121.9169,
              37.7466,
              8008.62,
              false,
              209.61,
              351.53,
              13,
              null,
              8161.02,
              "1757",
              false,
              0
            ],
            [
              "a3566a",
              "N505FC  ",
              "United States",
              0,
              0,
              -121.9344,
              37.3616,
              null,
              true,
              0,
              230.62,
              null,
              null,
              null,
              "6042",
              false,
              0
            ],
            [
              "a2d3b3",
              "SWA458  ",
              "United States",
              0,
              0,
              -122.2155,
              37.7109,
              null,
              true,
              0,
              306.56,
              null,
              null,
              null,
              null,
              false,
              0
            ],
            [
              "a54f11",
              "N362Q   ",
              "United States",
              0,
              0,
              -122.0931,
              37.4636,
              259.08,
              false,
              43.66,
              145.56,
              0.33,
              null,
              205.74,
              "1200",
              false,
              0
            ],
            [
              "ad9e83",
              "N977JW  ",
              "United States",
              0,
              0,
              -122.114,
              37.4544,
              null,
              true,
              2.83,
              244.69,
              null,
              null,
              null,
              "1200",
              false,
              0
            ],
            [
              "a6165b",
              "",
              "United States",
              0,
              0,
              -121.9982,
              37.498,
              1676.4,
              false,
              123.89,
              274.76,
              -5.2,
              null,
              1653.54,
              "7253",
              false,
              0
            ],
            [
              "ad5460",
              "SWA1682 ",
              "United States",
              0,
              0,
              -122.2136,
              37.7023,
              null,
              true,
              0.51,
              210.94,
              null,
              null,
              null,
              null,
              false,
              0
            ]
        ]
      }
    """ : "";

}
