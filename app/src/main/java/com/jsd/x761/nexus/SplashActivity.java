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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jsd.x761.nexus.Nexus.R;

/**
 * An activity that displays the app splash screen and checks for the
 * required app permissions.
 */
public class SplashActivity extends AppCompatActivity {
  private static final String TAG = "SPLASH_ACTIVITY";
  public static final String MESSAGE_TOKEN = "SPLASH_ACTIVITY_MESSAGES";

  private final int FINE_LOCATION_REQUEST = 1;
  private final int BLUETOOTH_SCAN_REQUEST = 2;
  private final int BLUETOOTH_ENABLE_REQUEST = 3;
  private final int BLUETOOTH_CONNECT_REQUEST = 4;
  private final int BLUETOOTH_REQUEST = 5;

  private final Handler mHandler = new Handler(Looper.getMainLooper());

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(TAG, "onCreate");
    super.onCreate(savedInstanceState);

    setContentView(R.layout.splash_activity);

    this.getSupportActionBar().hide();

    // Check for Bluetooth permissions
    checkBluetoothPermission();
  }

  @Override
  protected void onDestroy() {
    Log.i(TAG, "onDestroy");
    super.onDestroy();

    mHandler.removeCallbacksAndMessages(MESSAGE_TOKEN);
  }

  protected void checkFineLocationPermission() {
    Log.i(TAG, "checkFineLocationPermission");
    if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
      this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      runOnUiThread(() -> {
        AlertDialog.Builder b = new AlertDialog.Builder(SplashActivity.this);
        b.setMessage("Please allow Nexus to access this device's precise location all the time");
        b.setTitle("Location Access");
        b.setPositiveButton(android.R.string.ok, null);
        b.setOnDismissListener(dialog -> {
          Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
          startActivityForResult(intent, FINE_LOCATION_REQUEST);
        });
        b.show();
      });
    }
    else {
      // Check for Bluetooth scanning permission
      checkBluetoothScanPermission();
    }
  }

  protected void checkBluetoothScanPermission() {
    Log.i(TAG, "checkBluetoothScanPermission");
    if(this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
      runOnUiThread(() -> {
        AlertDialog.Builder b = new AlertDialog.Builder(SplashActivity.this);
        b.setMessage("Please allow Nexus to find and connect to nearby devices");
        b.setTitle("Nearby Device Access");
        b.setPositiveButton(android.R.string.ok, null);
        b.setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, BLUETOOTH_SCAN_REQUEST));
        b.show();
      });
    }
    else {
      // Check for Bluetooth connect permission
      checkBluetoothConnectPermission();
    }
  }

  protected void checkBluetoothPermission() {
    Log.i(TAG, "checkBluetoothPermission");
    // Check for location access permission
    checkFineLocationPermission();
  }

  protected void checkBluetoothConnectPermission() {
    Log.i(TAG, "checkBluetoothConnectPermission");
    if(this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
      runOnUiThread(() -> {
        AlertDialog.Builder b = new AlertDialog.Builder(SplashActivity.this);
        b.setMessage("Please allow Nexus to connect to Bluetooth devices");
        b.setTitle("Bluetooth Connect Access");
        b.setPositiveButton(android.R.string.ok, null);
        b.setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BLUETOOTH_CONNECT_REQUEST));
        b.show();
      });
    }
    else {
      // Turn on Bluetooth
      enableBluetooth();
    }
  }

  @SuppressLint("MissingPermission")
  protected void enableBluetooth() {
    Log.i(TAG, "enableBluetooth");
    // Get the Bluetooth manager and adapter
    BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
    if(bluetoothManager == null) {
      Log.i(TAG, "Failed to get Bluetooth manager");
      runOnUiThread(() -> Toast.makeText(SplashActivity.this, "Failed to get Bluetooth manager", Toast.LENGTH_SHORT).show());
      finish();
      return;
    }

    BluetoothAdapter bluetoothAdapter;
    bluetoothAdapter = bluetoothManager.getAdapter();

    // Enable Bluetooth
    if(!bluetoothAdapter.isEnabled()) {
      startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 3);
    }
    else {
      // Start the DS1 device scan activity
      startMainMenuActivity();
    }
  }

  @Override
  protected void onActivityResult(
    int requestCode, int resultCode, Intent intent) {
    Log.i(TAG, "onActivityResult");
    super.onActivityResult(requestCode, resultCode, intent);

    if(requestCode == BLUETOOTH_ENABLE_REQUEST) {
      if(resultCode == RESULT_OK) {
        // Start the main menu activity
        startMainMenuActivity();
      }
      else {
        finish();
      }
    }
    else if(requestCode == FINE_LOCATION_REQUEST) {
      requestPermissions(new String[]{
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, FINE_LOCATION_REQUEST);
    }
  }

  private void startMainMenuActivity() {
    Log.i(TAG, "startMainMenuActivity");
    Runnable startTask = () -> {
      Intent intentStart = new Intent(SplashActivity.this, MainMenuActivity.class);
      startActivity(intentStart);
      finish();
    };

    // Wait a few secs before starting the DS1 device scan activity
    mHandler.postDelayed(startTask, MESSAGE_TOKEN, Configuration.SPLASH_TIMER);
  }

  @Override
  public void onRequestPermissionsResult(
    int requestCode, String[] permissions, int[] grantResults) {
    Log.i(TAG, "onRequestPermissionsResult");
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    // Handle the results of the various permission requests
    if(requestCode == BLUETOOTH_REQUEST) {
      if(grantResults.length > 0) {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          checkFineLocationPermission();
          return;
        }
      }
      Log.i(TAG, "Permission to use Bluetooth was denied");
      runOnUiThread(() -> Toast.makeText(SplashActivity.this, "Permission to use Bluetooth was denied", Toast.LENGTH_SHORT).show());
      finish();

    }
    else if(requestCode == FINE_LOCATION_REQUEST) {
      if(grantResults.length > 0) {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          checkBluetoothScanPermission();
          return;
        }
      }
      Log.i(TAG, "Permission to access this device's precise location was denied");
      runOnUiThread(() -> Toast.makeText(SplashActivity.this, "Permission to access this device's precise location was denied", Toast.LENGTH_SHORT).show());
      finish();

    }
    else if(requestCode == BLUETOOTH_SCAN_REQUEST) {
      if(grantResults.length > 0) {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          checkBluetoothConnectPermission();
          return;
        }
      }
      Log.i(TAG, "Permission to find and connect to nearby devices was denied");
      runOnUiThread(() -> Toast.makeText(SplashActivity.this, "Permission to find and connect to nearby devices was denied", Toast.LENGTH_SHORT).show());
      finish();

    }
    else if(requestCode == BLUETOOTH_CONNECT_REQUEST) {
      if(grantResults.length > 0) {
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          enableBluetooth();
          return;
        }
      }
      Log.i(TAG, "Permission to connect to Bluetooth devices was denied");
      runOnUiThread(() -> Toast.makeText(SplashActivity.this, "Permission to connect to Bluetooth devices was denied", Toast.LENGTH_SHORT).show());
      finish();
    }
  }
}
