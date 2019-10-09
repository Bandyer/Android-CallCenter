/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center

import android.app.Application
import android.util.Log
import com.bandyer.android_common.logging.BaseLogger
import com.bandyer.android_common.logging.NetworkLogger
import com.bandyer.communication_center.CommunicationCenter
import com.bandyer.communication_center.Environment
import com.bandyer.communication_center.utils.logging.CommCenterLogger
import com.bandyer.demo_communication_center.utils.StethoReporter
import com.bandyer.demo_communication_center.BuildConfig
import com.bandyer.demo_communication_center.R
import com.facebook.stetho.Stetho
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient

/**
 *
 * @author kristiyan
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Debug tools
        initStetho()

        val client = OkHttpClient.Builder().addNetworkInterceptor(StethoInterceptor())

        // The sdk needs to me initialized with a builder pattern.
        // the app_id check values/configuration.xml
        val commBuilder = CommunicationCenter.Builder(this, getString(R.string.app_id))


        //The following statements will set the logging tools, useful when debugging the application.
        // Be aware do not log in PRODUCTION!!!
        if (BuildConfig.DEBUG) {
            commBuilder
                    // sdk networking logger
                    .setNetworkLogger(object : NetworkLogger {

                        // Utility that prints webSocket communication on the Chrome-console

                        private val stethoReporter = StethoReporter()

                        override fun onConnected(tag: String, url: String) {
                            Log.d(tag, "onConnected $url")
                            stethoReporter.onCreated(url)
                        }

                        override fun onMessageReceived(tag: String, response: String) {
                            Log.d(tag, "onMessageReceived $response")
                            stethoReporter.onReceive(response)
                        }

                        override fun onMessageSent(tag: String, request: String) {
                            Log.d(tag, "onMessageSent $request")
                            stethoReporter.onSend(request)
                        }

                        override fun onError(tag: String, reason: String) {
                            Log.e(tag, "connection error $reason")
                            stethoReporter.onError(reason)
                        }

                        override fun onDisconnected(tag: String) {
                            Log.d(tag, "onDisconnected")
                            stethoReporter.onClosed()
                        }
                    })
                    // sdk logic logger
                    .setLogger(object : CommCenterLogger(BaseLogger.VERBOSE) {

                        // You may want to filter the targets depending on which part you are developing and interested in to log.
                        override val target: Int
                            get() = super.target

                        override fun verbose(tag: String, message: String) {
                            Log.v(tag, message)
                        }

                        override fun debug(tag: String, message: String) {
                            Log.d(tag, message)
                        }

                        override fun info(tag: String, message: String) {
                            Log.i(tag, message)
                        }

                        override fun warn(tag: String, message: String) {
                            Log.w(tag, message)
                        }

                        override fun error(tag: String, message: String) {
                            Log.e(tag, message)
                        }
                    })
        }

        commBuilder
                // link your OkHttpClient in case you have it customized
                .setHttpStackBuilder(client)
                //Here we set the sdk environment to sandbox, don't forget to set it to production when ready to release your app.
                .setEnvironment(Environment.sandbox())
                // link your personal Gson in case you wish to personalize some settings
                .setGsonBuilder(GsonBuilder().setPrettyPrinting())

        //This is the statement that will actually initialize the sdk with the configuration provided.
        // Once initialized, you cannot change any setting the sdk is working in.
        // The communicationCenter may also be initialized with default parameters `initWithDefault()`
        CommunicationCenter.init(commBuilder)
    }

    /***************************************Stetho**************************************************
     * Using Stetho to debug networking data in a easy way
     *
     * For more information visit:
     * https://github.com/facebook/stetho
     */

    private fun initStetho() {
        Stetho.initializeWithDefaults(this)
    }
}
