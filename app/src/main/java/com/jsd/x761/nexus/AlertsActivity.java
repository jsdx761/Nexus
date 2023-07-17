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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.jsd.x761.nexus.Nexus.R;
import com.nolimits.ds1library.DS1Service;

import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An activity that collects and presents a view of the relevant alerts.
 */
public class AlertsActivity extends DS1ServiceActivity {
  private static final String TAG = "ALERTS_ACTIVITY";
  public static final String MESSAGE_TOKEN = "ALERTS_ACTIVITY_MESSAGES";

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private AlertsAdapter mAlertsAdapter;
  private ServiceConnection mSpeechServiceConnection;
  private SpeechService mSpeechService;
  private boolean mBoundSpeechService;
  private Runnable mClearDS1AlertsTask;
  private SharedPreferences mSharedPreferences;
  private Runnable mDebugBackgroundAlertsTask;
  private ImageView mLocationActiveImage;
  RecyclerView mAlertsRecyclerView;
  private FusedLocationProviderClient mLocationClient;
  private LocationRequest mLocationRequest;
  private LocationCallback mLocationCallback;
  private Runnable mOnGetInitialLocationTask;
  private Location mLastLocation;
  private Location mLocation;
  private float mBearing = 0.0f;
  private boolean mLocationActive;
  private Runnable mLocationNotAvailableTask;
  private ImageView mNetworkConnectedImage;
  private boolean mNetworkConnected = true;
  private Runnable mNetworkCheckTask;
  private Executor mNetworkCheckTaskExecutor;
  private boolean mDS1AlertsActive;
  private boolean mReportsEnabled;
  private ImageView mReportsActiveImage;
  private String mReportsSourceURL;
  private String mReportsSourceName;
  private int mReportsActive;
  private Executor mReportsFetchTaskExecutor;
  private Runnable mCheckForReportsTask;
  private boolean mAircraftsEnabled;
  private ImageView mAircraftsActiveImage;
  private AircraftsDatabase mAircraftsDatabase;
  private Runnable mCheckForAircraftsTask;
  private String mAircraftsSourceURL;
  private int mAircraftsActive;
  private Executor mAircraftsFetchTaskExecutor;
  private String mAircraftsUser;
  private String mAircraftsPassword;

