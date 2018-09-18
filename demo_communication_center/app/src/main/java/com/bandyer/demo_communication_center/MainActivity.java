/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.bandyer.communication_center.call.Call;
import com.bandyer.communication_center.call.CallOptions;
import com.bandyer.communication_center.call.CallType;
import com.bandyer.communication_center.call.IncomingCall;
import com.bandyer.communication_center.call.OnCallCreationObserver;
import com.bandyer.communication_center.call_client.CallClient;
import com.bandyer.communication_center.call_client.CallClientStatus;
import com.bandyer.communication_center.call_client.CallException;
import com.bandyer.communication_center.call_client.OnCallClientObserver;
import com.bandyer.communication_center.call_client.OnIncomingCallObserver;
import com.bandyer.communication_center.call_client.User;
import com.bandyer.demo_communication_center.adapter_items.UserSelectionItem;
import com.bandyer.demo_communication_center.utils.LoginManager;
import com.bandyer.demo_communication_center.utils.networking.BandyerUsers;
import com.bandyer.demo_communication_center.utils.networking.MockedNetwork;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;
import com.mikepenz.fastadapter.select.SelectExtension;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * This Activity will be called after the user has logged or if an external url was opened with this app.
 * It's main job is to redirect to the dialing(outgoing) or ringing(ringing) call activities.
 *
 * @author kristiyan
 */
public class MainActivity extends BaseActivity implements OnIncomingCallObserver, OnCallCreationObserver, OnCallClientObserver {

    private FastItemAdapter<UserSelectionItem> fastAdapter;
    private List<String> calleeSelected;

    @BindView(R.id.contactsList)
    RecyclerView listContacts;

    private ProgressDialog dialog;

    private boolean shouldOpenExternalUrl = false;

    // the external url to provide to the call client in case we want to setup a call coming from an url.
    // The url may be provided to join an existing call, or to create a new one.
    private String joinUrl;

