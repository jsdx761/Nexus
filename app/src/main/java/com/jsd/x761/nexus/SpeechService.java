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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;

import com.jsd.x761.nexus.Nexus.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A service that handles speech announcements and audio focus.
 */
public class SpeechService extends Service {
  private static final String TAG = "SPEECH_SERVICE";
  public static final String MESSAGE_TOKEN = "SPEECH_SERVICE_MESSAGES";

  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private boolean mReady = false;
  private final List<Runnable> mReadyCallbacks = new ArrayList<>();
  private final IBinder mBinder;
  private AudioManager mAudioManager;
  private AudioFocusRequest mAudioFocusRequest;
  private int mDuckedAudioMedia = 0;
  private List<Voice> mUKVoices = new ArrayList<>();
  private int mVoiceIndex = -1;
  protected TextToSpeech mTextToSpeech;
  protected Map<String, Runnable> mTextToSpeechCallback = new HashMap<>();

  public class ThisBinder extends Binder {
    public ThisBinder() {
    }

    public SpeechService getService() {
      return SpeechService.this;
    }
  }

  public SpeechService() {
    mBinder = new ThisBinder();
  }

  @Override
  public void onCreate() {
    Log.i(TAG, "onCreate");
    super.onCreate();

    mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

    // Initialize the text to speech engine
    mTextToSpeech = new TextToSpeech(getApplicationContext(), status -> {
      if(status != TextToSpeech.ERROR) {
        mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {

          @Override
          public void onStart(String s) {
            Log.i(TAG, "UtteranceProgressListener.onStart");
            for(int i = 0; i < Configuration.AUDIO_ADJUST_RAISE_COUNT; i++) {
              mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE, 0);
            }
          }

          @Override
          public void onDone(String s) {
            Log.i(TAG, "UtteranceProgressListener.onDone");
            for(int i = 0; i < Configuration.AUDIO_ADJUST_RAISE_COUNT; i++) {
              mAudioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_LOWER, 0);
            }
            Log.i(TAG, "onPlaySpeech.onDone.run()");
            Runnable callback = mTextToSpeechCallback.get(s);
            callback.run();
          }

          @Override
          public void onError(String s) {
            Log.i(TAG, "UtteranceProgressListener.onError");
          }
        });

        // Pick a voice similar to the voice used in the builtin navigation
        // system in a Jaguar F-Pace
        mUKVoices = new ArrayList<>();
        mTextToSpeech.setLanguage(Locale.UK);
        Set<Voice> voices = mTextToSpeech.getVoices();
        for(Voice voice : voices) {
          if(voice.getLocale() == Locale.UK) {
            voice.isNetworkConnectionRequired();
            if(true) {
              mUKVoices.add(voice);
              if(voice.getName().equals("en-gb-x-gbc-local")) {
                mVoiceIndex = mUKVoices.size() - 1;
              }
            }
          }
        }
        Log.i(TAG, String.format("mVoiceIndex %d, name %s", mVoiceIndex, mUKVoices.get(mVoiceIndex).getName()));
        mTextToSpeech.setVoice(mUKVoices.get(mVoiceIndex));
        mTextToSpeech.setPitch(Configuration.AUDIO_SPEECH_PITCH);
        mTextToSpeech.setSpeechRate(Configuration.AUDIO_SPEECH_RATE);

        // Register the notification sounds used to announce the various
        // types of threats
        String packageName = getPackageName();
        mTextToSpeech.addEarcon("[s1]", packageName, R.raw.s1);
        mTextToSpeech.addEarcon("[s2]", packageName, R.raw.s2);
        mTextToSpeech.addEarcon("[s3]", packageName, R.raw.s3);
        mTextToSpeech.addEarcon("[s4]", packageName, R.raw.s4);
        mTextToSpeech.addEarcon("[s5]", packageName, R.raw.s5);
        mTextToSpeech.addEarcon("[s6]", packageName, R.raw.s6);
        mTextToSpeech.addEarcon("[s7]", packageName, R.raw.s7);
        mTextToSpeech.addEarcon("[s8]", packageName, R.raw.s8);
        mTextToSpeech.addEarcon("[s9]", packageName, R.raw.s9);
        mTextToSpeech.addEarcon("[s10]", packageName, R.raw.s10);

        mReady = true;
        for(Runnable readyCallback : mReadyCallbacks) {
          Log.i(TAG, "postDelayed() readyCallback");
          mHandler.postDelayed(readyCallback, MESSAGE_TOKEN, 1);
        }
      }
    });
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public void isReady(Runnable onDone) {
    if(mReady) {
      Log.i(TAG, "isReady onDone()");
      onDone.run();
    }
    else {
      Log.i(TAG, "mReadyCallbacks.add() readyCallback");
      mReadyCallbacks.add(onDone);
    }
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "onDestroy");
    super.onDestroy();

    mHandler.removeCallbacksAndMessages(MESSAGE_TOKEN);

    // Cleanup text to speech resources and abandon audio focus
    if(mTextToSpeech != null) {
      Log.i(TAG, "mTextToSpeech.shutdown()");
      mTextToSpeech.shutdown();
    }
  }

  public void requestAudioFocus(Runnable onDone) {
    Log.i(TAG, String.format("requestAudioFocus mDuckedAudioMedia %d", mDuckedAudioMedia));

    // Keep track of concurrent in-flight audio focus requests
    // Request audio focus with ducking for speech over a voice communication stream
    mDuckedAudioMedia++;
    if(mDuckedAudioMedia == 1) {
      AudioAttributes playbackAttributes =
        new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
      mAudioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK).setAudioAttributes(playbackAttributes)
        .setAcceptsDelayedFocusGain(false)
        .build();
      Log.i(TAG, "requestAudioFocus()");
      mAudioManager.requestAudioFocus(mAudioFocusRequest);
      onDone.run();
    }
    else {
      onDone.run();
    }
  }

  public void reallyAbandonAudioFocus(Runnable onDone) {
    mDuckedAudioMedia = 0;
    abandonAudioFocus(onDone);
  }

  public void abandonAudioFocus(Runnable onDone) {
    Log.i(TAG, String.format("abandonAudioFocus mDuckedAudioMedia %d", mDuckedAudioMedia));
    // Keep track of concurrent in-flight audio focus requests
    // Abandon audio focus once all audio focus requests are cleared
    if(mDuckedAudioMedia > 0) {
      mDuckedAudioMedia--;
    }
    if(mDuckedAudioMedia == 0 && mAudioFocusRequest != null) {
      Log.i(TAG, "abandonAudioFocus()");
      mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
      onDone.run();
    }
    else {
      onDone.run();
    }
  }

  public void addOnUtteranceProgressCallback(String key, Runnable runnable) {
    mTextToSpeechCallback.put(key, runnable);
  }

  public void removeOnUtteranceProgressCallback(String key) {
    mTextToSpeechCallback.remove(key);
  }

  public void playEarcon(String earcon) {
    // Play a notification sound on the voice call stream
    // The voice call stream is separate from the music stream, the user can
    // adjust the individual volume of each stream separately
    String uuid = UUID.randomUUID().toString();
    Log.i(TAG, String.format("mTextToSpeech.playEarcon() uuid %s earcon %s", uuid, earcon));
    Bundle params = new Bundle();
    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
    mTextToSpeech.playEarcon(earcon, TextToSpeech.QUEUE_ADD, params, uuid);
  }

  public void playSpeech(String speech, String uuid) {
    // Play a speech announcement on the voice call stream
    // The voice call stream is separate from the music stream, the user can
    // adjust the individual volume of each stream separately
    Log.i(TAG, String.format("mTextToSpeech.speak() uuid %s speech %s", uuid, speech));
    Bundle params = new Bundle();
    params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
    mTextToSpeech.speak(speech, TextToSpeech.QUEUE_ADD, params, uuid);
  }

  public void announceEvent(String event, Runnable onDone) {
    Log.i(TAG, "announceEvent");

    // Request audio focus
    Log.i(TAG, "requestAudioFocus()");
    requestAudioFocus(() -> {
      String uuid = UUID.randomUUID().toString();
      addOnUtteranceProgressCallback(uuid, () -> {
        Log.i(TAG, String.format("UtteranceProgressListener.onDone %s", uuid));
        removeOnUtteranceProgressCallback(uuid);

        // Abandon audio focus once done
        Log.i(TAG, "abandonAudioFocus()");
        abandonAudioFocus(() -> {
          Log.i(TAG, "announceEvent.onDone.run()");
          onDone.run();
        });
      });

      // Play a speech announcement on the voice call stream
      // The voice call stream is separate from the music stream, the user
      // can adjust the individual volume of each stream separately
      Log.i(TAG, String.format("mTextToSpeech.speak() uuid %s speech %s", uuid, event));
      Bundle params = new Bundle();
      params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
      mTextToSpeech.speak(event, TextToSpeech.QUEUE_ADD, params, uuid);
    });
  }
}