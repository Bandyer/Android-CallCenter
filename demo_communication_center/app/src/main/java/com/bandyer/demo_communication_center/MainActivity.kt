/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.bandyer.communication_center.call.*
import com.bandyer.communication_center.call_client.*
import com.bandyer.demo_communication_center.adapter_items.UserSelectionItem
import com.bandyer.demo_communication_center.utils.LoginManager
import com.bandyer.demo_communication_center.utils.networking.BandyerUsers
import com.bandyer.demo_communication_center.utils.networking.MockedNetwork
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter.select.SelectExtension
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Callback
import retrofit2.Response
import java.util.*

/**
 * This Activity will be called after the user has logged or if an external url was opened with this app.
 * It's main job is to redirect to the dialing(outgoing) or ringing(ringing) call activities.
 *
 * @author kristiyan
 */
class MainActivity : BaseActivity(), OnIncomingCallObserver, OnCallCreationObserver, OnCallClientObserver {

    private var fastAdapter: FastItemAdapter<UserSelectionItem>? = null
    private var calleeSelected: MutableList<String>? = null
    private var dialog: ProgressDialog? = null

    private var shouldOpenExternalUrl = false

    // the external url to provide to the call client in case we want to setup a call coming from an url.
    // The url may be provided to join an existing call, or to create a new one.
    private var joinUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // We listen for an incoming call.
        CallClient.getInstance().addIncomingCallObserver(this)
        findViewById<View>(R.id.call).setOnClickListener { call() }


        // if no valid user exists, delete all the preferences and show the LoginActivity
        // else setup the recycler view
        if (!LoginManager.isUserLogged(this)) logout()
        else setUpRecyclerView()

        // get the user that is currently logged in the sample app
        val userAlias = LoginManager.getLoggedUser(this)
        userGreeting.text = String.format(resources.getString(R.string.pick_users), userAlias)

        // in case the MainActivity has been shown by opening an external link, handle it
        handleExternalUrl(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // in case the MainActivity has been shown by opening an external link, handle it
        handleExternalUrl(intent)
    }

    override fun onBackPressed() {
        logout()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        // return true so that the menu pop up is opened
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> logout()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logout() {
        LoginManager.logout(this)
        CallClient.getInstance().destroy()
        LoginActivity.show(this)
    }

    private fun setUpRecyclerView() {
        calleeSelected = ArrayList()

        fastAdapter = FastItemAdapter()
        fastAdapter?.withSelectable(true)

        fastAdapter?.clear()

        // Fetch the sample users you can use to login with.
        MockedNetwork.getSampleUsers(this, object : Callback<BandyerUsers> {

            override fun onResponse(call: retrofit2.Call<BandyerUsers>, response: Response<BandyerUsers>) {

                // Add each user(except the logged one) to the recyclerView adapter to be displayed in the list.
                for (user in response.body()!!.user_id_list!!)
                    if (user != LoginManager.getLoggedUser(this@MainActivity))
                        fastAdapter?.add(UserSelectionItem(user))
            }

            override fun onFailure(call: retrofit2.Call<BandyerUsers>, t: Throwable) {
                // If contacts could not be fetched show error dialog.
                showErrorDialog("${t.message}")
            }

        })

        // on user selection put in a list to be called on click on call button.
        fastAdapter?.withOnPreClickListener { v, adapter, item, position ->
            val selectExtension = fastAdapter?.getExtension<SelectExtension<UserSelectionItem>>(SelectExtension::class.java)
            selectExtension?.toggleSelection(position)

            if (!item.isSelected)
                calleeSelected?.remove(item.name)
            else
                calleeSelected?.add(item.name)
            true
        }

        contactsList.layoutManager = LinearLayoutManager(this)
        contactsList.adapter = fastAdapter
    }

    /**
     * This is how an outgoing call is started. You must provide an array of users alias identifying the users your user wants to communicate with.
     * Starting an outgoing call is an asynchronous process, failure or success is reported in the callback provided.
     * <p>
     * WARNING!!!
     * Be aware that all the observers in this SDK, MUST NOT be defined as anonymous class because the call client will have a weak reference to them to avoid leaks and other scenarios.
     * If you do implement the observer anonymously the methods may not be called.
     */
    fun call() {
        Log.d("MainActivity", "Call!!!!!!")
        CallClient.getInstance().call(calleeSelected!!, CallOptions(false, 0, CallType.AUDIO_VIDEO), this)
    }

    /**
     * Handle an external url by calling join method
     * <p>
     * WARNING!!!
     * Be sure to have the call client connected before joining a call with the url provided.
     * Otherwise you will receive an error.
     */
    private fun handleExternalUrl(intent: Intent) {
        // do not handle the url if we do not have a valid user
        if (!LoginManager.isUserLogged(this))
            return

        shouldOpenExternalUrl = Intent.ACTION_VIEW == intent.action
        if (shouldOpenExternalUrl) {
            val uri = intent.data
            if (uri != null) {
                joinUrl = uri.toString()
                // if client is not running, then I need to initialize it
                if (CallClient.getInstance().status !== CallClientStatus.RUNNING) {
                    dialog = ProgressDialog.show(this, "", getString(R.string.preparing_call), true, false)
                    CallClient.getInstance().addStatusObserver(this)
                    CallClient.getInstance().init(LoginManager.getLoggedUser(this))
                } else
                    CallClient.getInstance().join(uri.toString(), this@MainActivity)
            }
        }
    }

    /////////////////////////////////// CALL INCOMING //////////////////////////////////////////////

    override fun onIncomingCall(call: IncomingCall) {
        // We have received an incoming call, let's show an activity to notify the user.
        RingingActivity.show(this)
    }

    //////////////////////////////////// CALL CREATED //////////////////////////////////////////////

    override fun onCallCreationSuccess(call: Call) {
        // We have create an outgoing call, let's show an activity to notify the user.
        DialingActivity.show(this@MainActivity)
    }

    override fun onCallCreationError(reason: CallCreationException) {
        Log.e("MainActivity", "onCallCreationError" + reason.message)
        // If contacts could not be fetched show error dialog.
        showErrorDialog("${reason.message}")
    }

    /***************************************Call Client Initialized *********************************
     * The following code applies only when coming from an external url, as the login phase was skipped
     * In all other cases, the call client should have been already initialized in the connection phase with our Bandyer platform.
     **********************************************************************************************/
    override fun onCallClientInitialized(sessionUser: User) {
        Log.d("CallClient", "init$sessionUser")
        shouldOpenExternalUrl = false // reset boolean to avoid reopening external url twice on resume
        CallClient.getInstance().join(joinUrl!!, this@MainActivity)
        CallClient.getInstance().removeStatusObserver(this) // don't listen for new events
        dialog?.dismiss() // dismiss the progress dialog
    }

    override fun onCallClientStopped() {
        Log.d("CallClient", "changed stopped")
    }

    override fun onCallClientResumed() {
        Log.d("CallClient", "changed resumed")
    }

    override fun onCallClientDestroyed() {
        Log.d("CallClient", "destroyed")
    }

    override fun onCallClientReconnecting() {
        Log.d("CallClient", "reconnecting")
    }

    override fun onCallClientFailed(reason: CallClientException) {
        Log.e("CallClient", "failed" + reason.message)
    }

    companion object {

        fun show(context: Activity) {
            val intent = Intent(context, MainActivity::class.java)
            context.startActivity(intent)
        }
    }

}
