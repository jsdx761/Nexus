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

import android.util.Log;

/**
 * An activity that displays the app main menu.
 */
public class MainMenuActivity extends MenuActivity {
  private static final String TAG = "MAIN_MENU_ACTIVITY";

  @Override
  protected void addMenuItems() {
    Log.i(TAG, "addMenuItems");

    /*
    mArrayMenu.add("  Menu");
    mIntentMenu.add(new ActivityConfiguration(null));
    mHeaderList.add(mArrayMenu.size() - 1);
    */

    mArrayMenu.add("      Threats");
    mIntentMenu.add(new ActivityConfiguration(ThreatsActivity.class));

    mArrayMenu.add("      Sources");
    mIntentMenu.add(new ActivityConfiguration(null));

    mArrayMenu.add("        DS1 Radar");
    mIntentMenu.add(new ActivityConfiguration(DS1ScanActivity.class));

    mArrayMenu.add("        Reports");
    mIntentMenu.add(new ActivityConfiguration(ReportsPrefActivity.class));

    mArrayMenu.add("        Aircrafts");
    mIntentMenu.add(new ActivityConfiguration(AircraftsPrefActivity.class));

    mArrayMenu.add("      DS1 Volume");
    mIntentMenu.add(new ActivityConfiguration(DS1VolumeActivity.class));
  }
}
