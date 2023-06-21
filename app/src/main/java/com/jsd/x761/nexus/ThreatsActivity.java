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

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.jsd.x761.nexus.Nexus.R;
import com.nolimits.ds1library.DS1Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An activity that collects and presents a view of the relevant threats.
 */
public class ThreatsActivity extends DS1ServiceActivity {
  private static final String TAG = "THREATS_ACTIVITY";
  public static final String MESSAGE_TOKEN = "THREATS_ACTIVITY_MESSAGES";

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private ThreatsAdapter mThreatsAdapter;
  private ServiceConnection mSpeechServiceConnection;
  private SpeechService mSpeechService;
  private Runnable mClearAlertsTask;
  private ConnectivityManager mConnectivityManager;
  private SharedPreferences mSharedPreferences;
  private FusedLocationProviderClient mLocationClient;
  private LocationListener mLocationListener;
  private Location mLastLocation;
  private Location mLocation;
  private boolean mHasLocation = true;
  private Executor mReportsFetchTaskExecutor;
  private Executor mAircraftsFetchTaskExecutor;
  private Runnable mCheckForReportsTask;
  private String mReportsSourceURL;
  private boolean mReportsAvailable = true;
  private Runnable mDebugBackgroundAlertsTask;
  private Runnable mDebugTestAlertsTask;
  private boolean mNetworkConnected = true;
  private float mBearing = 0.0f;
  private AircraftsDatabase mAircraftsDatabase;
  private Runnable mCheckForAircraftsTask;
  private String mAircraftsSourceURL;
  private boolean mAircraftsAvailable = true;
  private String mAircraftsUser;
  private String mAircraftsPassword;

