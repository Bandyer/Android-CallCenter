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
import com.bandyer.communication_center.call.CallException;
import com.bandyer.communication_center.call.CallType;
import com.bandyer.communication_center.call.CallUpgradeException;
import com.bandyer.communication_center.call.IncomingCall;
import com.bandyer.communication_center.call.OnCallEventObserver;
import com.bandyer.communication_center.call.participant.CallParticipant;
import com.bandyer.communication_center.call.participant.OnCallParticipantObserver;
import com.bandyer.communication_center.call_client.CallClient;
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
 * This Activity is shown after an incoming call has been received.
 * Displays all the participants and their actions.
 * If the call is answered, we can proceed to enter the virtual room where the call will take place.
 * If the call is declined we set a reason and close this activity.
 *
 * @author kristiyan
 */
@RuntimePermissions
public class RingingActivity extends BaseActivity implements OnCallEventObserver {

    public static void show(Activity context) {
        Intent intent = new Intent(context, RingingActivity.class);
        context.startActivity(intent);
    }

    private IncomingCall call;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.caller)
    AppCompatImageView caller;

    private RoomToken roomToken;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringing);
        // Request the current call from the call client, we expect it to be an incoming call
        call = (IncomingCall) CallClient.getInstance().getOngoingCall();
        if (call == null)
            return;
        // This statement is needed to subscribe as a call observer.
        // Once we are subscribed, we will be notified anytime the call state changes
        call.addEventObserver(this);


        // This statement is needed to subscribe as a participant observer.
        // Once we are subscribed, we will be notified anytime a participant status changes
        call.getParticipants().addObserver(new OnCallParticipantObserver() {
            @Override
            public void onCallParticipantStatusChanged(@NonNull CallParticipant participant) {
                Snackbar.make(toolbar.getRootView(), participant.getStatus().name(), Snackbar.LENGTH_LONG).show();
            }

            @Override
            public void onCallParticipantUpgradedCallType(@NonNull CallParticipant participant, @NonNull CallType callType) {
                Log.d("CallActivity", "onCallParticipantUpgradedCallType participant = " + participant.getUser().getUserAlias());
            }
        });

        StringBuilder callers = new StringBuilder(call.getParticipants().getCaller().getUser().getUserAlias());

        for (CallParticipant callee : call.getParticipants().getCallees()) {
            if (!callee.getUser().equals(CallClient.getInstance().getSessionUser()))
                callers.append(", ").append(callee.getUser().getUserAlias());
        }

        toolbar.setTitle(String.format("%s", callers));
        int numberOfUsers = call.getParticipants().getCallees().size();
        toolbar.setSubtitle(getResources().getQuantityString(R.plurals.user_is_calling_you, numberOfUsers));

        // display a beautiful girl in case we are in a call with a single user
        if (numberOfUsers == 1)
            Picasso.get()
                    .load("https://images.pexels.com/photos/733872/pexels-photo-733872.jpeg?auto=compress&cs=tinysrgb&dpr=2&h=750&w=1260")
                    .fit()
                    .centerCrop()
                    .into(caller);
    }

    /**
     * Then we decline the call. The sdk will take care of notifying the other participants about our intent.
     */
    @OnClick(R.id.decline)
    public void decline() {
        if (call != null)
            call.decline(Call.DeclineReason.DO_NOT_DISTURB);
    }

    /**
     * Then we answer the call. The sdk will take care of notifying the other participants about our intent.
     */
    @OnClick(R.id.answer)
    public void answer() {
        if (call != null)
            call.answer();
    }

    /**
     * We can now proceed to enter the virtual room where the call will take place.
     * The roomToken will be used in the other activity to set up the room.
     */
    @Override
    public void onCallStarted(@NonNull Call call, @NonNull RoomToken roomToken) {
        this.roomToken = roomToken;
        // this will call the method showCallActivity if permissions are granted
        RingingActivityPermissionsDispatcher.showCallActivityWithPermissionCheck(RingingActivity.this);
    }

    @Override
    public void onCallUpgraded() {
        Log.d("RingingActivity", "onCallUpgraded");
    }

    @Override
    public void onCallEnded(@NonNull Call call, @NonNull Call.EndReason callEndReason) {
        Log.d("RingingActivity", "onCallEnded " + callEndReason.name());
        onBackPressed();
    }

    @Override
    public void onCallStatusChanged(@NonNull Call call, @NonNull Call.Status status) {
        Log.d("RingingActivity", "onCallStatusChanged " + status.name());
    }

    /**
     * Decline in case of back pressed
     */
    @Override
    public void onBackPressed() {
        if (call != null)
            call.decline(Call.DeclineReason.NONE);
        super.onBackPressed();
    }

    @Override
    public void onCallError(@NonNull Call call, @NonNull CallException reason) {
        Log.e("RingingActivity", "onCallError" + reason.getMessage());
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
        Snackbar.make(toolbar.getRootView(), R.string.permission_camera_denied, LENGTH_SHORT).show();
    }

    @OnNeverAskAgain({Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO})
    void showNeverAskForCamera() {
        Snackbar.make(toolbar.getRootView(), R.string.permission_camera_never_ask_again, LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        RingingActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (call != null)
                call.hangUp(Call.EndReason.HANGUP);
            finish();
        }
    }
}
