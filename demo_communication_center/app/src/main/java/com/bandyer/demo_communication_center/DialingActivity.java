/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.bandyer.communication_center.call.Call;
import com.bandyer.communication_center.call.OnCallEventObserver;
import com.bandyer.communication_center.call.participant.CallParticipant;
import com.bandyer.communication_center.call.participant.OnCallParticipantObserver;
import com.bandyer.communication_center.call_client.CallClient;
import com.bandyer.communication_center.call_client.CallException;
import com.bandyer.communication_center.call_client.CallUpgradeException;
import com.bandyer.communication_center.call.CallType;
import com.bandyer.core_av.room.RoomToken;
import com.squareup.picasso.Picasso;

import butterknife.BindView;
import butterknife.OnClick;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.support.design.widget.Snackbar.LENGTH_SHORT;

/**
 * This Activity is shown after an outgoing call has been created.
 * Displays all the participants that are being called and their actions.
 * When the call is started, we can proceed to enter the virtual room where the call will take place.
 *
 * @author kristiyan
 */
@RuntimePermissions
public class DialingActivity extends BaseActivity implements OnCallEventObserver {

    public static void show(Activity context) {
        Intent intent = new Intent(context, DialingActivity.class);
        context.startActivity(intent);
    }

    private Call call;

    @BindView(R.id.callee)
    AppCompatImageView callee;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    private RoomToken roomToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialing);
        // Request the current call from the call client
        call = CallClient.getInstance().getOngoingCall();
        if (call == null)
            return;

        //This statement is needed to subscribe as a call observer.
        // Once we are subscribed, we will be notified anytime the call state changes
        call.addEventObserver(this);

        //This statement is needed to subscribe as a participant observer.
        // Once we are subscribed, we will be notified anytime a participant status changes
        call.getParticipants().addObserver(new OnCallParticipantObserver() {
            @Override
            public void onCallParticipantStatusChanged(@NonNull CallParticipant participant) {
                Snackbar.make(callee, participant.getStatus().name(), Snackbar.LENGTH_LONG).show();
            }
        });

        StringBuilder callees = new StringBuilder();

        for (CallParticipant callee : call.getParticipants().getCallees())
            callees.append(callee.getUser().getUserAlias()).append(",");
        callees.deleteCharAt(callees.length() - 1);

        int numberOfUsers = call.getParticipants().getCallees().size();

        toolbar.setTitle(String.format("%s", callees));
        toolbar.setSubtitle(getResources().getQuantityString(R.plurals.user_is_ringing, numberOfUsers));

        // display a beautiful girl in case we are in a call with a single user
        if (numberOfUsers == 1)
            Picasso.get()
                    .load("https://images.pexels.com/photos/954559/pexels-photo-954559.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260")
                    .fit()
                    .centerCrop()
                    .into(callee);
    }

    /**
     * We can now proceed to enter the virtual room where the call will take place.
     * The roomToken will be used in the other activity to set up the room.
     */
    @Override
    public void onCallStarted(@NonNull Call call, @NonNull RoomToken roomToken) {
        Log.d("DialingActivity", "onCallStarted");
        this.roomToken = roomToken;
        // this will call the method showCallActivity if permissions are granted
        DialingActivityPermissionsDispatcher.showCallActivityWithPermissionCheck(DialingActivity.this);
    }

    @Override
    public void onCallUpgraded(@NonNull CallParticipant callParticipant, @NonNull CallType callType) {
        Log.d("DialingActivity", "onCallUpgraded " + callType.name());
    }

    @Override
    public void onCallEnded(@NonNull Call call, @NonNull Call.EndReason callEndReason) {
        Log.d("DialingActivity", "onCallEnded " + callEndReason.name());
        onBackPressed();
    }

    @Override
    public void onCallStatusChanged(@NonNull Call call, @NonNull Call.Status status) {
        Log.d("DialingActivity", "onCallStatusChanged " + status.name());
    }

    // On Stop button pressed hangup and go back
    @OnClick(R.id.stop)
    public void onStopDial() {
        onBackPressed();
    }

    /**
     * Hangup in case of back pressed
     */
    @Override
    public void onBackPressed() {
        call.hangUp(Call.EndReason.HANGUP);
        super.onBackPressed();
    }

    @Override
    public void onCallError(@NonNull Call call, @NonNull CallException reason) {
        Log.e("DialingActivity", "onCallError" + reason);
        // If an error has occurred with the creation of the call show error dialog
        showErrorDialog(reason.getMessage());
        if (!(reason instanceof CallUpgradeException))
            onBackPressed();
    }

    /************************************Permissions Requests **************************************
     * Request Camera and Audio permissions using PermissionDispatcher library
     *
     * For more information about PermissionDispatcher:
     * https://github.com/permissions-dispatcher/PermissionsDispatcher
     **********************************************************************************************/

    @NeedsPermission({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showCallActivity() {
        CallActivity.show(this, roomToken.getToken());
    }

    @OnShowRationale({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showDeniedForCamera() {
        Snackbar.make(callee, R.string.permission_camera_denied, LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showNeverAskForCamera() {
        Snackbar.make(callee, R.string.permission_camera_never_ask_again, LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        DialingActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (call == null)
                return;
            call.hangUp(Call.EndReason.HANGUP);
            finish();
        }
    }
}
