/*
 * Copyright (C) 2015 The Android Open Source Project
 * Copyright (C) 2024 The LineageOS Project
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

package com.android.messaging.ui.appsettings;

import static com.android.messaging.util.ChangeDefaultSmsAppHelper.REQUEST_SET_DEFAULT_SMS_APP;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NavUtils;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.messaging.R;
import com.android.messaging.ui.BugleActionBarActivity;
import com.android.messaging.ui.LicenseActivity;
import com.android.messaging.ui.UIIntents;
import com.android.messaging.util.BuglePrefs;
import com.android.messaging.util.ChangeDefaultSmsAppHelper;
import com.android.messaging.util.PhoneUtils;

public class ApplicationSettingsActivity extends BugleActionBarActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final boolean topLevel = getIntent().getBooleanExtra(
                UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS, false);
        if (topLevel) {
            getSupportActionBar().setTitle(getString(R.string.settings_activity_title));
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(android.R.id.content, new ApplicationSettingsFragment());
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (super.onCreateOptionsMenu(menu)) {
            return true;
        }
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        } else if (itemId == R.id.action_license) {
            final Intent intent = new Intent(this, LicenseActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class ApplicationSettingsFragment extends PreferenceFragmentCompat {

        private String mNotificationsPreferenceKey;
        private String mSmsEnabledPrefKey;
        private Preference mSmsEnabledPreference;
        private ChangeDefaultSmsAppHelper mChangeDefaultSmsAppHelper;
        private final ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        if (mChangeDefaultSmsAppHelper == null) {
                            mChangeDefaultSmsAppHelper = new ChangeDefaultSmsAppHelper();
                        }
                        mChangeDefaultSmsAppHelper.handleChangeDefaultSmsResult(
                                REQUEST_SET_DEFAULT_SMS_APP, result.getResultCode(), null);
                    }
                });

        public ApplicationSettingsFragment() {
            // Required empty constructor
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState,
                                        @Nullable String rootKey) {
            getPreferenceManager().setSharedPreferencesName(BuglePrefs.SHARED_PREFERENCES_NAME);
            addPreferencesFromResource(R.xml.preferences_application);

            mNotificationsPreferenceKey =
                    getString(R.string.notifications_pref_key);
            mSmsEnabledPrefKey = getString(R.string.sms_enabled_pref_key);
            mSmsEnabledPreference = findPreference(mSmsEnabledPrefKey);

            final PreferenceScreen advancedScreen = (PreferenceScreen) findPreference(
                    getString(R.string.advanced_pref_key));
            final boolean topLevel = getActivity().getIntent().getBooleanExtra(
                    UIIntents.UI_INTENT_EXTRA_TOP_LEVEL_SETTINGS, false);
            if (topLevel) {
                advancedScreen.setIntent(UIIntents.get()
                        .getAdvancedSettingsIntent(getPreferenceScreen().getContext()));
            } else {
                // Hide the Advanced settings screen if this is not top-level; these are shown at
                // the parent SettingsActivity.
                getPreferenceScreen().removePreference(advancedScreen);
            }
        }

        @Override
        public boolean onPreferenceTreeClick(@NonNull Preference preference) {
            if (preference.getKey() == mNotificationsPreferenceKey) {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getContext().getPackageName());
                startActivity(intent);
            }
            return super.onPreferenceTreeClick(preference);
        }

        private void updateSmsEnabledPreferences() {
            String defaultSmsAppLabel = getString(R.string.default_sms_app,
                    PhoneUtils.getDefault().getDefaultSmsAppLabel());
            if (!PhoneUtils.getDefault().isDefaultSmsApp()) {
                mSmsEnabledPreference.setOnPreferenceClickListener(preference -> {
                    final Intent intent =
                            UIIntents.get().getChangeDefaultSmsAppIntent(getActivity());
                    mLauncher.launch(intent);
                    return true;
                });
            } else {
                mSmsEnabledPreference.setOnPreferenceClickListener(null);
                // Fallback since we don't always get our own name
                if (TextUtils.isEmpty(defaultSmsAppLabel)) {
                    defaultSmsAppLabel = getString(R.string.app_name);
                }
            }
            mSmsEnabledPreference.setSummary(defaultSmsAppLabel);
        }

        @Override
        public void onResume() {
            super.onResume();
            updateSmsEnabledPreferences();
        }
    }
}