  @SuppressLint("MissingPermission")
  @Override
  protected void onCreate(Bundle b) {
    Log.i(TAG, "onCreate");
    super.onCreate(b);

    setTitle("Alerts");

    setContentView(R.layout.alerts_activity);
    mDS1ConnectedImage = findViewById(R.id.ds1ConnectedImage);
    mReportsActiveImage = findViewById(R.id.reportsActiveImage);
    mAircraftsActiveImage = findViewById(R.id.aircraftsActiveImage);
    mNetworkConnectedImage = findViewById(R.id.networkConnectedImage);
    mLocationActiveImage = findViewById(R.id.locationActiveImage);
    mAlertsRecyclerView = findViewById(R.id.alertsRecyclerView);
    mAlertsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    mSharedPreferences = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    mReportsEnabled = mSharedPreferences.getBoolean(getString(R.string.key_reports_enabled), false);
    mReportsSourceURL = mSharedPreferences.getString(getString(R.string.key_reports_url), getString(R.string.default_reports_url));
    try {
      String[] parts = new URL(mReportsSourceURL).getHost().split("\\.");
      mReportsSourceName = parts[parts.length - 2];
      mReportsSourceName = mReportsSourceName.substring(0, 1).toUpperCase() + mReportsSourceName.substring(1).toLowerCase();
    }
    catch(Exception e) {
      mReportsSourceName = "Crowd-sourced";
    }
    mAircraftsEnabled = mSharedPreferences.getBoolean(getString(R.string.key_aircrafts_enabled), false);
    mAircraftsSourceURL = mSharedPreferences.getString(getString(R.string.key_aircrafts_url), getString(R.string.default_aircrafts_url));
    mAircraftsUser = mSharedPreferences.getString(getString(R.string.key_aircrafts_user), getString(R.string.default_aircrafts_user));
    if(mAircraftsUser.equals(getString(R.string.default_aircrafts_user))) {
      mAircraftsUser = "";
    }
    mAircraftsPassword =
      mSharedPreferences.getString(getString(R.string.key_aircrafts_password), getString(R.string.default_aircrafts_password));
    if(mAircraftsPassword.equals(getString(R.string.default_aircrafts_password))) {
      mAircraftsPassword = "";
    }

    mLocationClient = LocationServices.getFusedLocationProviderClient(this);

    mNetworkCheckTaskExecutor = Executors.newSingleThreadExecutor();
    mReportsFetchTaskExecutor = Executors.newSingleThreadExecutor();
    mAircraftsFetchTaskExecutor = Executors.newSingleThreadExecutor();

    // Bind to the speech service
    Log.i(TAG, "bindSpeechService()");
    bindSpeechService(() -> {
      Log.i(TAG, "bindSpeechService.onDone");

      // Check if DS1 alerts are enabled
      if(Configuration.ENABLE_RADAR_ALERTS) {
        if(mDS1ServiceEnabled) {
          mDS1AlertsActive = true;
        }
      }

      // Check if crowd-sourced reports are enabled
      if(Configuration.ENABLE_REPORTS) {
        if(Configuration.DEBUG_INJECT_TEST_REPORTS != 0 || (mReportsEnabled && !mReportsSourceURL.equals(getString(R.string.default_reports_url)))) {
          mReportsActive = 1;
        }
      }

      // Check if aircraft recognition is enabled
      if(Configuration.ENABLE_AIRCRAFTS) {
        if(Configuration.DEBUG_INJECT_TEST_AIRCRAFTS != 0 || mAircraftsEnabled) {
          // Load aircrafts database
          mAircraftsDatabase = new AircraftsDatabase(AlertsActivity.this);
          if(mAircraftsDatabase.getInterestingAircrafts().size() != 0) {
            mAircraftsActive = 1;
          }
        }
      }

      if(!mDS1AlertsActive && mReportsActive == 0 && mAircraftsActive == 0) {
        Log.i(TAG, "no alert sources enabled");
        runOnUiThread(() -> {
          AlertDialog.Builder d = new AlertDialog.Builder(this);
          d.setMessage("No alert sources are enabled.\n\nPlease enable and configure some alert sources in Settings.");
          d.setTitle("Alerts");
          d.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              mHandler.postDelayed(() -> {
                Intent intent = new Intent(AlertsActivity.this, SettingsMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
              }, MESSAGE_TOKEN, 1);
            }
          });
          d.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
              finish();
            }
          });
          d.show();
        });
      }

      if(mDS1AlertsActive) {
        // Bind to the DS1 service
        Log.i(TAG, "bindDS1Service()");
        bindDS1Service(() -> {
          Log.i(TAG, "bindDS1Service.onDone");
          if(Configuration.DEBUG_TEST_ALERTS_TIMER != 0) {
            // Inject test background DS1 alerts every few seconds to help
            // test the app without having to use an actual DS1 device
            // everytime
            Log.i(TAG, "using test background DS1 alerts");
            mDebugBackgroundAlertsTask = () -> {
              try {
                onDS1DeviceData();
              }
              finally {
                mHandler.postDelayed(mDebugBackgroundAlertsTask, MESSAGE_TOKEN, Configuration.DEBUG_TEST_ALERTS_TIMER);
              }
            };
            mHandler.postDelayed(mDebugBackgroundAlertsTask, MESSAGE_TOKEN, 1);
          }
        });
      }

      if(mReportsActive != 0 || mAircraftsActive != 0) {
        mLocationActive = true;

        // Regularly check network connectivity
        mNetworkCheckTask = () -> {
          isNetworkConnected(() -> {
            mHandler.postDelayed(mNetworkCheckTask, MESSAGE_TOKEN, Configuration.NETWORK_CONNECT_CHECK_TIMER);
          }, 0);
        };
        mHandler.postDelayed(mNetworkCheckTask, MESSAGE_TOKEN, 1);

        mOnGetInitialLocationTask = () -> {
          if(mReportsActive != 0) {
            // Regularly fetch crow-sourced reports
            mCheckForReportsTask = () -> {
              try {
                checkForReports(0);
              }
              finally {
                mHandler.postDelayed(mCheckForReportsTask, MESSAGE_TOKEN, Configuration.REPORTS_CHECK_TIMER);
              }
            };
            mHandler.postDelayed(mCheckForReportsTask, MESSAGE_TOKEN, 1);
          }

          if(mAircraftsActive != 0) {
            // Regularly fetch aircrafts
            mCheckForAircraftsTask = () -> {
              try {
                checkForAircrafts(0);
              }
              finally {
                if(Configuration.DEBUG_INJECT_TEST_AIRCRAFTS != 0 || (mAircraftsUser.length() != 0 && mAircraftsPassword.length() != 0)) {
                  mHandler.postDelayed(mCheckForAircraftsTask, MESSAGE_TOKEN, Configuration.AIRCRAFTS_AUTHENTICATED_CHECK_TIMER);
                }
                else {
                  mHandler.postDelayed(mCheckForAircraftsTask, MESSAGE_TOKEN, Configuration.AIRCRAFTS_ANONYMOUS_CHECK_TIMER);
                }
              }
            };
            mHandler.postDelayed(mCheckForAircraftsTask, MESSAGE_TOKEN, 1);
          }
        };

        // Regularly get the current location
        mLocationCallback = new LocationCallback() {
          @Override
          public void onLocationResult(@NonNull LocationResult locationResult) {
            Log.i(TAG, String.format("locationCallback.onLocationResult %s", locationResult));
            if(locationResult == null) {
              AlertsActivity.this.onLocationChanged(null);
            }
            else {
              AlertsActivity.this.onLocationChanged(locationResult.getLastLocation());
              if(mOnGetInitialLocationTask != null) {
                mHandler.postDelayed(mOnGetInitialLocationTask, MESSAGE_TOKEN, 1);
                mOnGetInitialLocationTask = null;
              }
            }
          }

          @Override
          public void onLocationAvailability(@NonNull LocationAvailability locationAvailability) {
            Log.i(TAG, String.format("locationCallback.onLocationAvailability %b", locationAvailability.isLocationAvailable()));
            if(!locationAvailability.isLocationAvailable()) {
              // Delay the location unavailable announcement a bit as it may
              // become available again soon
              if(mLocationNotAvailableTask != null) {
                mHandler.removeCallbacks(mLocationNotAvailableTask);
              }
              mLocationNotAvailableTask = () -> {
                AlertsActivity.this.onLocationChanged(null);
              };
              mHandler.postDelayed(mLocationNotAvailableTask, MESSAGE_TOKEN, Configuration.LOCATION_AVAILABILITY_CHECK_TIMER);
            }
            else {
              // Cancel any pending location unavailable announcement
              if(mLocationNotAvailableTask != null) {
                mHandler.removeCallbacks(mLocationNotAvailableTask);
                mLocationNotAvailableTask = null;
              }
            }
          }
        };

        Log.i(TAG, "locationclient.requestLocationUpdates()");
        mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, Configuration.CURRENT_LOCATION_TIMER).build();
        mLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
      }
    });
  }

  private void bindSpeechService(Runnable onDone) {
    Log.i(TAG, "bindSpeechService");
    // Bind to the speech service
    mSpeechServiceConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder s) {
        Log.i(TAG, "mSpeechServiceConnection.onServiceConnected");
        mSpeechService = ((SpeechService.ThisBinder)s).getService();
        if(!mBoundSpeechService) {
          mBoundSpeechService = true;
          mAlertsAdapter = new AlertsAdapter(AlertsActivity.this, mSpeechService, mReportsSourceName);
          mAlertsRecyclerView.setAdapter(mAlertsAdapter);
          mHandler.postDelayed(onDone, MESSAGE_TOKEN, 1);
        }
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
      if(mDS1ServiceActive == 0) {
        mSpeechService.announceEvent("Radar detector is back on", () -> {
        });
      }
      else if(mDS1ServiceActive == 1) {
        mSpeechService.announceEvent("Radar detector is on", () -> {
        });
      }
    }
    super.onDS1DeviceConnected();
  }

  @Override
  protected void onDS1DeviceDisconnected() {
    Log.i(TAG, "onDS1DeviceDisconnected");
    // Announce that the DS1 device is disconnected
    if(mSpeechService != null && mDS1ServiceActive != 0) {
      mSpeechService.announceEvent("Radar detector is off", () -> {
      });
    }
    super.onDS1DeviceDisconnected();

    // Try to reconnect to the DS1 device
    scheduleRefreshDS1Service();
  }

  @Override
  protected void onDestroy() {
    Log.i(TAG, "onDestroy");
    super.onDestroy();

    mAlertsAdapter.onDestroy();

    mHandler.removeCallbacksAndMessages(MESSAGE_TOKEN);

    if(mSpeechServiceConnection != null) {
      Log.i(TAG, "unbindService() mSpeechServiceConnection");
      unbindService(mSpeechServiceConnection);
    }
    if(mLocationClient != null && mLocationCallback != null) {
      Log.i(TAG, "locationclient.removeLocationUpdates()");
      mLocationClient.removeLocationUpdates(mLocationCallback);
    }
  }

  @Override
  protected void onDS1DeviceData() {
    Log.i(TAG, "onDS1DeviceData");
    List<Alert> alerts = new ArrayList<>();
    if(mDS1Service != null && mDS1Service.isConnected()) {
      // Collect alerts from the DS1 device
      List<DS1Service.RD_Alert> ds1Alerts = mDS1Service.getmAlerts();
      if(ds1Alerts != null) {
        for(DS1Service.RD_Alert ds1Alert : ds1Alerts) {
          if(ds1Alert.detected && !ds1Alert.muted) {
            alerts.add(Alert.fromDS1Alert(ds1Alert));
          }
        }
      }
    }
    if(Configuration.DEBUG_INJECT_TEST_ALERTS != 0) {
      // Inject test alerts to help test the app without having to
      // use an actual DS1 device everytime
      Log.i(TAG, "injecting test DS1 alerts");
      DS1Service ds1Service = new DS1Service();
      int n = 0;
      for(String alert : Configuration.DEBUG_TEST_ALERTS) {
        if(n < Configuration.DEBUG_INJECT_TEST_ALERTS) {
          alerts.add(Alert.fromDS1Alert(ds1Service.new RD_Alert(alert)));
          n++;
        }
      }
    }

    List<Alert> newAlerts = new ArrayList<>();
    List<Alert> mAlerts = mAlertsAdapter.getRadarAlerts();
    for(Alert alert : alerts) {
      for(Alert mAlert : mAlerts) {
        // Detect repeating alerts, reuse the existing alert instead of
        // adding the repeated alert to avoid announcing the same alert
        // over and over
        if(alert.alertClass == mAlert.alertClass && alert.band == mAlert.band && alert.frequency == mAlert.frequency) {
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
    if(mClearDS1AlertsTask != null) {
      Log.i(TAG, "removeCallbacks() mClearDS1AlertsTask");
      mHandler.removeCallbacks(mClearDS1AlertsTask);
      mClearDS1AlertsTask = null;
    }
    mAlertsAdapter.setRadarAlerts(newAlerts, () -> startClearAlertsTask());
  }

  private void startClearAlertsTask() {
    if(mClearDS1AlertsTask != null) {
      Log.i(TAG, "removeCallbacks() mClearDS1AlertsTask");
      mHandler.removeCallbacks(mClearDS1AlertsTask);
      mClearDS1AlertsTask = null;
    }
    mClearDS1AlertsTask = () -> {
      Log.i(TAG, "setRadarAlerts(())");
      mAlertsAdapter.setRadarAlerts(new ArrayList<>(), () -> {
      });
    };

    // Clear alerts after a few seconds without receiving any new alerts
    Log.i(TAG, "postDelayed() mClearDS1AlertsTask");
    mHandler.postDelayed(mClearDS1AlertsTask, MESSAGE_TOKEN, Configuration.ALERTS_CLEAR_TIMER);
  }

  protected void onLocationChanged(Location location) {
    Log.i(TAG, "onLocationChanged");
    mHandler.postDelayed(() -> {
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

        // Refresh reports and aircrafts with the new location
        if(mReportsActive != 0) {
          List<Alert> updatedReports = new ArrayList<>();
          for(Alert alert : mAlertsAdapter.getReportAlerts()) {
            updatedReports.add(Alert.fromReport(mLocation, alert));
          }
          onReportsData(updatedReports);

          List<Alert> updatedAircrafts = new ArrayList<>();
          for(Alert alert : mAlertsAdapter.getAircraftAlerts()) {
            updatedAircrafts.add(Alert.fromAircraft(mLocation, alert));
          }
          onAircraftsData(updatedAircrafts);
        }
      }
      else {
        Log.i(TAG, "location not available");
        mLocation = null;
        mLastLocation = null;
      }

      // Announce location availability changes
      if(location == null) {
        mLocationActiveImage.setColorFilter(Color.DKGRAY);
        if(mLocationActive) {
          mLocationActive = false;
          mSpeechService.announceEvent("Location is off", () -> {
          });
        }
      }
      else {
        mLocationActiveImage.setColorFilter(Color.LTGRAY);
        if(!mLocationActive) {
          mLocationActive = true;
          mSpeechService.announceEvent("Location is back on", () -> {
          });
        }
      }
    }, MESSAGE_TOKEN, 1);
  }

  private void isNetworkConnected(Runnable onDone, int retryCount) {
    Log.i(TAG, "isNetworkConnected");
    Runnable networkCheckTask = () -> {
      boolean connected;
      try {
        // Doing this is actually more reliable than checking for an active
        // network on the Android connectivity manager
        URL url = new URL("https://www.google.com");
        Log.i(TAG, String.format("openConnection() %s", url));
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.setRequestProperty("Connection", "close");
        connection.setConnectTimeout(Configuration.NETWORK_CONNECT_TIMEOUT);
        connection.connect();
        Log.i(TAG, String.format("connection responseCode %d", connection.getResponseCode()));
        connected = connection.getResponseCode() == 200;
      }
      catch(Exception e) {
        Log.e(TAG, String.format("Exception %s", e));
        connected = false;
      }
      final boolean networkConnected = connected;
      Log.i(TAG, String.format("isNetworkConnected %b", networkConnected));

      if(!networkConnected && retryCount < Configuration.NETWORK_CONNECT_RETRY_COUNT) {
        // Retry a few times to work around short lived connectivity issues
        Log.i(TAG, "post retry isNetworkConnected()");
        mHandler.postDelayed(() -> {
          isNetworkConnected(onDone, retryCount + 1);
        }, MESSAGE_TOKEN, Configuration.NETWORK_CONNECT_RETRY_TIMER);
        return;
      }

      mHandler.postDelayed(() -> {
        if(networkConnected) {
          mNetworkConnectedImage.setColorFilter(Color.LTGRAY);
        }
        else {
          mNetworkConnectedImage.setColorFilter(Color.DKGRAY);
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
          onDone.run();
        }
      }, MESSAGE_TOKEN, 1);
    };
    mNetworkCheckTaskExecutor.execute(networkCheckTask);
  }

  private void checkForReports(int retryCount) {
    Log.i(TAG, String.format("checkForReports retryCount %d", retryCount));

    // Fetch crowd-sourced reports in a radius around the current location
    if(mNetworkConnected) {
      if(mLocation != null) {
        ReportsFetchTask reportsFetchTask = new ReportsFetchTask(mReportsSourceURL, mLocation) {
          @Override
          protected void onDone(List<Alert> reports) {
            if(reports == null) {
              Log.i(TAG, "reportsFetchTask.onDone null reports");
              if(retryCount < Configuration.REPORTS_CHECK_RETRY_COUNT) {
                // Retry a few times before giving up, to work around short
                // lived connectivity issues
                Log.i(TAG, "post retry checkForReports()");
                mHandler.postDelayed(() -> {
                  checkForReports(retryCount + 1);
                }, MESSAGE_TOKEN, Configuration.REPORTS_CHECK_RETRY_TIMER);
              }
              else {
                onReportsData(null);
              }
            }
            else {
              Log.i(TAG, String.format("reportsFetchTask.onDone %d reports", reports.size()));
              onReportsData(reports);
            }
          }
        };

        Log.i(TAG, "reportsFetchTask.execute()");
        mReportsFetchTaskExecutor.execute(reportsFetchTask);
      }
      else {
        onReportsData(null);
      }
    }
    else {
      onReportsData(null);
    }
  }

  protected void onReportsData(List<Alert> reports) {
    if(reports == null) {
      mReportsActiveImage.setColorFilter(Color.DKGRAY);
      if(mReportsActive != 0) {
        mReportsActive = 0;
        mSpeechService.announceEvent(String.format("%s alerts are off", mReportsSourceName), () -> {
        });
      }
      return;
    }
    else {
      mReportsActiveImage.setColorFilter(Color.LTGRAY);
      if(mReportsActive == 0) {
        mReportsActive = 2;
        mSpeechService.announceEvent(String.format("%s alerts are back on", mReportsSourceName), () -> {
        });
      }
      else if(mReportsActive == 1) {
        mReportsActive = 2;
        mSpeechService.announceEvent(String.format("%s alerts are on", mReportsSourceName), () -> {
        });
      }
    }

    mHandler.postDelayed(() -> {
      // Filter out reports beyond configured distance
      List<Alert> inRangeReports = new ArrayList<>();
      for(Alert report : reports) {
        if(report.distance <= Configuration.REPORTS_MAX_DISTANCE) {
          inRangeReports.add(report);
        }
      }

      // Filter out duplicate reports
      List<Alert> uniqueReports = new ArrayList<>();
      for(Alert report : inRangeReports) {
        boolean duplicate = false;
        for(Alert uniqueReport : uniqueReports) {
          if(report.isDuplicateReport(uniqueReport)) {
            duplicate = true;
            break;
          }
        }
        if(!duplicate) {
          uniqueReports.add(report);
        }
      }

      // Update existing reports with their new position
      List<Alert> newReports = new ArrayList<>();
      List<Alert> mReports = mAlertsAdapter.getReportAlerts();
      for(Alert report : uniqueReports) {
        for(Alert mReport : mReports) {
          if(report.isSameReport(mReport)) {
            Log.i(TAG, String.format("existing report with new distance %f", report.distance));
            mReport.distance = report.distance;
            mReport.bearing = report.bearing;
            report = mReport;
            break;
          }
        }
        newReports.add(report);
      }

      // Sort final list of reports by priority
      newReports.sort(Comparator.comparingInt(o -> o.priority));

      mAlertsAdapter.setReportAlerts(newReports, () -> {
      });
    }, MESSAGE_TOKEN, 1);
  }

  private void checkForAircrafts(int retryCount) {
    Log.i(TAG, "checkForAircrafts");

    // Fetch aircraft state vectors in a radius around the current location
    if(mNetworkConnected) {
      if(mLocation != null) {
        AircraftsFetchTask aircraftsFetchTask = new AircraftsFetchTask(mAircraftsSourceURL, mAircraftsUser, mAircraftsPassword, mAircraftsDatabase,
          mLocation) {
          @Override
          protected void onDone(List<Alert> aircrafts) {
            if(aircrafts == null) {
              Log.i(TAG, "aircraftsFetchTask.onDone null aircraft state vectors");
              if(retryCount < Configuration.AIRCRAFTS_CHECK_RETRY_COUNT) {
                // Retry a few times before giving up, to work around short
                // lived connectivity issues
                Log.i(TAG, "post retry checkForAircrafts()");
                mHandler.postDelayed(() -> {
                  checkForReports(retryCount + 1);
                }, MESSAGE_TOKEN, Configuration.AIRCRAFTS_CHECK_RETRY_TIMER);
              }
              else {
                onAircraftsData(null);
              }
            }
            else {
              Log.i(TAG, String.format("aircraftsFetchTask.onDone %d aircraft state vectors", aircrafts.size()));
              onAircraftsData(aircrafts);
            }
          }
        };

        Log.i(TAG, "aircraftsFetchTask.execute()");
        mAircraftsFetchTaskExecutor.execute(aircraftsFetchTask);
      }
      else {
        onAircraftsData(null);
      }
    }
    else {
      onAircraftsData(null);
    }
  }

  protected void onAircraftsData(List<Alert> aircrafts) {
    if(aircrafts == null) {
      mAircraftsActiveImage.setColorFilter(Color.DKGRAY);
      if(mAircraftsActive != 0) {
        mAircraftsActive = 0;
        mSpeechService.announceEvent("Aircraft alerts are off", () -> {
        });
      }
      return;
    }
    else {
      mAircraftsActiveImage.setColorFilter(Color.LTGRAY);
      if(mAircraftsActive == 0) {
        mAircraftsActive = 2;
        mSpeechService.announceEvent("Aircraft alerts are back on", () -> {
        });
      }
      else if(mAircraftsActive == 1) {
        mAircraftsActive = 2;
        mSpeechService.announceEvent("Aircraft alerts are on", () -> {
        });
      }
    }

    mHandler.postDelayed(() -> {
      // Filter out reports beyond configured distance
      List<Alert> inRangeAircrafts = new ArrayList<>();
      for(Alert aircraft : aircrafts) {
        if(aircraft.distance <= Configuration.AIRCRAFTS_MAX_DISTANCE) {
          inRangeAircrafts.add(aircraft);
        }
      }

      // Update existing aircraft state vectors with their new position
      List<Alert> newAircrafts = new ArrayList<>();
      List<Alert> mAircrafts = mAlertsAdapter.getAircraftAlerts();
      for(Alert aircraft : inRangeAircrafts) {
        for(Alert mAircraft : mAircrafts) {
          if(aircraft.isSameAircraft(mAircraft)) {
            Log.i(TAG, String.format("existing aircraft state vector with new distance %f", aircraft.distance));
            mAircraft.distance = aircraft.distance;
            mAircraft.latitude = aircraft.latitude;
            mAircraft.longitude = aircraft.longitude;
            mAircraft.bearing = aircraft.bearing;
            aircraft = mAircraft;
            break;
          }
        }
        newAircrafts.add(aircraft);
      }

      // Sort aircraft state vectors by priority
      newAircrafts.sort(Comparator.comparingInt(o -> o.priority));

      mAlertsAdapter.setAircraftAlerts(newAircrafts, () -> {
      });
    }, MESSAGE_TOKEN, 1);
  }

  public void onSettingsClick(View v) {
    Log.i(TAG, "onSettingsClick");
    Intent intent = new Intent(this, SettingsMenuActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(intent);
    finish();
  }
}
