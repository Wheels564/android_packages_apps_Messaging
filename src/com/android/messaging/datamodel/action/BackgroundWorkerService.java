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

package com.android.messaging.datamodel.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

import com.android.messaging.Factory;
import com.android.messaging.datamodel.DataModel;
import com.android.messaging.util.Assert;
import com.android.messaging.util.LogUtil;
import com.android.messaging.util.LoggingTimer;

import java.util.List;

/**
 * Background worker service is an initial example of a background work queue handler
 * Used to actually "send" messages which may take some time and should not block ActionService
 * or UI
 */
public class BackgroundWorkerService extends JobIntentService {
    private static final String TAG = LogUtil.BUGLE_DATAMODEL_TAG;
    private static final boolean VERBOSE = false;

    /**
     * Unique job ID for this service.
     */
    public static final int JOB_ID = 1001;

    private final ActionService mHost;

    public BackgroundWorkerService() {
        super();
        mHost = DataModel.get().getActionService();
    }

    /**
     * Queue a list of requests from action service to this worker
     */
    public static void queueBackgroundWork(final List<Action> actions) {
        for (final Action action : actions) {
            startServiceWithAction(action, 0);
        }
    }

    // ops
    protected static final int OP_PROCESS_REQUEST = 400;

    // extras
    protected static final String EXTRA_OP_CODE = "op";
    protected static final String EXTRA_ACTION = "action";
    protected static final String EXTRA_ATTEMPT = "retry_attempt";

    /**
     * Queue action intent to the BackgroundWorkerService.
     */
    private static void startServiceWithAction(final Action action,
            final int retryCount) {
        final Intent intent = new Intent();
        intent.putExtra(EXTRA_ACTION, action);
        intent.putExtra(EXTRA_ATTEMPT, retryCount);
        startServiceWithIntent(OP_PROCESS_REQUEST, intent);
    }

    /**
     * Queue intent to the BackgroundWorkerService.
     */
    private static void startServiceWithIntent(final int opcode, final Intent intent) {
        final Context context = Factory.get().getApplicationContext();

        intent.setClass(context, BackgroundWorkerService.class);
        intent.putExtra(EXTRA_OP_CODE, opcode);

        enqueueWork(context, intent);
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, BackgroundWorkerService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull final Intent intent) {
        final int opcode = intent.getIntExtra(EXTRA_OP_CODE, 0);

        if (opcode == OP_PROCESS_REQUEST) {
            final Action action = intent.getParcelableExtra(EXTRA_ACTION);
            final int attempt = intent.getIntExtra(EXTRA_ATTEMPT, -1);
            doBackgroundWork(action, attempt);
        } else {
            LogUtil.w(TAG, "Unrecognized opcode in BackgroundWorkerService " + opcode);
            throw new RuntimeException("Unrecognized opcode in BackgroundWorkerService");
        }
    }

    /**
     * Local execution of background work for action on ActionService thread
     */
    private void doBackgroundWork(final Action action, final int attempt) {
        action.markBackgroundWorkStarting();
        Bundle response = null;
        try {
            final LoggingTimer timer = new LoggingTimer(
                    TAG, action.getClass().getSimpleName() + "#doBackgroundWork");
            timer.start();

            response = action.doBackgroundWork();

            timer.stopAndLog();
            action.markBackgroundCompletionQueued();
            mHost.handleResponseFromBackgroundWorker(action, response);
        } catch (final Exception exception) {
            final boolean retry = false;
            LogUtil.e(TAG, "Error in background worker", exception);
            Assert.fail("Unexpected error in background worker - abort");
            action.markBackgroundCompletionQueued();
            mHost.handleFailureFromBackgroundWorker(action, exception);
        }
    }
}
