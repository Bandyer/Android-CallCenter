/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center_2.utils

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.bandyer.demo_communication_center_2.BaseActivity

/**
 * Utility used to remember the logged user, identified by the userAlias
 *
 * @author kristiyan
 */
object LoginManager {

    private const val MY_PREFS_NAME = "myPrefs"

    /**
     * Utility to log a user in the application
     *
     * @param context   App or Activity
     * @param userAlias the user identifier to remember
     */
    fun login(context: Context, userAlias: String) {
        val editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit()
        editor.putString("userAlias", userAlias)
        editor.apply()
    }

    /**
     * Utility to return the logged user
     *
     * @param context Activity or App
     * @return userAlias
     */
    fun getLoggedUser(context: Context): String {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        return prefs.getString("userAlias", "")
    }

    /**
     * Utility to check whenever the use has been logged or not
     *
     * @param context App or Activity
     * @return true if the user is logged, false otherwise
     */
    fun isUserLogged(context: Context): Boolean {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        return prefs.getString("userAlias", "") != ""
    }

    /**
     * Utility to logout the user from the application
     *
     * @param context BaseActivity
     */
    fun logout(context: BaseActivity) {
        val prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}