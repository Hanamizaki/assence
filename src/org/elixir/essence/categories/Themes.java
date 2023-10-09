/*
 * Copyright (C) 2014-2016 The Dirty Unicorns Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elixir.essence.categories;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.ContentResolver;
import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.widget.Toast;
import android.util.DisplayMetrics;
import android.view.Display;
import static android.os.UserHandle.USER_SYSTEM;
import static android.os.UserHandle.USER_CURRENT;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;

import android.graphics.Color;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.util.custom.customUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.io.File;

import com.android.settingslib.display.DisplayDensityConfiguration;

import static android.provider.Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK;
import static android.provider.Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_STYLES;
import static android.provider.Settings.Secure.ELIXIR_EXCLUSIVE_BUILD;
import static android.provider.Settings.Secure.LOCK_SCREEN_CUSTOM_CLOCK_HIDE_DATE;
import static android.provider.Settings.Secure.LOCKSCREEN_DEPTH_CLOCK;
import static android.provider.Settings.Secure.LOCKSCREEN_DEPTH_CLOCK_STYLES;

public class Themes extends SettingsPreferenceFragment 
	implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "Themes";
    private static final String KEY_CUSTOM_CLOCK = "lock_screen_custom_clock";
    private static final String KEY_DEPTH_CLOCK = "lock_screen_depth_clock";
    private static final String KEY_CUSTOM_CLOCK_STYLE = "ls_clock_styles";
    private static final String KEY_DEPTH_CLOCK_STYLE = "ls_depth_clock_styles";
    private static final String KEY_EXCLUSIVE_CAT = "exclusive_clock";
    private String OLD_CLOCK = "DEFAULT";
    private String OLD_DEPTH_CLOCK = "DEFAULT";
    private Context mContext;
    private SwitchPreference mCustomClock;
    private SwitchPreference mDepthClock;
    private ListPreference mClockStyle;
    private ListPreference mDepthClockStyle;
    private ContentResolver resolver;
    private IOverlayManager mOverlayService;
    private PreferenceCategory mExclusiveBuild;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.themes);

        mContext = getActivity();

        mOverlayService = IOverlayManager.Stub
                .asInterface(ServiceManager.getService(Context.OVERLAY_SERVICE));

        resolver = getActivity().getContentResolver();
        final PreferenceScreen screen = getPreferenceScreen();

        final Boolean isExclusive = Settings.Secure.getInt(resolver, ELIXIR_EXCLUSIVE_BUILD, 0) != 0;
        Log.w("Essence", "Status of exclusive :- " + isExclusive.toString());
        if (isExclusive) {
            mCustomClock = (SwitchPreference) findPreference(KEY_CUSTOM_CLOCK);
            if (mCustomClock != null) {
                mCustomClock.setChecked(Settings.Secure.getIntForUser(resolver, LOCK_SCREEN_CUSTOM_CLOCK, 0, UserHandle.USER_CURRENT) != 0);
                mCustomClock.setOnPreferenceChangeListener(this);
            }
    
            mClockStyle = (ListPreference) findPreference(KEY_CUSTOM_CLOCK_STYLE);
            if (mClockStyle != null) {
                mClockStyle.setValue(Settings.Secure.getString(resolver, LOCK_SCREEN_CUSTOM_CLOCK_STYLES));
                OLD_CLOCK = Settings.Secure.getString(resolver, LOCK_SCREEN_CUSTOM_CLOCK_STYLES);
                mClockStyle.setSummary(mClockStyle.getEntry());
                mClockStyle.setOnPreferenceChangeListener(this);
            }

            mDepthClock = (SwitchPreference) findPreference(KEY_DEPTH_CLOCK);
            if (mDepthClock != null) {
                mDepthClock.setChecked(Settings.Secure.getIntForUser(resolver, LOCKSCREEN_DEPTH_CLOCK, 0, UserHandle.USER_CURRENT) != 0);
                mDepthClock.setOnPreferenceChangeListener(this);
            }
    
            mDepthClockStyle = (ListPreference) findPreference(KEY_DEPTH_CLOCK_STYLE);
            if (mDepthClockStyle != null) {
                mDepthClockStyle.setValue(Settings.Secure.getString(resolver, LOCKSCREEN_DEPTH_CLOCK_STYLES));
                OLD_DEPTH_CLOCK = Settings.Secure.getString(resolver, LOCKSCREEN_DEPTH_CLOCK_STYLES);
                mDepthClockStyle.setSummary(mDepthClockStyle.getEntry());
                mDepthClockStyle.setOnPreferenceChangeListener(this);
            }
        } else {
            mExclusiveBuild = (PreferenceCategory) findPreference(KEY_EXCLUSIVE_CAT);
            screen.removePreference(mExclusiveBuild);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.CUSTOM_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mCustomClock) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putInt(resolver, LOCK_SCREEN_CUSTOM_CLOCK, value ? 1 : 0);
            Settings.Secure.putString(resolver, LOCKSCREEN_DEPTH_CLOCK_STYLES, "DEFAULT");
            mDepthClockStyle.setValue("DEFAULT");
            if (isOverlayEnabled(OLD_DEPTH_CLOCK)) {
                RROManager(OLD_DEPTH_CLOCK, false);
            }
            if (!value) {
                hideDateView(true);
            } else {
                Settings.Secure.putInt(resolver, LOCKSCREEN_DEPTH_CLOCK, 0);
                mDepthClock.setChecked(false);
            }
            customUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mClockStyle) {
            mClockStyle.setValue((String) newValue);
            mClockStyle.setSummary(mClockStyle.getEntry());
            Settings.Secure.putString(resolver, LOCK_SCREEN_CUSTOM_CLOCK_STYLES, (String) newValue);
            String current = Settings.Secure.getString(resolver, LOCK_SCREEN_CUSTOM_CLOCK_STYLES);
            if ((Arrays.asList(mDateViewShowClocks).contains(current))) {
                Log.i(TAG, "This clock doent have a date view, enabling smartspace date view");
                hideDateView(true);
            } else {
                hideDateView(false);
            }
            if (isOverlayEnabled(OLD_CLOCK)) {
                RROManager(OLD_CLOCK, false);
            }
            OLD_CLOCK = Settings.Secure.getString(resolver, LOCK_SCREEN_CUSTOM_CLOCK_STYLES);
            if (isOverlayEnabled(current)) {
                // Already enabled ??
            } else {
                RROManager(current, true);
            }
            return true;
        } else if (preference == mDepthClock) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putInt(resolver, LOCKSCREEN_DEPTH_CLOCK, value ? 1 : 0);
            Settings.Secure.putString(resolver, LOCK_SCREEN_CUSTOM_CLOCK_STYLES, "DEFAULT");
            mClockStyle.setValue("DEFAULT");
            if (isOverlayEnabled(OLD_CLOCK)) {
                RROManager(OLD_CLOCK, false);
            }
            if (!value) {
                hideDateView(true);
            } else {
                Settings.Secure.putInt(resolver, LOCK_SCREEN_CUSTOM_CLOCK, 0);
                mCustomClock.setChecked(false);
            }
            customUtils.showSystemUiRestartDialog(getActivity());
            return true;
        } else if (preference == mDepthClockStyle) {
            mDepthClockStyle.setValue((String) newValue);
            mDepthClockStyle.setSummary(mDepthClockStyle.getEntry());
            Settings.Secure.putString(resolver, LOCKSCREEN_DEPTH_CLOCK_STYLES, (String) newValue);
            String current = Settings.Secure.getString(resolver, LOCKSCREEN_DEPTH_CLOCK_STYLES);
            if ((Arrays.asList(mDateViewShowClocks).contains(current))) {
                Log.i(TAG, "This clock doent have a date view, enabling smartspace date view");
                hideDateView(true);
            } else {
                hideDateView(false);
            }
            if (isOverlayEnabled(OLD_DEPTH_CLOCK)) {
                RROManager(OLD_DEPTH_CLOCK, false);
            }
            OLD_DEPTH_CLOCK = Settings.Secure.getString(resolver, LOCKSCREEN_DEPTH_CLOCK_STYLES);
            if ((Arrays.asList(mDepthClocks).contains(current))) {
                Log.i(TAG, "One of depth clock is selected, enabling its wallpaper and setting screen DPI!");
                applyScreenDpi(411);
                Toast.makeText(mContext, "Don't change screen DPI\nand lockscreen wallpaper now!", Toast.LENGTH_LONG).show();
                int position = findPosition(mDepthClocks, current);
                String pathOfWall = "/product/elixir/depth_wallpaper/depth" + position + ".jpg";
                applyWallpaper(pathOfWall);
            } else {
                Log.i(TAG, "None depth clock!");
            }
            if (isOverlayEnabled(current)) {
                // Already enabled ??
            } else {
                RROManager(current, true);
            }
            return true;
        }
        return false;
    }

    private void applyWallpaper(String filePath) {
        File file = new File(filePath);
        Bitmap wallpaperBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
        try {
            wallpaperManager.setBitmap(wallpaperBitmap);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    private void applyScreenDpi(int newDpi) {
        try {
            final Resources res = mContext.getResources();
            final DisplayMetrics metrics = res.getDisplayMetrics();
            final int newSwDp = newDpi;
            final int minDimensionPx = Math.min(metrics.widthPixels, metrics.heightPixels);
            final int newDensity = DisplayMetrics.DENSITY_MEDIUM * minDimensionPx / newSwDp;
            final int densityDpi = Math.max(newDensity, 120);
            DisplayDensityConfiguration.setForcedDisplayDensity(Display.DEFAULT_DISPLAY, densityDpi);
        } catch (Exception e) {
            Log.i(TAG, e.toString());
        }
    }

    private static int findPosition(String[] array, String target) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(target)) {
                return i; // Found a match, return the index
            }
        }
        return -1; // String not found in the array
    }


    private void hideDateView(Boolean show) {
        Settings.Secure.putInt(resolver, LOCK_SCREEN_CUSTOM_CLOCK_HIDE_DATE, show ? 0 : 1);
    }

    private void RROManager(String name, boolean status) {
        if (status) {
            Log.d(TAG, "Enabling Overlay Package :- " + name);
        }
        else {
            Log.d(TAG, "Disabling Overlay Package :- " + name);
        }
        try {
            mOverlayService.setEnabled(name, status, UserHandle.USER_CURRENT);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Overlay " + name + " doesn't exists");
        } catch (Exception re) {
            Log.e(TAG, String.valueOf(re));
        }
    }

    // Clocks that doesnt have a built in date view
    private static final String[] mDateViewShowClocks = {
        "com.android.systemui.lsclock.nothing",
        "com.android.systemui.lsclock.hyperbox",
        "com.android.systemui.lsclock.oosalike"
    };

    // List of depth clocks added in rom
    private static final String[] mDepthClocks = {
        "com.android.systemui.lsclock.depth1",
        "com.android.systemui.lsclock.depth2",
	"com.android.systemui.lsclock.depth3",
	"com.android.systemui.lsclock.depth4",
        "com.android.systemui.lsclock.depth5",
	"com.android.systemui.lsclock.depth6",
	"com.android.systemui.lsclock.depth7",
	"com.android.systemui.lsclock.depth8",
        "com.android.systemui.lsclock.depth9",
	"com.android.systemui.lsclock.depth10",
	"com.android.systemui.lsclock.depth11",
        "com.android.systemui.lsclock.depth12",
	"com.android.systemui.lsclock.depth13",
	"com.android.systemui.lsclock.depth14",
	"com.android.systemui.lsclock.depth15",
        "com.android.systemui.lsclock.depth16",
	"com.android.systemui.lsclock.depth17",
	"com.android.systemui.lsclock.depth18",
        "com.android.systemui.lsclock.depth19", 
        "com.android.systemui.lsclock.depth20",
        "com.android.systemui.lsclock.depth21",
        "com.android.systemui.lsclock.depth22",
        "com.android.systemui.lsclock.depth23",
        "com.android.systemui.lsclock.depth24",
        "com.android.systemui.lsclock.depth25"

    };

    private boolean isOverlayEnabled(String name) {
        OverlayInfo info = null;
        Log.i(TAG, "Getting information of overlay :- " + name);
        try {
            info = mOverlayService.getOverlayInfo(name, UserHandle.USER_CURRENT);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Overlay " + name + " doesn't exists");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info != null && info.isEnabled();
    }
}
