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

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jsd.x761.nexus.Nexus.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A view adapter that displays and announce a list of alerts.
 */
public class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.ViewHolder> {
  private static final String TAG = "ALERTS_ADAPTER";
  public static final String MESSAGE_TOKEN = AlertsActivity.MESSAGE_TOKEN;

  private final AlertsActivity mActivity;
  private final SpeechService mSpeechService;
  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private final List<Alert> mRadarAlerts = new ArrayList<>();
  private final List<Alert> mReportAlerts = new ArrayList<>();
  private final List<Alert> mAircraftAlerts = new ArrayList<>();
  private final List<Alert> mItems = new ArrayList<>();
  private Runnable mReportsReminderTask;
  private Runnable mAircraftsReminderTask;
  private final String mReportsSourceName;
  private boolean mReportsAllClear = true;
  private boolean mAircraftsAllClear = true;
  private Runnable mAllClearTask;

  public AlertsAdapter(AlertsActivity activity, SpeechService speechService, String reportsSourceName) {
    mActivity = activity;
    mSpeechService = speechService;
    mReportsSourceName = reportsSourceName;

    // Play reminders regularly while there are active reports and aircraft
    // state vectors
    mReportsReminderTask = () -> {
      if(mReportAlerts.size() > 0) {
        List<Alert> announces = new ArrayList<>();
        announces.add(mReportAlerts.get(0));
        // Request audio focus and duck current audio
        Log.i(TAG, "requestAudioFocus()");
        mSpeechService.requestAudioFocus(() -> {

          // Announce the reports
          Log.i(TAG, "playAlertAnnounce() report pos 0");
          AlertsAdapter.this.playAlertAnnounce(announces, "report", 0, 0, 1, () -> {

            // Abandon audio focus once done
            Log.i(TAG, "abandonAudioFocus()");
            mSpeechService.abandonAudioFocus(() -> {
              Log.i(TAG, "setReportAlerts.onDone.run()");
            });
          });
        });
      }
      mHandler.postDelayed(mReportsReminderTask, MESSAGE_TOKEN, Configuration.REPORTS_REMINDER_TIMER);
    };
    mHandler.postDelayed(mReportsReminderTask, MESSAGE_TOKEN, Configuration.REPORTS_REMINDER_TIMER);

    mAircraftsReminderTask = () -> {
      if(mAircraftAlerts.size() > 0) {
        List<Alert> announces = new ArrayList<>();
        announces.add(mAircraftAlerts.get(0));
        // Request audio focus and duck current audio
        Log.i(TAG, "requestAudioFocus()");
        mSpeechService.requestAudioFocus(() -> {

          // Announce the aircraft state vectors
          Log.i(TAG, "playAlertAnnounce() report pos 0");
          AlertsAdapter.this.playAlertAnnounce(announces, "aircraft", 0, 0, 1, () -> {

            // Abandon audio focus once done
            Log.i(TAG, "abandonAudioFocus()");
            mSpeechService.abandonAudioFocus(() -> {
              Log.i(TAG, "setReportAlerts.onDone.run()");
            });
          });
        });
      }
      mHandler.postDelayed(mAircraftsReminderTask, MESSAGE_TOKEN, Configuration.AIRCRAFTS_REMINDER_TIMER);
    };
    mHandler.postDelayed(mAircraftsReminderTask, MESSAGE_TOKEN, Configuration.AIRCRAFTS_REMINDER_TIMER);

    mAllClearTask = () -> {
      if(mReportAlerts.size() == 0 && !mReportsAllClear) {
        mReportsAllClear = true;
        mSpeechService.announceEvent(String.format("%s alerts are all clear now", mReportsSourceName), () -> {
        });
      }
      if(mAircraftAlerts.size() == 0 && !mAircraftsAllClear) {
        mAircraftsAllClear = true;
        mSpeechService.announceEvent("Aircraft alerts are all clear now", () -> {
        });
      }
      mHandler.postDelayed(mAllClearTask, MESSAGE_TOKEN, Configuration.CLEAR_REMINDER_TIMER);
    };
    mHandler.postDelayed(mAllClearTask, MESSAGE_TOKEN, Configuration.CLEAR_REMINDER_TIMER);
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(ViewGroup vg, int vt) {
    Log.i(TAG, "onCreateViewHolder");
    View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.alert_item, vg, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder vh, int pos) {
    Log.i(TAG, String.format("onCreateViewHolder pos %d", pos));

    Alert alert = mItems.get(pos);

    // Show the class of alert
    String alertClass = "";
    switch(alert.alertClass) {
      case Alert.ALERT_CLASS_RADAR:
        alertClass = "Radar";
        break;
      case Alert.ALERT_CLASS_LASER:
        alertClass = "Laser";
        break;
      case Alert.ALERT_CLASS_SPEED_CAM:
        alertClass = "Speed Cam";
        break;
      case Alert.ALERT_CLASS_RED_LIGHT_CAM:
        alertClass = "Red Light Cam";
        break;
      case Alert.ALERT_CLASS_USER_MARK:
        alertClass = "User Mark";
        break;
      case Alert.ALERT_CLASS_LOCKOUT:
        alertClass = "Lockout";
        break;
      case Alert.ALERT_CLASS_REPORT:
        if("POLICE".equals(alert.type)) {
          alertClass = "Speed Trap";
        }
        else if("ACCIDENT".equals(alert.type)) {
          alertClass = "Accident";
        }
        else {
          alertClass = "Hazard";
        }
        break;
      case Alert.ALERT_CLASS_AIRCRAFT:
        alertClass = alert.type;
        break;
    }

    if(alert.alertClass == Alert.ALERT_CLASS_RADAR) {
      // For radar alerts, show the alert intensity, band, frequency and a
      // default 12 o'clock direction as the DS1 isn't directional
      vh.typeText.setText(alertClass);
      vh.strengthProgressBar.setProgress((int)alert.intensity);

      String band = switch(alert.band) {
        case 0 -> "X band";
        case 1 -> "K band";
        case 2 -> "Ka band";
        case 3 -> "Pop K band";
        case 4 -> "MRCD";
        case 5 -> "MRCT";
        case 6 -> "GT3";
        case 7 -> "GT4";
        default -> "";
      };
      vh.banddOrLocationText.setText(band);

      if(alert.frequency < 1) {
        vh.frequencyOrDistanceText.setText("");
      }
      else {
        DecimalFormat df = new DecimalFormat("0.#");
        vh.frequencyOrDistanceText.setText(String.format("%s GHz", df.format(alert.frequency)));
      }
      vh.bearingText.setText("12 o'clock");
    }
    else if(alert.alertClass == Alert.ALERT_CLASS_LASER) {
      // For laser alerts, show a max intensity and a default 12 o'clock
      // direction
      vh.typeText.setText(alertClass);
      vh.strengthProgressBar.setProgress(100);
      vh.bearingText.setText("12 o'clock");
      vh.frequencyOrDistanceText.setText("Lidar");
      vh.banddOrLocationText.setText("");
    }
    else if(alert.alertClass == Alert.ALERT_CLASS_REPORT) {
      // For crow-sourced reports, show the report direction in clock bearing
      // format, the distance and the reported street or city
      vh.typeText.setText(alertClass);
      vh.frequencyOrDistanceText.setText("");
      vh.bearingText.setText(String.format("%d o'clock", alert.bearing));
      vh.strengthProgressBar.setProgress(Math.round(Geospatial.getStrength(alert.distance, Configuration.REPORTS_MAX_DISTANCE) * 100.0f));
      vh.banddOrLocationText.setText(alert.street.length() != 0 ? alert.street : alert.city);
      if(alert.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        vh.frequencyOrDistanceText.setText(String.format("%s %s", df.format(alert.distance), alert.distance >= 2.0f ? "miles" : "mile"));
      }
      else {
        vh.frequencyOrDistanceText.setText("");
      }
    }
    else if(alert.alertClass == Alert.ALERT_CLASS_AIRCRAFT) {
      // For aircraft vector states, show the report direction in clock bearing
      // format, the distance and the reported street or city
      if(alert.manufacturer.length() != 0) {
        String manufacturer = alert.manufacturer.substring(0, 1).toUpperCase() + alert.manufacturer.substring(1).toLowerCase();
        vh.typeText.setText(String.format("%s %s", manufacturer, alertClass));
      }
      else {
        vh.typeText.setText(alertClass);
      }
      vh.frequencyOrDistanceText.setText("");
      vh.bearingText.setText(String.format("%d o'clock", alert.bearing));
      vh.strengthProgressBar.setProgress(Math.round(Geospatial.getStrength(alert.distance, Configuration.AIRCRAFTS_MAX_DISTANCE) * 100.0f));
      vh.banddOrLocationText.setText(alert.owner.length() != 0 ? alert.owner : "Unidentified");
      if(alert.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        vh.frequencyOrDistanceText.setText(String.format("%s %s", df.format(alert.distance), alert.distance >= 2.0f ? "miles" : "mile"));
      }
      else {
        vh.frequencyOrDistanceText.setText("");
      }
    }
    else {
      // For other types of alerts, just show a max intensity and a default
      // 12 o'clock direction
      vh.typeText.setText(alertClass);
      vh.strengthProgressBar.setProgress(100);
      vh.bearingText.setText("12 o'clock");
      vh.frequencyOrDistanceText.setText("");
      vh.banddOrLocationText.setText("");
    }
  }

  @Override
  public int getItemCount() {
    return mItems.size();
  }

  public List<Alert> getRadarAlerts() {
    return mRadarAlerts;
  }

  public List<Alert> getReportAlerts() {
    return mReportAlerts;
  }

  public List<Alert> getAircraftAlerts() {
    return mAircraftAlerts;
  }

  private void playEarconAnnounce(List<Alert> alerts, int pos, int maxEarcons, Runnable onDone) {
    Log.i(TAG, String.format("playEarconAnnounce %d", pos));
    if(pos >= maxEarcons) {
      Log.i(TAG, "playEarconAnnounce.onDone.run()");
      onDone.run();
      return;
    }

    Alert alert = alerts.get(pos);
    String earcon;
    if(alert.alertClass == Alert.ALERT_CLASS_RADAR) {
      earcon = switch(alert.band) {
        case Alert.ALERT_BAND_X -> "[s2]";
        case Alert.ALERT_BAND_K, Alert.ALERT_BAND_POP_K -> "[s4]";
        case Alert.ALERT_BAND_KA -> "[s3]";
        case Alert.ALERT_BAND_MRCD, Alert.ALERT_BAND_MRCT -> "[s10]";
        case Alert.ALERT_BAND_GT3, Alert.ALERT_BAND_GT4 -> "[s6]";
        default -> "[s6]";
      };
    }
    else {
      earcon = switch(alert.alertClass) {
        case Alert.ALERT_CLASS_LASER -> "[s5]";
        case Alert.ALERT_CLASS_SPEED_CAM, Alert.ALERT_CLASS_RED_LIGHT_CAM -> "s10";
        case Alert.ALERT_CLASS_REPORT -> "[s8]";
        case Alert.ALERT_CLASS_USER_MARK, Alert.ALERT_CLASS_LOCKOUT -> "[s9]";
        case Alert.ALERT_CLASS_AIRCRAFT -> "[s7]";
        default -> "[s6]";
      };
    }

    // Play the sound indicating the class of alert on the voice call stream
    Runnable nextAlertTask = () -> {
      Log.i(TAG, "playEarconAnnounce.onDone.run()");
      onDone.run();
    };
    mSpeechService.playEarcon(earcon);
    mHandler.postDelayed(nextAlertTask, MESSAGE_TOKEN, Configuration.AUDIO_EARCON_TIMER);
  }

  protected void playSpeechAnnounce(List<Alert> alerts, int pos, int maxSpeech, Runnable onDone) {
    Log.i(TAG, String.format("playSpeechAnnounce %d", pos));
    if(pos >= maxSpeech) {
      Log.i(TAG, "playSpeechAnnounce.onDone.run()");
      onDone.run();
      return;
    }

    Alert alert = alerts.get(pos);
    if(alert.announced > 1) {
      Log.i(TAG, String.format("alert announced %d", alert.announced));
      Log.i(TAG, "playSpeechAnnounce.onDone.run()");
      onDone.run();
      return;
    }

    String alertClass = "";
    switch(alert.alertClass) {
      case Alert.ALERT_CLASS_RADAR:
        alertClass = "Radar";
        break;
      case Alert.ALERT_CLASS_LASER:
        alertClass = "Laser";
        break;
      case Alert.ALERT_CLASS_SPEED_CAM:
        alertClass = "Speed Cam";
        break;
      case Alert.ALERT_CLASS_RED_LIGHT_CAM:
        alertClass = "Red Light Cam";
        break;
      case Alert.ALERT_CLASS_USER_MARK:
        alertClass = "User Mark";
        break;
      case Alert.ALERT_CLASS_LOCKOUT:
        alertClass = "Lockout";
        break;
      case Alert.ALERT_CLASS_REPORT:
        if("POLICE".equals(alert.type)) {
          alertClass = "Speed Trap";
        }
        else if("ACCIDENT".equals(alert.type)) {
          alertClass = "Accident";
        }
        else {
          alertClass = "Hazard";
        }
        break;
      case Alert.ALERT_CLASS_AIRCRAFT:
        alertClass = alert.type;
        break;
    }

    String speech = "";
    if(alert.alertClass == Alert.ALERT_CLASS_RADAR) {
      // For radar alerts, the speech announce includes the class of
      // alert, the band and frequency
      String band = switch(alert.band) {
        case Alert.ALERT_BAND_X -> "X band";
        case Alert.ALERT_BAND_K -> "K band";
        case Alert.ALERT_BAND_KA -> "K A band";
        case Alert.ALERT_BAND_POP_K -> "Pop K band";
        case Alert.ALERT_BAND_MRCD -> "M R C D";
        case Alert.ALERT_BAND_MRCT -> "M R C T";
        case Alert.ALERT_BAND_GT3 -> "G T 3";
        case Alert.ALERT_BAND_GT4 -> "G T 4";
        default -> "";
      };

      speech += band;
      if(alert.frequency >= 1) {
        DecimalFormat df = new DecimalFormat("0.#");
        speech += String.format(" %s", df.format(alert.frequency));
      }
      speech += String.format(" %s", alertClass);
    }
    else if(alert.alertClass == Alert.ALERT_CLASS_REPORT) {
      // For crowd-sourced reports, the speech announce includes the class
      // of alert, the direction in clock bearing form, the distance and the
      // reported street or city
      speech += alertClass;
      if(alert.announced > 1) {
        speech += " now";
      }
      if(alert.bearing != 0) {
        speech += String.format(" at %d o'clock", alert.bearing);
      }
      if(alert.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        speech += String.format(" %s %s away", df.format(alert.distance), alert.distance >= 2.0f ? "miles" : "mile");
      }
      if(alert.street.length() != 0) {
        speech += String.format(" on %s", alert.street);
      }
      else if(alert.city.length() != 0) {
        speech += String.format(" in %s", alert.city);
      }
    }
    else if(alert.alertClass == Alert.ALERT_CLASS_AIRCRAFT) {
      // For aircraft state vectors, the speech announce includes the
      // class of alert, the direction in clock bearing form, the distance and
      // the reported street or city
      if(alert.owner.length() != 0) {
        speech += alert.owner;
      }
      else {
        speech += "Unidentified";
      }
      if(alert.manufacturer.length() != 0) {
        String manufacturer = alert.manufacturer.substring(0, 1).toUpperCase() + alert.manufacturer.substring(1).toLowerCase();
        speech += String.format(" %s", manufacturer);
      }
      speech += String.format(" %s", alertClass);
      if(alert.announced > 1) {
        speech += " now";
      }
      if(alert.bearing != 0) {
        speech += String.format(" at %d o'clock", alert.bearing);
      }
      if(alert.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        speech += String.format(" %s %s away", df.format(alert.distance), alert.distance >= 2.0f ? "miles" : "mile");
      }
    }
    else {
      // For other classes of alerts, the speech announce just includes
      // the alert class
      speech += alertClass;
    }

    String uuid = UUID.randomUUID().toString();
    mSpeechService.addOnUtteranceProgressCallback(uuid, () -> {
      Log.i(TAG, String.format("UtteranceProgressListener.onDone %s", uuid));
      mSpeechService.removeOnUtteranceProgressCallback(uuid);
      Log.i(TAG, "playSpeechAnnounce.onDone.run()");
      onDone.run();
    });

    // Play the speech announce on the voice call stream
    mSpeechService.playSpeech(speech, uuid);
  }

  protected void playAlertAnnounce(List<Alert> alerts, String type, int pos, int maxSpeech, int maxEarcons, Runnable onDone) {
    Log.i(TAG, String.format("playAlertAnnounce %s pos %d", type, pos));
    if(pos == alerts.size()) {
      mHandler.postDelayed(() -> {
        Log.i(TAG, "playAlertAnnounce.onDone.run()");
        onDone.run();
      }, MESSAGE_TOKEN, 1);
      return;
    }

    // Play a sound for each alert
    Log.i(TAG, String.format("playEarconAnnounce() %s %d", type, pos));
    playEarconAnnounce(alerts, pos, maxEarcons, () -> {
      // Play a spech announce for the alert
      mHandler.postDelayed(() -> {
        Log.i(TAG, String.format("playSpeechAnnounce() %s %d", type, pos));
        playSpeechAnnounce(alerts, pos, maxSpeech, () -> {
          mHandler.postDelayed(() -> {
            // Announce the next alert
            Log.i(TAG, String.format("playAlertAnnounce() %s %d", type, pos + 1));
            playAlertAnnounce(alerts, type, pos + 1, maxSpeech, maxEarcons, onDone);
          }, MESSAGE_TOKEN, 1);
        });
      }, MESSAGE_TOKEN, 1);
    });
  }

  public void setRadarAlerts(List<Alert> alerts, Runnable onDone) {
    Log.i(TAG, "setRadarAlerts");
    // Combine alerts, reports and aircrafts in a single list of alerts
    mRadarAlerts.clear();
    mRadarAlerts.addAll(alerts);
    mItems.clear();
    mItems.addAll(mRadarAlerts);
    mItems.addAll(mReportAlerts);
    mItems.addAll(mAircraftAlerts);
    mActivity.runOnUiThread(() -> notifyDataSetChanged());

    if(mRadarAlerts.size() > 0) {
      // Announce the alerts unless they've been muted
      List<Alert> announces = new ArrayList<>();
      for(Alert alert : mRadarAlerts) {
        if(!alert.muted) {
          alert.announced += 1;
          announces.add(alert);
        }
      }
      if(announces.size() != 0) {

        // Request audio focus and duck current audio
        Log.i(TAG, "requestAudioFocus()");
        mSpeechService.requestAudioFocus(() -> {

          // Announce the alerts
          Log.i(TAG, "playAlertAnnounce() alert pos 0");
          playAlertAnnounce(announces, "alert", 0, Configuration.DS1_ALERTS_MAX_SPEECH_ANNOUNCES, Configuration.DS1_ALERTS_MAX_EARCON_ANNOUNCES, () -> {

            // Abandon audio focus once done
            Log.i(TAG, "abandonAudioFocus()");
            mSpeechService.abandonAudioFocus(() -> {
              Log.i(TAG, "setRadarAlerts.onDone.run()");
              onDone.run();
            });
          });
        });
      }
    }
    else {
      Log.i(TAG, "setRadarAlerts.onDone.run()");
      onDone.run();
    }
  }

  public void setReportAlerts(List<Alert> reports, Runnable onDone) {
    Log.i(TAG, "setReportAlerts");
    // Combine alerts, reports and aircrafts in a single list of alerts
    mReportAlerts.clear();
    mReportAlerts.addAll(reports);
    mItems.clear();
    mItems.addAll(mRadarAlerts);
    mItems.addAll(mReportAlerts);
    mItems.addAll(mAircraftAlerts);
    mActivity.runOnUiThread(() -> notifyDataSetChanged());

    if(mReportAlerts.size() > 0) {
      mReportsAllClear = false;

      // Announce the reports if they've not been announced yet or if their
      // distance or bearing has changed significantly since then
      List<Alert> announces = new ArrayList<>();
      for(Alert report : mReportAlerts) {
        if(report.shouldAnnounceReport()) {
          report.announced += 1;
          report.announceDistance = report.distance;
          report.announceBearing = report.bearing;
          announces.add(report);
        }
      }

      if(announces.size() != 0) {

        // Reschedule the reminder task for later as some reports are going
        // to be announced right away
        mHandler.removeCallbacks(mReportsReminderTask);
        mHandler.postDelayed(mReportsReminderTask, MESSAGE_TOKEN, Configuration.REPORTS_REMINDER_TIMER);

        // Request audio focus and duck current audio
        Log.i(TAG, "requestAudioFocus()");
        mSpeechService.requestAudioFocus(() -> {

          // Announce the reports
          Log.i(TAG, "playAlertAnnounce() report pos 0");
          playAlertAnnounce(announces, "report", 0, Configuration.REPORTS_MAX_SPEECH_ANNOUNCES, Configuration.REPORTS_MAX_EARCON_ANNOUNCES, () -> {

            // Abandon audio focus once done
            Log.i(TAG, "abandonAudioFocus()");
            mSpeechService.abandonAudioFocus(() -> {
              Log.i(TAG, "setReportAlerts.onDone.run()");
              onDone.run();
            });
          });
        });
      }
    }
    else {
      Log.i(TAG, "setReportAlerts.onDone.run()");
      onDone.run();
    }
  }

  public void setAircraftAlerts(List<Alert> aircrafts, Runnable onDone) {
    Log.i(TAG, "setAircraftAlerts");
    // Combine alerts, aircrafts and aircrafts in a single list of alerts
    mAircraftAlerts.clear();
    mAircraftAlerts.addAll(aircrafts);
    mItems.clear();
    mItems.addAll(mRadarAlerts);
    mItems.addAll(mReportAlerts);
    mItems.addAll(mAircraftAlerts);
    mActivity.runOnUiThread(() -> notifyDataSetChanged());

    if(mAircraftAlerts.size() > 0) {
      mAircraftsAllClear = false;

      // Announce the aircrafts if they've not been announced yet or if their
      // distance or bearing has changed significantly since then
      List<Alert> announces = new ArrayList<>();
      for(Alert aircraft : mAircraftAlerts) {
        if(aircraft.shouldAnnounceAircraft()) {
          aircraft.announced += 1;
          aircraft.announceDistance = aircraft.distance;
          aircraft.announceBearing = aircraft.bearing;
          announces.add(aircraft);
        }
      }

      if(announces.size() != 0) {

        // Reschedule the reminder task for later as some reports are going
        // to be announced right away
        mHandler.removeCallbacks(mAircraftsReminderTask);
        mHandler.postDelayed(mAircraftsReminderTask, MESSAGE_TOKEN, Configuration.AIRCRAFTS_REMINDER_TIMER);

        // Request audio focus and duck current audio
        Log.i(TAG, "requestAudioFocus()");
        mSpeechService.requestAudioFocus(() -> {

          // Announce the aircrafts
          Log.i(TAG, "playAlertAnnounce() aircraft pos 0");
          playAlertAnnounce(announces, "aircraft", 0, Configuration.AIRCRAFTS_MAX_SPEECH_ANNOUNCES, Configuration.AIRCRAFTS_MAX_EARCON_ANNOUNCES, () -> {

            // Abandon audio focus once done
            Log.i(TAG, "abandonAudioFocus()");
            mSpeechService.abandonAudioFocus(() -> {
              Log.i(TAG, "setReportAlerts.onDone.run()");
              onDone.run();
            });
          });
        });
      }
    }
    else {
      Log.i(TAG, "setAircraftAlerts.onDone.run()");
      onDone.run();
    }
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public TextView typeText;
    public TextView frequencyOrDistanceText;
    public TextView banddOrLocationText;
    public TextView bearingText;
    public ProgressBar strengthProgressBar;

    public ViewHolder(View v) {
      super(v);

      typeText = v.findViewById(R.id.typeText);
      frequencyOrDistanceText = v.findViewById(R.id.frequencyOrDistanceText);
      banddOrLocationText = v.findViewById(R.id.bandOrLocationText);
      bearingText = v.findViewById(R.id.bearingText);
      strengthProgressBar = v.findViewById(R.id.strengthProgressBar);
    }
  }

  public void onDestroy() {
    mHandler.removeCallbacksAndMessages(MESSAGE_TOKEN);
  }
}
