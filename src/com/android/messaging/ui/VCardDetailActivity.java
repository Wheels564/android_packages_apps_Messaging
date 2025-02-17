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
package com.android.messaging.ui;

import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import com.android.messaging.R;
import com.android.messaging.util.Assert;

/**
 * An activity that hosts VCardDetailFragment that shows the content of a VCard that contains one
 * or more contacts.
 */
public class VCardDetailActivity extends BugleActionBarActivity
        implements FragmentOnAttachListener {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().addFragmentOnAttachListener(this);
        setContentView(R.layout.vcard_detail_activity);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onAttachFragment(@NonNull FragmentManager fragmentManager,
                                 @NonNull final Fragment fragment) {
        if (fragment instanceof VCardDetailFragment) {
            final Uri vCardUri =
                    getIntent().getParcelableExtra(UIIntents.UI_INTENT_EXTRA_VCARD_URI);
            Assert.notNull(vCardUri);
            final VCardDetailFragment vCardDetailFragment = (VCardDetailFragment) fragment;
            vCardDetailFragment.setVCardUri(vCardUri);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Treat the home press as back press so that when we go back to
            // ConversationActivity, it doesn't lose its original intent (conversation id etc.)
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