  @Override
  protected void onCreate(Bundle b) {
    Log.i(TAG, "onCreate");
    super.onCreate(b);

    setTitle("Threats");

    setContentView(R.layout.threats_activity);
    mDS1ConnectedText = findViewById(R.id.ds1ConnectedText);
    RecyclerView threatsRecyclerView = findViewById(R.id.threatsRecyclerView);
    threatsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mThreatsAdapter = new ThreatsAdapter(ThreatsActivity.this);
    threatsRecyclerView.setAdapter(mThreatsAdapter);

    mSharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

    mLocationClient = LocationServices.getFusedLocationProviderClient(this);

    mConnectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

    mReportsFetchTaskExecutor = Executors.newSingleThreadExecutor();
    mAircraftsFetchTaskExecutor = Executors.newSingleThreadExecutor();

    // Bind to the speech service
    Log.i(TAG, "bindSpeechService()");
    bindSpeechService(new Runnable() {
      @Override
      public void run() {
        Log.i(TAG, "bindSpeechService.onDone");

        // Bind to the DS1 service
        Log.i(TAG, "bindDS1Service()");
        bindDS1Service(new Runnable() {
          @SuppressLint("MissingPermission")
          @Override
          public void run() {
            Log.i(TAG, "bindDS1Service.onDone");

            // Both services are now available, proceed with setup
            if(Configuration.ENABLE_ALERTS) {
              if(Configuration.DEBUG_INJECT_TEST_BACKGROUND_ALERTS) {
                // Inject test background DS1 alerts every few seconds to help
                // test the app without having to use an actual DS1 device
                // everytime
                Log.i(TAG, "using test background DS1 alerts");
                mDebugBackgroundAlertsTask = new Runnable() {
                  @Override
                  public void run() {
                    try {
                      onDS1DeviceData();
                    }
                    finally {
                      mHandler.postDelayed(this, MESSAGE_TOKEN, Configuration.DEBUG_TEST_BACKGROUND_ALERTS_TIMER);
                    }
                  }
                };
                mHandler.postDelayed(mDebugBackgroundAlertsTask, MESSAGE_TOKEN, 1);
              }
            }

            if(Configuration.ENABLE_REPORTS || Configuration.ENABLE_AIRCRAFTS) {
              // Regularly get the current location
              mLocationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                  ThreatsActivity.this.onLocationChanged(location);
                }
              };
              LocationRequest locationRequest = new LocationRequest.Builder(Configuration.CURRENT_LOCATION_TIMER).build();
              mLocationClient.requestLocationUpdates(locationRequest, mLocationListener, Looper.getMainLooper());

              // First time we're getting a location, proceed with
              // crowd-sourced reports and aircrafts
              if(Configuration.ENABLE_REPORTS) {
                // Regularly fetch crow-sourced reports
                mReportsSourceURL = mSharedPreferences.getString(getString(R.string.key_reports_url), getString(R.string.default_reports_url));
                if(Configuration.DEBUG_INJECT_TEST_REPORTS || !mReportsSourceURL.equals(getString(R.string.default_reports_url))) {

                  mCheckForReportsTask = new Runnable() {
                    @Override
                    public void run() {
                      try {
                        checkForReports();
                      }
                      finally {
                        mHandler.postDelayed(this, MESSAGE_TOKEN, Configuration.REPORTS_CHECK_TIMER);
                      }
                    }
                  };
                  mHandler.postDelayed(mCheckForReportsTask, MESSAGE_TOKEN, Configuration.REPORTS_INITIAL_CHECK_TIMER);
                }
              }

              if(Configuration.ENABLE_AIRCRAFTS) {
                // Load aircrafts database
                mAircraftsDatabase = new AircraftsDatabase(ThreatsActivity.this);
                // Regularly fetch aircraft state vectors
                mAircraftsSourceURL = mSharedPreferences.getString(getString(R.string.key_aircrafts_url), getString(R.string.default_aircrafts_url));
                if(Configuration.DEBUG_INJECT_TEST_AIRCRAFTS || (!mAircraftsSourceURL.equals(getString(R.string.default_aircrafts_url)) &&
                                                                   mAircraftsDatabase.getInterestingAircrafts().size() != 0)) {
                  mAircraftsUser = mSharedPreferences.getString(getString(R.string.key_aircrafts_user), getString(R.string.default_aircrafts_user));
                  if(mAircraftsUser.equals(getString(R.string.default_aircrafts_user))) {
                    mAircraftsUser = "";
                  }
                  mAircraftsPassword =
                    mSharedPreferences.getString(getString(R.string.key_aircrafts_password), getString(R.string.default_aircrafts_password));
                  if(mAircraftsPassword.equals(getString(R.string.default_aircrafts_password))) {
                    mAircraftsPassword = "";
                  }

                  mCheckForAircraftsTask = new Runnable() {
                    @Override
                    public void run() {
                      try {
                        checkForAircrafts();
                      }
                      finally {
                        if(Configuration.DEBUG_INJECT_TEST_AIRCRAFTS || (mAircraftsUser.length() != 0 && mAircraftsPassword.length() != 0)) {
                          mHandler.postDelayed(this, MESSAGE_TOKEN, Configuration.AIRCRAFTS_AUTHENTICATED_CHECK_TIMER);
                        }
                        else {
                          mHandler.postDelayed(this, MESSAGE_TOKEN, Configuration.AIRCRAFTS_ANONYMOUS_CHECK_TIMER);
                        }
                      }
                    }
                  };
                  mHandler.postDelayed(mCheckForAircraftsTask, MESSAGE_TOKEN, Configuration.AIRCRAFTS_INITIAL_CHECK_TIMER);
                }
              }
            }
          }
        });
      }
    });
  }

  private void bindSpeechService(Runnable onDone) {
    // Bind to the speech service
    mSpeechServiceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder s) {
        Log.i(TAG, "mSpeechServiceConnection.onServiceConnected");
        mSpeechService = ((SpeechService.ThisBinder)s).getService();
        mSpeechService.isReady(onDone);
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        Log.i(TAG, "mSpeechServiceConnection.onServiceDisconnected");
      }
    };

    Log.i(TAG, "bindService() mSpeechServiceConnection");
    Intent speechServiceIntent = new Intent(this, SpeechService.class);
    bindService(speechServiceIntent, mSpeechServiceConnection, BIND_AUTO_CREATE);
  }

  public SpeechService getSpeechService() {
    return mSpeechService;
  }

  @Override
  protected void onDS1DeviceConnected() {
    Log.i(TAG, "onDS1DeviceConnected");
    // Configure the DS1 device to notify of alerts in the background
    // and to report settings as well
    mHandler.postDelayed(() -> {
      mDS1Service.enableAlertNotifications();
      mDS1Service.setBackgroundAlert(true);
    }, MESSAGE_TOKEN, Configuration.DS1_SERVICE_SETUP_TIMER);

    // Announce that the DS1 device is connected
    if(mSpeechService != null) {
      mSpeechService.announceEvent("DS1 radar is online", () -> {
      });
    }
  }

  @Override
  protected void onDS1DeviceDisconnected() {
    Log.i(TAG, "onDS1DeviceDisconnected");
    // Announce that the DS1 device is disconnected
    if(mSpeechService != null) {
      mSpeechService.announceEvent("DS1 radar is offline", () -> {
      });
    }
  }

  @Override
  protected void onDestroy() {
    Log.i(TAG, "onDestroy");
    super.onDestroy();

    mHandler.removeCallbacksAndMessages(MESSAGE_TOKEN);

    if(mSpeechServiceConnection != null) {
      Log.i(TAG, "unbindService() mSpeechServiceConnection");
      unbindService(mSpeechServiceConnection);
    }
    if(mLocationClient != null && mLocationListener != null) {
      mLocationClient.removeLocationUpdates(mLocationListener);
    }
  }

  @Override
  protected void onDS1DeviceData() {
    Log.i(TAG, "onDS1DeviceData");
    List<Threat> alerts = new ArrayList<>();
    if(mDS1Service != null && mDS1Service.isConnected()) {
      // Collect alerts from the DS1 device
      List<DS1Service.RD_Alert> ds1Alerts = mDS1Service.getmAlerts();
      if(ds1Alerts != null) {
        for(DS1Service.RD_Alert ds1Alert : ds1Alerts) {
          if(ds1Alert.detected && !ds1Alert.muted) {
            alerts.add(Threat.fromDS1Alert(ds1Alert));
          }
        }
      }
    }
    if(Configuration.DEBUG_INJECT_TEST_ALERTS) {
      // Inject test alerts to help test the app without having to
      // use an actual DS1 device everytime
      Log.i(TAG, "injecting test DS1 alerts");
      DS1Service ds1Service = new DS1Service();
      for(String alert : Configuration.DEBUG_TEST_ALERTS) {
        alerts.add(Threat.fromDS1Alert(ds1Service.new RD_Alert(alert)));
      }
    }

    List<Threat> newAlerts = new ArrayList<>();
    List<Threat> mAlerts = mThreatsAdapter.getAlerts();
    for(Threat alert : alerts) {
      for(Threat mAlert : mAlerts) {
        // Detect repeating alerts, reuse the existing alert instead of
        // adding the repeated alert to avoid announcing the same alert
        // over and over
        if(alert.threatClass == mAlert.threatClass && alert.band == mAlert.band && alert.frequency == mAlert.frequency) {
          mAlert.direction = alert.direction;
          mAlert.intensity = alert.intensity;
          Log.i(TAG, "repeating alert");
          alert = mAlert;
          break;
        }
      }
      newAlerts.add(alert);
    }
    if(newAlerts.size() == 0) {
      return;
    }

    // Sort alerts by priority
    newAlerts.sort((o1, o2) -> o1.priority - o2.priority);

    // Clear alerts after a few seconds
    if(mClearAlertsTask != null) {
      Log.i(TAG, "removeCallbacks() mClearAlertsTask");
      mHandler.removeCallbacks(mClearAlertsTask);
      mClearAlertsTask = null;
    }
    mThreatsAdapter.setAlerts(newAlerts, () -> startClearAlertsTask());
  }

  private void startClearAlertsTask() {
    if(mClearAlertsTask != null) {
      Log.i(TAG, "removeCallbacks() mClearAlertsTask");
      mHandler.removeCallbacks(mClearAlertsTask);
      mClearAlertsTask = null;
    }
    mClearAlertsTask = () -> {
      Log.i(TAG, "setAlerts(())");
      mThreatsAdapter.setAlerts(new ArrayList<>(), () -> {
      });
    };

    // Clear alerts after a few seconds without receiving any new alerts
    Log.i(TAG, "postDelayed() mClearAlertsTask");
    mHandler.postDelayed(mClearAlertsTask, MESSAGE_TOKEN, Configuration.ALERTS_CLEAR_TIMER);
  }

  protected void onLocationChanged(Location location) {
    Log.i(TAG, "onLocationChanged");
    if(location != null) {
      mLastLocation = mLocation;
      mLocation = location;

      boolean hasBearing = false;
      if(Configuration.DEBUG_USE_ZERO_BEARING) {
        mBearing = 0.0f;
        mLocation.setBearing(mBearing);
        hasBearing = true;
      }
      else if(!Configuration.USE_COMPUTED_LOCATION_BEARING) {
        if(mLocation.hasBearing()) {
          mBearing = mLocation.getBearing();
          hasBearing = true;
        }
        else {
          // Use the last recorded bearing if no new current bearing
          Log.i(TAG, String.format("using last known bearing %f", mBearing));
          mLocation.setBearing(mBearing);
        }
      }
      else {
        if(mLastLocation != null) {
          if(Geospatial.getDistance(mLastLocation, mLocation) > Configuration.COMPUTED_BEARING_DISTANCE_THRESHOLD) {
            mBearing = Geospatial.getBearing(mLastLocation, mLocation);
            hasBearing = true;
          }
          else {
            Log.i(TAG, String.format("using last known bearing %f", mBearing));
          }
          mLocation.setBearing(mBearing);
        }
        else {
          Log.i(TAG, String.format("using last known bearing %f", mBearing));
          mLocation.setBearing(mBearing);
        }
      }
      Log.i(
        TAG,
        String.format("location lat %f lng %f bearing %f", (float)mLocation.getLatitude(), (float)mLocation.getLongitude(), mLocation.getBearing()));

      if(Configuration.DEBUG_ANNOUNCE_VEHICLE_BEARING) {
        if(hasBearing) {
          DecimalFormat df = new DecimalFormat("0.#");
          mSpeechService.announceEvent(String.format("Vehicle bearing is %s", df.format(mBearing)), () -> {
          });
        }
        else {
          mSpeechService.announceEvent("No vehicle bearing", () -> {
          });
        }
      }
    }
    else {
      Log.i(TAG, "location not available");
    }

    // Announce location availability changes
    if(location == null && mHasLocation) {
      mHasLocation = false;
      mSpeechService.announceEvent("Location is off", () -> {
      });
    }
    else if(location != null && !mHasLocation) {
      mHasLocation = true;
      mSpeechService.announceEvent("Location is back on", () -> {
      });
    }
  }

  private void isNetworkConnected(Runnable onDone) {
    Log.i(TAG, "isNetworkConnected");
    Network network = mConnectivityManager.getActiveNetwork();
    boolean networkConnected;
    if(network != null) {
      Log.i(TAG, "isNetworkConnected true");
      networkConnected = true;
    }
    else {
      Log.i(TAG, "isNetworkConnected false");
      networkConnected = false;
    }

    // Announce network connectivity changes
    if(!networkConnected && mNetworkConnected) {
      mNetworkConnected = networkConnected;
      mSpeechService.announceEvent("Network is offline", () -> mHandler.postDelayed(onDone, MESSAGE_TOKEN, 1));
    }
    else if(networkConnected && !mNetworkConnected) {
      mNetworkConnected = networkConnected;
      mSpeechService.announceEvent("Network is back online", () -> mHandler.postDelayed(onDone, MESSAGE_TOKEN, 1));
    }
    else {
      mHandler.postDelayed(onDone, MESSAGE_TOKEN, 1);
    }
  }

  private void checkForReports() {
    Log.i(TAG, "checkForReports");

    // Check network connectivity then fetch crowd-sourced reports in a
    // radius around the current location
    isNetworkConnected(() -> {
      if(mNetworkConnected) {
        if(mLocation != null) {
          ReportsFetchTask reportsFetchTask = new ReportsFetchTask(mReportsSourceURL, mLocation) {
            @Override
            protected void onDone(List<Threat> reports) {
              if(reports == null) {
                Log.i(TAG, "reportsFetchTask.onDone null reports");
              }
              else {
                Log.i(TAG, String.format("reportsFetchTask.onDone %d reports", reports.size()));
              }
              onReportsData(reports);
            }
          };

          Log.i(TAG, "reportsFetchTask.execute()");
          mReportsFetchTaskExecutor.execute(reportsFetchTask);
        }
      }
    });
  }

  protected void onReportsData(List<Threat> reports) {
    if(reports == null) {
      if(mReportsAvailable) {
        mReportsAvailable = false;
        mSpeechService.announceEvent("Reports are off", () -> {
        });
      }
      return;
    }
    else {
      if(!mReportsAvailable) {
        mReportsAvailable = true;
        mSpeechService.announceEvent("Reports are back on", () -> {
        });
      }
    }
    if(reports.size() == 0) {
      return;
    }
    mHandler.postDelayed(() -> {
      // Sort collected reports by priority
      reports.sort(Comparator.comparingInt(o -> o.priority));

      // Filter out duplicate reports
      List<Threat> uniqueReports = new ArrayList<>();
      for(Threat report : reports) {
        boolean duplicate = false;
        for(Threat uniqueReport : uniqueReports) {
          if(report.isDuplicateReport(uniqueReport)) {
            duplicate = true;
            break;
          }
        }
        if(!duplicate) {
          uniqueReports.add(report);
        }
      }

      // Announce each report once initially and then announce a reminder
      // once it gets closer
      List<Threat> newReports = new ArrayList<>();
      List<Threat> mReports = mThreatsAdapter.getReports();
      for(Threat report : uniqueReports) {
        for(Threat mReport : mReports) {
          if(report.isSameReport(mReport)) {
            Log.i(TAG, String.format("existing report with new distance %f", report.distance));
            if(report.distance <= Configuration.REPORTS_REMINDER_DISTANCE && mReport.distance > Configuration.REPORTS_REMINDER_DISTANCE) {
              mReport.announced = 0;
            }
            mReport.distance = report.distance;
            mReport.hour = report.hour;
            report = mReport;
            break;
          }
        }
        newReports.add(report);
      }

      // Sort final list of reports by priority
      newReports.sort(Comparator.comparingInt(o -> o.priority));

      mThreatsAdapter.setReports(newReports, () -> {
      });
    }, MESSAGE_TOKEN, 1);
  }

  private void checkForAircrafts() {
    Log.i(TAG, "checkForAircrafts");

    // Check network connectivity then fetch aircraft state vectors in a
    // radius around the current location
    isNetworkConnected(() -> {
      if(mNetworkConnected) {
        if(mLocation != null) {
          AircraftsFetchTask aircraftsFetchTask = new AircraftsFetchTask(mAircraftsSourceURL, mAircraftsUser, mAircraftsPassword, mAircraftsDatabase,
            mLocation) {
            @Override
            protected void onDone(List<Threat> aircrafts) {
              if(aircrafts == null) {
                Log.i(TAG, "aircraftsFetchTask.onDone null aircraft state vectors");
              }
              else {
                Log.i(TAG, String.format("aircraftsFetchTask.onDone %d aircraft state vectors", aircrafts.size()));
              }
              onAircraftsData(aircrafts);
            }
          };

          Log.i(TAG, "aircraftsFetchTask.execute()");
          mAircraftsFetchTaskExecutor.execute(aircraftsFetchTask);
        }
      }
    });
  }

  protected void onAircraftsData(List<Threat> aircrafts) {
    if(aircrafts == null) {
      if(mAircraftsAvailable) {
        mAircraftsAvailable = false;
        mSpeechService.announceEvent("Aircraft recognition is off", () -> {
        });
      }
      return;
    }
    else {
      if(!mAircraftsAvailable) {
        mAircraftsAvailable = true;
        mSpeechService.announceEvent("Aircraft recognition is back on", () -> {
        });
      }
    }
    if(aircrafts.size() == 0) {
      return;
    }
    mHandler.postDelayed(() -> {
      List<Threat> newAircrafts = new ArrayList<>();
      List<Threat> mAircrafts = mThreatsAdapter.getAircrafts();
      // Announce each aircraft state vector once initially and then
      // announce a reminder once it gets closer
      for(Threat aircraft : aircrafts) {
        for(Threat mAircraft : mAircrafts) {
          if(aircraft.isSameAircraft(mAircraft)) {
            Log.i(TAG, String.format("existing aircraft state vector with new distance %f", aircraft.distance));
            if(aircraft.distance <= Configuration.AIRCRAFTS_REMINDER_DISTANCE && mAircraft.distance > Configuration.AIRCRAFTS_REMINDER_DISTANCE) {
              mAircraft.announced = 0;
            }
            mAircraft.distance = aircraft.distance;
            mAircraft.latitude = aircraft.latitude;
            mAircraft.longitude = aircraft.longitude;
            mAircraft.hour = aircraft.hour;
            aircraft = mAircraft;
            break;
          }
        }
        newAircrafts.add(aircraft);
      }

      // Sort aircraft state vectors by priority
      newAircrafts.sort(Comparator.comparingInt(o -> o.priority));

      mThreatsAdapter.setAircrafts(newAircrafts, () -> {
      });
    }, MESSAGE_TOKEN, 1);
  }

  public void onRefreshClick(View v) {
    Log.i(TAG, "onRefreshClick");

    if(Configuration.DEBUG_REFRESH_ALERTS_ONLY) {
      mThreatsAdapter.setAlerts(new ArrayList<>(), () -> {
        // mHandler.post(() -> {
        // Check for new DS1 alerts, crowd-sourced reports and aircraft state
        // vectors
        if(Configuration.ENABLE_ALERTS) {
          Runnable onDS1DeviceDataTask = () -> onDS1DeviceData();
          mHandler.postDelayed(onDS1DeviceDataTask, MESSAGE_TOKEN, 1);

          if(Configuration.DEBUG_INJECT_TEST_ALERTS && Configuration.DEBUG_TEST_ALERTS_REPEAT_COUNT > 0) {
            // Inject test alerts to help test the app without having to use
            // an actual DS1 device everytime
            mDebugTestAlertsTask = new Runnable() {
              int i = 0;

              @Override
              public void run() {
                try {
                  onDS1DeviceData();
                }
                finally {
                  i++;
                  if(i < Configuration.DEBUG_TEST_ALERTS_REPEAT_COUNT) {
                    mHandler.postDelayed(this, MESSAGE_TOKEN, Configuration.DEBUG_TEST_ALERTS_REPEAT_TIMER);
                  }
                  else {
                    mDebugTestAlertsTask = null;
                  }
                }
              }
            };
            mHandler.postDelayed(mDebugTestAlertsTask, MESSAGE_TOKEN, 1);
          }
        }
      });
      return;
    }

    mThreatsAdapter.setAlerts(new ArrayList<>(), () -> {
      mThreatsAdapter.setReports(new ArrayList<>(), () -> {
        mThreatsAdapter.setAircrafts(new ArrayList<>(), () -> {
          // Check for new DS1 alerts, crowd-sourced reports and aircraft state
          // vectors
          if(Configuration.ENABLE_ALERTS) {
            Runnable onDS1DeviceDataTask = () -> onDS1DeviceData();
            mHandler.postDelayed(onDS1DeviceDataTask, MESSAGE_TOKEN, 1);

            if(Configuration.DEBUG_INJECT_TEST_ALERTS) {
              // Inject test alerts to help test the app without having to use
              // an actual DS1 device everytime
              mDebugTestAlertsTask = new Runnable() {
                int i = 0;

                @Override
                public void run() {
                  try {
                    onDS1DeviceData();
                  }
                  finally {
                    i++;
                    if(i < Configuration.DEBUG_TEST_ALERTS_REPEAT_COUNT) {
                      mHandler.postDelayed(this, MESSAGE_TOKEN, Configuration.DEBUG_TEST_ALERTS_REPEAT_TIMER);
                    }
                    else {
                      mDebugTestAlertsTask = null;
                    }
                  }
                }
              };
              mHandler.postDelayed(mDebugTestAlertsTask, MESSAGE_TOKEN, 1);
            }
          }
          if(Configuration.ENABLE_REPORTS) {
            if(Configuration.DEBUG_INJECT_TEST_REPORTS || !mReportsSourceURL.equals(getString(R.string.default_reports_url))) {
              mHandler.removeCallbacks(mCheckForReportsTask);
              mHandler.postDelayed(mCheckForReportsTask, MESSAGE_TOKEN, 1);
            }
          }
          if(Configuration.ENABLE_AIRCRAFTS) {
            if(Configuration.DEBUG_INJECT_TEST_AIRCRAFTS || !mAircraftsSourceURL.equals(getString(R.string.default_aircrafts_url))) {
              mHandler.removeCallbacks(mCheckForAircraftsTask);
              mHandler.postDelayed(mCheckForAircraftsTask, MESSAGE_TOKEN, 1);
            }
          }
        });
      });
    });
  }
}
