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

import androidx.recyclerview.widget.RecyclerView;

import com.jsd.x761.nexus.Nexus.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A view adapter that displays and announce a list of threats.
 */
public class ThreatsAdapter extends RecyclerView.Adapter<ThreatsAdapter.ViewHolder> {
  private static final String TAG = "THREATS_ADAPTER";
  public static final String MESSAGE_TOKEN = ThreatsActivity.MESSAGE_TOKEN;

  private final ThreatsActivity mActivity;
  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private final List<Threat> mAlerts = new ArrayList<>();
  private final List<Threat> mReports = new ArrayList<>();
  private final List<Threat> mAircrafts = new ArrayList<>();
  private final List<Threat> mItems = new ArrayList<>();

  public ThreatsAdapter(ThreatsActivity activity) {
    mActivity = activity;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup vg, int vt) {
    Log.i(TAG, "onCreateViewHolder");
    View v = LayoutInflater.from(vg.getContext()).inflate(R.layout.threat_item, vg, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(ViewHolder vh, int pos) {
    Log.i(TAG, String.format("onCreateViewHolder pos %d", pos));

    Threat threat = mItems.get(pos);

    // Show the class of threat
    String threat_class = "";
    switch(threat.threatClass) {
      case Threat.ALERT_CLASS_RADAR:
        threat_class = "Radar";
        break;
      case Threat.ALERT_CLASS_LASER:
        threat_class = "Laser";
        break;
      case Threat.ALERT_CLASS_SPEED_CAM:
        threat_class = "Speed Cam";
        break;
      case Threat.ALERT_CLASS_RED_LIGHT_CAM:
        threat_class = "Red Light Cam";
        break;
      case Threat.ALERT_CLASS_USER_MARK:
        threat_class = "User Mark";
        break;
      case Threat.ALERT_CLASS_LOCKOUT:
        threat_class = "Lockout";
        break;
      case Threat.ALERT_CLASS_REPORT:
        if("POLICE".equals(threat.type)) {
          threat_class = "Speed Trap";
        }
        else if("ACCIDENT".equals(threat.type)) {
          threat_class = "Accident";
        }
        else {
          threat_class = "Hazard";
        }
        break;
      case Threat.ALERT_CLASS_AIRCRAFT:
        threat_class = threat.type;
        break;
    }

    if(threat.threatClass == Threat.ALERT_CLASS_RADAR) {
      // For radar alerts, show the threat intensity, band, frequency and a
      // default 12 o'clock direction as the DS1 isn't directional
      vh.typeText.setText(threat_class);
      vh.strengthProgressBar.setProgress((int)threat.intensity);

      String band = switch(threat.band) {
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

      if(threat.frequency < 1) {
        vh.frequencyOrDistanceText.setText("");
      }
      else {
        DecimalFormat df = new DecimalFormat("0.#");
        vh.frequencyOrDistanceText.setText(String.format("%s GHz", df.format(threat.frequency)));
      }
      vh.bearingText.setText("12 o'clock");
    }
    else if(threat.threatClass == Threat.ALERT_CLASS_LASER) {
      // For laser alerts, show a max intensity and a default 12 o'clock
      // direction
      vh.typeText.setText(threat_class);
      vh.strengthProgressBar.setProgress(100);
      vh.bearingText.setText("12 o'clock");
      vh.frequencyOrDistanceText.setText("Lidar");
      vh.banddOrLocationText.setText("");
    }
    else if(threat.threatClass == Threat.ALERT_CLASS_REPORT) {
      // For crow-sourced reports, show the report direction in clock hour
      // format, the distance and the reported street or city
      vh.typeText.setText(threat_class);
      vh.frequencyOrDistanceText.setText("");
      vh.bearingText.setText(String.format("%d o'clock", threat.hour));
      vh.strengthProgressBar.setProgress(Math.round(Geospatial.getStrength(threat.distance, Configuration.REPORTS_MAX_DISTANCE) * 100.0f));
      vh.banddOrLocationText.setText(threat.street.length() != 0 ? threat.street : threat.city);
      if(threat.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        vh.frequencyOrDistanceText.setText(String.format("%s miles", df.format(threat.distance)));
      }
      else {
        vh.frequencyOrDistanceText.setText("");
      }
    }
    else if(threat.threatClass == Threat.ALERT_CLASS_AIRCRAFT) {
      // For aircraft vector states, show the report direction in clock hour
      // format, the distance and the reported street or city
      if(threat.manufacturer.length() != 0) {
        String manufacturer = threat.manufacturer.substring(0, 1).toUpperCase() + threat.manufacturer.substring(1).toLowerCase();
        vh.typeText.setText(String.format("%s %s", manufacturer, threat_class));
      }
      else {
        vh.typeText.setText(threat_class);
      }
      vh.frequencyOrDistanceText.setText("");
      vh.bearingText.setText(String.format("%d o'clock", threat.hour));
      vh.strengthProgressBar.setProgress(Math.round(Geospatial.getStrength(threat.distance, Configuration.AIRCRAFTS_MAX_DISTANCE) * 100.0f));
      vh.banddOrLocationText.setText(threat.owner.length() != 0 ? threat.owner : "Unidentified");
      if(threat.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        vh.frequencyOrDistanceText.setText(String.format("%s miles", df.format(threat.distance)));
      }
      else {
        vh.frequencyOrDistanceText.setText("");
      }
    }
    else {
      // For other types of threats, just show a max intensity and a default
      // 12 o'clock direction
      vh.typeText.setText(threat_class);
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

  public List<Threat> getAlerts() {
    return mAlerts;
  }

  public List<Threat> getReports() {
    return mReports;
  }

  public List<Threat> getAircrafts() {
    return mAircrafts;
  }

  private void playThreatSound(List<Threat> threats, int pos, int max, Runnable onDone) {
    Log.i(TAG, String.format("playThreatSound %d", pos));
    Threat threat = threats.get(pos);

    String earcon;
    if(threat.threatClass == Threat.ALERT_CLASS_RADAR) {
      earcon = switch(threat.band) {
        case Threat.ALERT_BAND_X -> "[s2]";
        case Threat.ALERT_BAND_K, Threat.ALERT_BAND_POP_K -> "[s4]";
        case Threat.ALERT_BAND_KA -> "[s3]";
        case Threat.ALERT_BAND_MRCD, Threat.ALERT_BAND_MRCT -> "[s10]";
        case Threat.ALERT_BAND_GT3, Threat.ALERT_BAND_GT4 -> "[s6]";
        default -> "[s6]";
      };
    }
    else {
      earcon = switch(threat.threatClass) {
        case Threat.ALERT_CLASS_LASER -> "[s5]";
        case Threat.ALERT_CLASS_SPEED_CAM, Threat.ALERT_CLASS_RED_LIGHT_CAM -> "s10";
        case Threat.ALERT_CLASS_REPORT -> "[s8]";
        case Threat.ALERT_CLASS_USER_MARK, Threat.ALERT_CLASS_LOCKOUT -> "[s9]";
        case Threat.ALERT_CLASS_AIRCRAFT -> "[s7]";
        default -> "[s6]";
      };
    }

    if(pos < max) {
      // Play the sound indicating the class of threat on the voice call stream
      Runnable nextThreatTask = () -> {
        Log.i(TAG, "playThreatSound.onDone.run()");
        onDone.run();
      };
      mHandler.postDelayed(nextThreatTask, MESSAGE_TOKEN, Configuration.AUDIO_EARCON_TIMER);
      mActivity.getSpeechService().playEarcon(earcon);
    }
  }

  protected void playThreatInfo(List<Threat> threats, int pos, int max, Runnable onDone) {
    Log.i(TAG, String.format("playThreatInfo %d", pos));
    Threat threat = threats.get(pos);

    Log.i(TAG, String.format("playThreatInfo announced %d", threat.announced));
    if(threat.announced > 0) {
      // Only announce the info about a threat once, or again as a later reminder
      // once it gets closer, in that case its announce count will have been
      // reset to zero
      Log.i(TAG, "playThreatInfo.onDone.run()");
      onDone.run();
      return;
    }

    String threat_class = "";
    switch(threat.threatClass) {
      case Threat.ALERT_CLASS_RADAR:
        threat_class = "Radar";
        break;
      case Threat.ALERT_CLASS_LASER:
        threat_class = "Laser";
        break;
      case Threat.ALERT_CLASS_SPEED_CAM:
        threat_class = "Speed Cam";
        break;
      case Threat.ALERT_CLASS_RED_LIGHT_CAM:
        threat_class = "Red Light Cam";
        break;
      case Threat.ALERT_CLASS_USER_MARK:
        threat_class = "User Mark";
        break;
      case Threat.ALERT_CLASS_LOCKOUT:
        threat_class = "Lockout";
        break;
      case Threat.ALERT_CLASS_REPORT:
        if("POLICE".equals(threat.type)) {
          threat_class = "Speed Trap";
        }
        else if("ACCIDENT".equals(threat.type)) {
          threat_class = "Accident";
        }
        else {
          threat_class = "Hazard";
        }
        break;
      case Threat.ALERT_CLASS_AIRCRAFT:
        threat_class = threat.type;
        break;
    }

    String speech = "";
    if(threat.threatClass == Threat.ALERT_CLASS_RADAR) {
      // For radar threats, the speech announcement includes the class of
      // threat, the band and frequency
      String band = switch(threat.band) {
        case Threat.ALERT_BAND_X -> "X band";
        case Threat.ALERT_BAND_K -> "K band";
        case Threat.ALERT_BAND_KA -> "K A band";
        case Threat.ALERT_BAND_POP_K -> "Pop K band";
        case Threat.ALERT_BAND_MRCD -> "M R C D";
        case Threat.ALERT_BAND_MRCT -> "M R C T";
        case Threat.ALERT_BAND_GT3 -> "G T 3";
        case Threat.ALERT_BAND_GT4 -> "G T 4";
        default -> "";
      };

      speech += band;
      if(threat.frequency >= 1) {
        DecimalFormat df = new DecimalFormat("0.#");
        speech += String.format(" %s", df.format(threat.frequency));
      }
      speech += String.format(" %s", threat_class);
    }
    else if(threat.threatClass == Threat.ALERT_CLASS_REPORT) {
      // For crowd-sourced reports, the speech announcement includes the class
      // of threat, the direction in clock hour form, the distance and the
      // reported street or city
      speech += threat_class;
      if(threat.hour != 0) {
        speech += String.format(" at %d o'clock", threat.hour);
      }
      if(threat.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        speech += String.format(" %s miles away", df.format(threat.distance));
      }
      if(threat.street.length() != 0) {
        speech += String.format(" on %s", threat.street);
      }
      else if(threat.city.length() != 0) {
        speech += String.format(" in %s", threat.city);
      }
    }
    else if(threat.threatClass == Threat.ALERT_CLASS_AIRCRAFT) {
      // For aircraft state vectors, the speech announcement includes the
      // class of threat, the direction in clock hour form, the distance and
      // the reported street or city
      if(threat.owner.length() != 0) {
        speech += threat.owner;
      }
      else {
        speech += "Unidentified";
      }
      if(threat.manufacturer.length() != 0) {
        String manufacturer = threat.manufacturer.substring(0, 1).toUpperCase() + threat.manufacturer.substring(1).toLowerCase();
        speech += String.format(" %s", manufacturer);
      }
      speech += String.format(" %s", threat_class);
      if(threat.hour != 0) {
        speech += String.format(" at %d o'clock", threat.hour);
      }
      if(threat.distance != 0) {
        DecimalFormat df = new DecimalFormat("0.#");
        speech += String.format(" %s miles away", df.format(threat.distance));
      }
    }
    else {
      // For other classes of threats, the speech announcement just includes
      // the threat class
      speech += threat_class;
    }

    if(pos < max) {
      String uuid = UUID.randomUUID().toString();
      mActivity.getSpeechService().addOnUtteranceProgressCallback(uuid, () -> {
        Log.i(TAG, String.format("UtteranceProgressListener.onDone %s", uuid));
        mActivity.getSpeechService().removeOnUtteranceProgressCallback(uuid);
        Log.i(TAG, "playThreatInfo.onDone.run()");
        onDone.run();
      });

      // Play the speech announcement on the voice call stream
      threat.announced += 1;
      mActivity.getSpeechService().playSpeech(speech, uuid);
    }
    else {
      Log.i(TAG, "playThreatInfo.onDone.run()");
      onDone.run();
    }
  }

  protected void announceThreat(List<Threat> threats, String type, int pos, int max, Runnable onDone) {
    Log.i(TAG, String.format("announceThreat %s pos %d", type, pos));
    if(pos == threats.size()) {
      mHandler.postDelayed(() -> {
        Log.i(TAG, "announceThreat.onDone.run()");
        onDone.run();
      }, MESSAGE_TOKEN, 1);
      return;
    }

    // Play a sound for each threat
    Log.i(TAG, String.format("playThreatSound() %s %d", type, pos));
    playThreatSound(threats, pos, max, () -> {
      // Play a spech announcement for the threat
      mHandler.postDelayed(() -> {
        Log.i(TAG, String.format("playThreatInfo() %s %d", type, pos));
        playThreatInfo(threats, pos, max, () -> {
          mHandler.postDelayed(() -> {
            // Announce the next threat
            Log.i(TAG, String.format("announceThreat() %s %d", type, pos + 1));
            announceThreat(threats, type, pos + 1, max, onDone);
          }, MESSAGE_TOKEN, 1);
        });
      }, MESSAGE_TOKEN, 1);
    });
  }

  public void setAlerts(List<Threat> alerts, Runnable onDone) {
    Log.i(TAG, "setAlerts");
    // Combine alerts, reports and aircrafts in a single list of threats
    mAlerts.clear();
    mAlerts.addAll(alerts);
    mItems.clear();
    mItems.addAll(mAlerts);
    mItems.addAll(mReports);
    mItems.addAll(mAircrafts);
    mActivity.runOnUiThread(() -> notifyDataSetChanged());

    if(mAlerts.size() > 0) {
      // Announce the alerts, keeping track of how many times each one
      // announced
      List<Threat> announcements = new ArrayList<>();
      for(Threat alert : mAlerts) {
        if(!alert.muted) {
          announcements.add(alert);
        }
      }
      if(announcements.size() != 0) {
        // Optionally start an async task to proactively abandon audio focus
        // after a few seconds
        Runnable abandonAudioTask = null;
        if(Configuration.ABANDON_AUDIO_TIMER != 0) {
          abandonAudioTask = () -> {
            Log.i(TAG, "abandonAudioFocus()");
            mActivity.getSpeechService().abandonAudioFocus(() -> {
              Log.i(TAG, "setAlerts.onDone.run()");
              onDone.run();
            });
          };
          Log.i(TAG, "postDelayed() abandonAudioTask");
          mHandler.postDelayed(abandonAudioTask, MESSAGE_TOKEN, Configuration.ABANDON_AUDIO_TIMER);
        }

        // Request audio focus and duck current audio
        Log.i(TAG, "requestAudioFocus()");
        mActivity.getSpeechService().requestAudioFocus(() -> {
          // Announce the alerts
          Log.i(TAG, "announceThreat() alert pos 0");
          announceThreat(announcements, "alert", 0, Configuration.ALERTS_MAX_ANNOUNCE_COUNT, () -> {
            if(abandonAudioTask != null) {
              Log.i(TAG, "removeCallbacks abandonAudioTask");
              mHandler.removeCallbacks(abandonAudioTask);
            }

            // Abandon audio focus once done
            Log.i(TAG, "abandonAudioFocus()");
            mActivity.getSpeechService().abandonAudioFocus(() -> {
              Log.i(TAG, "setAlerts.onDone.run()");
              onDone.run();
            });
          });
        });
      }
    }
    else {
      Log.i(TAG, "setAlerts.onDone.run()");
      onDone.run();
    }
  }

  public void setReports(List<Threat> reports, Runnable onDone) {
    Log.i(TAG, "setReports");
    // Combine alerts, reports and aircrafts in a single list of threats
    mReports.clear();
    mReports.addAll(reports);
    mItems.clear();
    mItems.addAll(mAlerts);
    mItems.addAll(mReports);
    mItems.addAll(mAircrafts);
    mActivity.runOnUiThread(() -> notifyDataSetChanged());

    if(mReports.size() > 0) {
      // Announce the reports, keeping track of how many times each one
      // announced
      List<Threat> announcements = new ArrayList<>();
      for(Threat report : mReports) {
        // report.announced = report.announced + 1;
        announcements.add(report);
      }

      // Optionally start an async task to proactively abandon audio focus
      // after a few seconds
      Runnable abandonAudioTask = null;
      if(Configuration.ABANDON_AUDIO_TIMER != 0) {
        abandonAudioTask = () -> {
          Log.i(TAG, "abandonAudioFocus()");
          mActivity.getSpeechService().abandonAudioFocus(() -> {
            Log.i(TAG, "setReports.onDone.run()");
            onDone.run();
          });
        };
        Log.i(TAG, "postDelayed() abandonAudioTask");
        mHandler.postDelayed(abandonAudioTask, MESSAGE_TOKEN, Configuration.ABANDON_AUDIO_TIMER);
      }

      // Request audio focus and duck current audio
      Log.i(TAG, "requestAudioFocus()");
      mActivity.getSpeechService().requestAudioFocus(() -> {
        // Announce the reports
        Log.i(TAG, "announceThreat() report pos 0");
        announceThreat(announcements, "report", 0, Configuration.REPORTS_MAX_ANNOUNCE_COUNT, () -> {
          if(abandonAudioTask != null) {
            Log.i(TAG, "removeCallbacks abandonAudioTask");
            mHandler.removeCallbacks(abandonAudioTask);
          }

          // Abandon audio focus once done
          Log.i(TAG, "abandonAudioFocus()");
          mActivity.getSpeechService().abandonAudioFocus(() -> {
            Log.i(TAG, "setReports.onDone.run()");
            onDone.run();
          });
        });
      });
    }
    else {
      Log.i(TAG, "setReports.onDone.run()");
      onDone.run();
    }
  }

  public void setAircrafts(List<Threat> aircrafts, Runnable onDone) {
    Log.i(TAG, "setAircrafts");
    // Combine alerts, aircrafts and aircrafts in a single list of threats
    mAircrafts.clear();
    mAircrafts.addAll(aircrafts);
    mItems.clear();
    mItems.addAll(mAlerts);
    mItems.addAll(mReports);
    mItems.addAll(mAircrafts);
    mActivity.runOnUiThread(() -> notifyDataSetChanged());

    if(mAircrafts.size() > 0) {
      // Announce the aircrafts, keeping track of how many times each one
      // announced
      List<Threat> announcements = new ArrayList<>();
      for(Threat aircraft : mAircrafts) {
        // aircraft.announced = aircraft.announced + 1;
        announcements.add(aircraft);
      }

      // Optionally start an async task to proactively abandon audio focus
      // after a few seconds
      Runnable abandonAudioTask = null;
      if(Configuration.ABANDON_AUDIO_TIMER != 0) {
        abandonAudioTask = () -> {
          Log.i(TAG, "abandonAudioFocus()");
          mActivity.getSpeechService().abandonAudioFocus(() -> {
            Log.i(TAG, "setAircrafts.onDone.run()");
            onDone.run();
          });
        };
        Log.i(TAG, "postDelayed() abandonAudioTask");
        mHandler.postDelayed(abandonAudioTask, MESSAGE_TOKEN, Configuration.ABANDON_AUDIO_TIMER);
      }

      // Request audio focus and duck current audio
      Log.i(TAG, "requestAudioFocus()");
      mActivity.getSpeechService().requestAudioFocus(() -> {
        // Announce the aircrafts
        Log.i(TAG, "announceThreat() aircraft pos 0");
        announceThreat(announcements, "aircraft", 0, Configuration.AIRCRAFTS_MAX_ANNOUNCE_COUNT, () -> {
          if(abandonAudioTask != null) {
            Log.i(TAG, "removeCallbacks abandonAudioTask");
            mHandler.removeCallbacks(abandonAudioTask);
          }

          // Abandon audio focus once done
          Log.i(TAG, "abandonAudioFocus()");
          mActivity.getSpeechService().abandonAudioFocus(() -> {
            Log.i(TAG, "setReports.onDone.run()");
            onDone.run();
          });
        });
      });
    }
    else {
      Log.i(TAG, "setAircrafts.onDone.run()");
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
}
