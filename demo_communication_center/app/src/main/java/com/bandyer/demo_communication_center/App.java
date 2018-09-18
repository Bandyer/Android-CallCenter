/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bandyer.android_common.logging.BaseLogger;
import com.bandyer.android_common.logging.NetworkLogger;
import com.bandyer.communication_center.CommunicationCenter;
import com.bandyer.communication_center.Environment;
import com.bandyer.communication_center.utils.logging.CommCenterLogger;
import com.bandyer.demo_communication_center.utils.StethoReporter;
import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.google.gson.GsonBuilder;
import com.squareup.leakcanary.LeakCanary;

import okhttp3.OkHttpClient;

/**
 * @author kristiyan
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Debug tools
        initLeakCanary();
        initStetho();

        OkHttpClient.Builder client = new OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor());

        // The sdk needs to me initialized with a builder pattern.
        // the app_id check values/configuration.xml
        CommunicationCenter.Builder commBuilder = new CommunicationCenter.Builder(this, getString(R.string.app_id));


        //The following statements will set the logging tools, useful when debugging the application.
        // Be aware do not log in PRODUCTION!!!
        if (BuildConfig.DEBUG) {
            commBuilder
                    // sdk networking logger
                    .setNetworkLogger(new NetworkLogger() {

                        // Utility that prints webSocket communication on the Chrome-console

                        private StethoReporter stethoReporter = new StethoReporter();

                        @Override
                        public void onConnected(@NonNull String tag, @NonNull String url) {
                            Log.d(tag, "onConnected " + url);
                            stethoReporter.onCreated(url);
                        }

                        @Override
                        public void onMessageReceived(@NonNull String tag, @NonNull String response) {
                            Log.d(tag, "onMessageReceived " + response);
                            stethoReporter.onReceive(response);
                        }

                        @Override
                        public void onMessageSent(@NonNull String tag, @NonNull String request) {
                            Log.d(tag, "onMessageSent " + request);
                            stethoReporter.onSend(request);
                        }

                        @Override
                        public void onError(@NonNull String tag, @NonNull String reason) {
                            Log.e(tag, "connection error " + reason);
                            stethoReporter.onError(reason);
                        }

                        @Override
                        public void onDisconnected(@NonNull String tag) {
                            Log.d(tag, "onDisconnected");
                            stethoReporter.onClosed();
                        }
                    })
                    // sdk logic logger
                    .setLogger(new CommCenterLogger(BaseLogger.VERBOSE) {

                        // You may want to filter the targets depending on which part you are developing and interested in to log.
                        @Override
                        public int getTarget() {
                            return super.getTarget();
                        }

                        @Override
                        public void verbose(@NonNull String tag, @NonNull String message) {
                            Log.v(tag, message);
                        }

                        @Override
                        public void debug(@NonNull String tag, @NonNull String message) {
                            Log.d(tag, message);
                        }

                        @Override
                        public void info(@NonNull String tag, @NonNull String message) {
                            Log.i(tag, message);
                        }

                        @Override
                        public void warn(@NonNull String tag, @NonNull String message) {
                            Log.w(tag, message);
                        }

                        @Override
                        public void error(@NonNull String tag, @NonNull String message) {
                            Log.e(tag, message);
                        }
                    });
        }

        commBuilder
                // link your OkHttpClient in case you have it customized
                .setHttpStackBuilder(client)
                //Here we set the sdk environment to sandbox, don't forget to set it to production when ready to release your app.
                .setEnvironment(Environment.Configuration.sandbox())
                // link your personal Gson in case you wish to personalize some settings
                .setGsonBuilder(new GsonBuilder().setPrettyPrinting());

        //This is the statement that will actually initialize the sdk with the configuration provided.
        // Once initialized, you cannot change any setting the sdk is working in.
        // The communicationCenter may also be initialized with default parameters `initWithDefault()`
        CommunicationCenter.init(commBuilder);
    }


    /***************************************LeackCanary*********************************************
     * Using LeakCanary library to debug potential leaks.
     * Leaks may lead to your application consuming & retaining memory inefficiently, making the device and the application slower and crash prone
     * For more information visit:
     * https://github.com/square/leakcanary
     **********************************************************************************************/

    private void initLeakCanary() {
        if (LeakCanary.isInAnalyzerProcess(this))
            return;
        LeakCanary.install(this);
    }


    /***************************************Stetho**************************************************
     * Using Stetho to debug networking data in a easy way
     *
     * For more information visit:
     * https://github.com/facebook/stetho
     **********************************************************************************************/

    private void initStetho() {
        Stetho.initializeWithDefaults(this);
    }

}
