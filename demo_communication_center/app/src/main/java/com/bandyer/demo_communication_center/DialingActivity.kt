/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import android.util.Log
import com.bandyer.communication_center.call.*
import com.bandyer.communication_center.call.participant.CallParticipant
import com.bandyer.communication_center.call.participant.OnCallParticipantObserver
import com.bandyer.communication_center.call_client.CallClient
import com.bandyer.core_av.room.RoomToken
import com.bandyer.demo_communication_center.databinding.ActivityDialingBinding
import com.squareup.picasso.Picasso
import permissions.dispatcher.*

/**
 * This Activity is shown after an outgoing call has been created.
 * Displays all the participants that are being called and their actions.
 * When the call is started, we can proceed to enter the virtual room where the call will take place.
 *
 * @author kristiyan
 */
@RuntimePermissions
class DialingActivity : BaseActivity(), OnCallEventObserver {

    private var call: Call? = null
    private var roomToken: RoomToken? = null

    private lateinit var binding: ActivityDialingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDialingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        // Request the current call from the call client
        call = CallClient.getInstance().ongoingCall
        if (call == null)
            return
        //This statement is needed to subscribe as a call observer.
        // Once we are subscribed, we will be notified anytime the call state changes
        call!!.addEventObserver(this)

        //This statement is needed to subscribe as a participant observer.
        // Once we are subscribed, we will be notified anytime a participant status changes
        call!!.participants.addObserver(object : OnCallParticipantObserver {
            override fun onCallParticipantStatusChanged(participant: CallParticipant) {
                Snackbar.make(binding.callee, participant.status.name, Snackbar.LENGTH_LONG).show()
            }

            override fun onCallParticipantUpgradedCallType(participant: CallParticipant, callType: CallType) {
                Log.d("DialingActivity", "onCallUpgraded ${participant.user}")
            }
        })

        val numberOfUsers = call!!.participants.callees!!.size

        binding.toolbar.title = call?.participants?.callees!!.map {
            it.user.userAlias
        }.joinToString()

        binding.toolbar.subtitle = resources.getQuantityString(R.plurals.user_is_ringing, numberOfUsers)

        // display a beautiful girl in case we are in a call with a single user
        if (numberOfUsers == 1)
            Picasso.get()
                    .load("https://images.pexels.com/photos/954559/pexels-photo-954559.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260")
                    .fit()
                    .centerCrop()
                    .into(binding.callee)

        // On Stop button pressed hangup and go back
        binding.stop.setOnClickListener {
            onBackPressed()
        }
    }

    /**
     * Hangup in case of back pressed
     */
    override fun onBackPressed() {
        call?.hangUp(Call.EndReason.HANGUP)
        super.onBackPressed()
    }

    /**
     * We can now proceed to enter the virtual room where the call will take place.
     * The roomToken will be used in the other activity to set up the room.
     */
    override fun onCallStarted(call: Call, roomToken: RoomToken) {
        Log.d("DialingActivity", "onCallStarted")
        this.roomToken = roomToken
        // this will call the method showCallActivity if permissions are granted
        showCallActivityWithPermissionCheck()
    }

    override fun onCallUpgraded() {
        Log.d("DialingActivity", "onCallUpgraded")
    }

    override fun onCallEnded(call: Call, callEndReason: Call.EndReason) {
        Log.d("DialingActivity", "onCallEnded " + callEndReason.name)
        finish()
    }

    override fun onCallStatusChanged(call: Call, status: Call.Status) {
        Log.d("DialingActivity", "onCallStatusChanged " + status.name)
    }

    override fun onCallError(call: Call, reason: CallException) {
        Log.e("DialingActivity", "onCallError $reason")
        // If an error has occurred with the creation of the call show error dialog
        showErrorDialog("${reason.message}")
        if (reason !is CallUpgradeException)
            finish()
    }

    /************************************Permissions Requests **************************************
     * Request Camera and Audio permissions using PermissionDispatcher library
     *
     * For more information about PermissionDispatcher:
     * https://github.com/permissions-dispatcher/PermissionsDispatcher
     **********************************************************************************************/

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    internal fun showCallActivity() {
        CallActivity.show(this, roomToken!!.token)
    }

    @OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    internal fun showRationaleForCamera(request: PermissionRequest) {
        AlertDialog.Builder(this)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_allow) { _, _ -> request.proceed() }
                .setNegativeButton(R.string.button_deny) { _, _ -> request.cancel() }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    internal fun showDeniedForCamera() {
        Snackbar.make(binding.callee, R.string.permission_camera_denied, LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    internal fun showNeverAskForCamera() {
        Snackbar.make(binding.callee, R.string.permission_camera_never_ask_again, LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // NOTE: delegate the permission handling to generated method
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            call?.hangUp(Call.EndReason.HANGUP)
            finish()
        }
    }

    companion object {

        fun show(context: Activity) {
            val intent = Intent(context, DialingActivity::class.java)
            context.startActivity(intent)
        }
    }
}