    public static void show(Activity context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // We listen for an incoming call.
        CallClient.getInstance().addIncomingCallObserver(this);

        findViewById(R.id.call).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call();
            }
        });

        // if no valid user exists, delete all the preferences and show the LoginActivity
        // else setup the recycler view
        if (!LoginManager.isUserLogged(this)) logout();
        else setUpRecyclerView();

        // get the user that is currently logged in the sample app
        String userAlias = LoginManager.getLoggedUser(this);

        // set a title greeting the logged user
        TextView userGreeting = findViewById(R.id.userGreeting);
        userGreeting.setText(String.format(getResources().getString(R.string.pick_users), userAlias));

        // in case the MainActivity has been shown by opening an external link, handle it
        handleExternalUrl(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // in case the MainActivity has been shown by opening an external link, handle it
        handleExternalUrl(intent);
    }

    @Override
    public void onBackPressed() {
        logout();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        // return true so that the menu pop up is opened
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void logout() {
        LoginManager.logout(this);
        CallClient.getInstance().destroy();
        LoginActivity.show(this);
    }

    private void setUpRecyclerView() {
        calleeSelected = new ArrayList<>();

        fastAdapter = new FastItemAdapter<>();
        fastAdapter.withSelectable(true);

        fastAdapter.clear();

        // Fetch the sample users you can use to login with.
        MockedNetwork.getSampleUsers(this, new Callback<BandyerUsers>() {

            @Override
            public void onResponse(@NonNull retrofit2.Call<BandyerUsers> call, @NonNull Response<BandyerUsers> response) {
                // Add each user(except the logged one) to the recyclerView adapter to be displayed in the list.
                for (String user : response.body().user_id_list)
                    if (!user.equals(LoginManager.getLoggedUser(MainActivity.this)))
                        fastAdapter.add(new UserSelectionItem(user));
            }

            @Override
            public void onFailure(@NonNull retrofit2.Call<BandyerUsers> call, @NonNull Throwable t) {
                // If contacts could not be fetched show error dialog.
                showErrorDialog(t.getMessage());
            }

        });

        // on user selection put in a list to be called on click on call button.
        fastAdapter.withOnPreClickListener(new OnClickListener<UserSelectionItem>() {
            @Override
            public boolean onClick(@Nullable View v, @NonNull IAdapter<UserSelectionItem> adapter, @NonNull UserSelectionItem item, int position) {
                SelectExtension<UserSelectionItem> selectExtension = fastAdapter.getExtension(SelectExtension.class);
                if (selectExtension != null) {
                    selectExtension.toggleSelection(position);
                }

                if (!item.isSelected())
                    calleeSelected.remove(item.name);
                else
                    calleeSelected.add(item.name);
                return true;
            }
        });

        listContacts.setLayoutManager(new LinearLayoutManager(this));
        listContacts.setAdapter(fastAdapter);
    }

    /**
     * This is how an outgoing call is started. You must provide an array of users alias identifying the users your user wants to communicate with.
     * Starting an outgoing call is an asynchronous process, failure or success is reported in the callback provided.
     * <p>
     * WARNING!!!
     * Be aware that all the observers in this SDK, MUST NOT be defined as anonymous class because the call client will have a weak reference to them to avoid leaks and other scenarios.
     * If you do implement the observer anonymously the methods may not be called.
     */
    private void call() {
        CallClient.getInstance().call(calleeSelected, new CallOptions(false, 0, CallType.AUDIO_VIDEO), this);
    }

    /**
     * Handle an external url by calling join method
     * <p>
     * WARNING!!!
     * Be sure to have the call client connected before joining a call with the url provided.
     * Otherwise you will receive an error.
     */
    private void handleExternalUrl(Intent intent) {
        // do not handle the url if we do not have a valid user
        if (!LoginManager.isUserLogged(this))
            return;

        shouldOpenExternalUrl = Intent.ACTION_VIEW.equals(intent.getAction());
        if (shouldOpenExternalUrl) {
            final Uri uri = intent.getData();
            if (uri != null) {
                joinUrl = uri.toString();
                // if client is not running, then I need to initialize it
                if (CallClient.getInstance().getStatus() != CallClientStatus.RUNNING) {
                    dialog = ProgressDialog.show(this, "", getString(R.string.preparing_call), true, false);
                    CallClient.getInstance().addStatusObserver(this);
                    CallClient.getInstance().init(LoginManager.getLoggedUser(this));
                } else
                    CallClient.getInstance().join(uri.toString(), MainActivity.this);

            }
        }
    }

    /////////////////////////////////// CALL INCOMING //////////////////////////////////////////////

    @Override
    public void onIncomingCall(@NonNull IncomingCall call) {
        // We have received an incoming call, let's show an activity to notify the user.
        RingingActivity.show(this);
    }

    //////////////////////////////////// CALL CREATED //////////////////////////////////////////////

    @Override
    public void onCallCreationSuccess(@NonNull Call call) {
        // We have create an outgoing call, let's show an activity to notify the user.
        DialingActivity.show(MainActivity.this);
    }

    @Override
    public void onCallCreationError(@NonNull CallException reason) {
        Log.e("onCallCreationError", "onCallCreationError" + reason.getMessage());
        // If an error has occurred with the creation of the call show error dialog
        showErrorDialog(reason.getMessage());
    }

    /***************************************Call Client Initialized *********************************
     * The following code applies only when coming from an external url, as the login phase was skipped
     * In all other cases, the call client should have been already initialized in the connection phase with our Bandyer platform.
     **********************************************************************************************/
    @Override
    public void onCallClientInitialized(@NonNull User sessionUser) {
        Log.d("CallClient", "init" + sessionUser);
        shouldOpenExternalUrl = false; // reset boolean to avoid reopening external url twice on resume
        CallClient.getInstance().join(joinUrl, MainActivity.this);
        CallClient.getInstance().removeStatusObserver(this); // don't listen for new events
        dialog.dismiss(); // dismiss the progress dialog
    }

    @Override
    public void onCallClientStopped() {
        Log.d("CallClient", "changed stopped");
    }

    @Override
    public void onCallClientResumed() {
        Log.d("CallClient", "changed resumed");
    }

    @Override
    public void onCallClientDestroyed() {
        Log.d("CallClient", "destroyed");
    }

    @Override
    public void onCallClientReconnecting() {
        Log.d("CallClient", "onCallClientReconnecting");
    }

    @Override
    public void onCallClientFailed() {
        Log.e("CallClient", "onCallClientFailed");
    }
}
