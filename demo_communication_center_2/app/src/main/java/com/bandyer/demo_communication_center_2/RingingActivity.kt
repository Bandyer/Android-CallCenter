/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center_2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_SHORT
import android.support.v7.app.AlertDialog
import android.util.Log
import com.bandyer.communication_center.call.Call
import com.bandyer.communication_center.call.IncomingCall
import com.bandyer.communication_center.call.OnCallEventObserver
import com.bandyer.communication_center.call.participant.CallParticipant
import com.bandyer.communication_center.call.participant.OnCallParticipantObserver
import com.bandyer.communication_center.call_client.CallClient
import com.bandyer.communication_center.call_client.CallException
import com.bandyer.core_av.room.RoomToken
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_ringing.*
import permissions.dispatcher.*

/**
 * This Activity is shown after an incoming call has been received.
 * Displays all the participants and their actions.
 * If the call is answered, we can proceed to enter the virtual room where the call will take place.
 * If the call is declined we set a reason and close this activity.
 *
 * @author kristiyan
 */
@RuntimePermissions
class RingingActivity : BaseActivity(), OnCallEventObserver {

    private var call: IncomingCall? = null
    private var roomToken: RoomToken? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ringing)

        // Request the current call from the call client, we expect it to be an incoming call
        // and subscribe as a call observer.
        // Once we are subscribed, we will be notified anytime the call state changes
        call = CallClient.getInstance().ongoingCall!!.addEventObserver(this) as IncomingCall

        // This statement is needed to subscribe as a participant observer.
        // Once we are subscribed, we will be notified anytime a participant status changes
        call?.participants?.addObserver(object : OnCallParticipantObserver {
            override fun onCallParticipantStatusChanged(participant: CallParticipant) {
                Snackbar.make(caller, participant.status.name, Snackbar.LENGTH_LONG).show()
            }
        })

        /**
         * When we answer the call. The sdk will take care of notifying the other participants about our intent.
         */
        answer.setOnClickListener {
            call?.answer()
        }

        /**
         * When we decline the call. The sdk will take care of notifying the other participants about our intent.
         */
        decline.setOnClickListener {
            call?.decline(Call.DeclineReason.DO_NOT_DISTURB)
        }

        val callers = call!!.participants.callees!!.toMutableList()
                .apply { add(call!!.participants.caller) }
                .filter { it.user != CallClient.getInstance().sessionUser }
                .map { it.user.userAlias }.joinToString()

        toolbar.title = callers
        val numberOfUsers = call!!.participants.callees!!.size
        toolbar.subtitle = resources.getQuantityString(R.plurals.user_is_calling_you, numberOfUsers)

        // display a beautiful girl in case we are in a call with a single user
        if (numberOfUsers == 1)
            Picasso.get()
                    .load("https://images.pexels.com/photos/733872/pexels-photo-733872.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260")
                    .fit()
                    .centerCrop()
                    .into(caller)
    }

    /**
     * We can now proceed to enter the virtual room where the call will take place.
     * The roomToken will be used in the other activity to set up the room.
     */
    override fun onCallStarted(call: Call, roomToken: RoomToken) {
        this.roomToken = roomToken
        // this will call the method showCallActivity if permissions are granted
        showCallActivityWithPermissionCheck()
    }

    override fun onCallEnded(call: Call, callEndReason: Call.EndReason) {
        Log.d("RingingActivity", "onCallEnded " + callEndReason.name)
        onBackPressed()
    }

    /**
     * Decline in case of back pressed
     */
    override fun onBackPressed() {
        call?.decline(Call.DeclineReason.NONE)
        super.onBackPressed()
    }

    override fun onCallError(call: Call, reason: CallException) {
        Log.e("RingingActivity", "onCallError " + reason.message)
        onBackPressed()
    }

    override fun onCallStatusChanged(call: Call, status: Call.Status) {
        Log.d("RingingActivity", "onCallStatusChanged " + status.name)
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
                .setPositiveButton(R.string.button_allow) { dialog, which -> request.proceed() }
                .setNegativeButton(R.string.button_deny) { dialog, which -> request.cancel() }
                .show()
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    internal fun showDeniedForCamera() {
        Snackbar.make(caller!!, R.string.permission_camera_denied, LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    internal fun showNeverAskForCamera() {
        Snackbar.make(caller!!, R.string.permission_camera_never_ask_again, LENGTH_SHORT).show()
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
            val intent = Intent(context, RingingActivity::class.java)
            context.startActivity(intent)
        }
    }
}
