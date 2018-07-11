/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center_2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.View
import com.bandyer.communication_center.call_client.CallClient
import com.bandyer.communication_center.call_client.OnCallClientObserver
import com.bandyer.communication_center.call_client.User
import com.bandyer.demo_communication_center_2.adapter_items.UserItem
import com.bandyer.demo_communication_center_2.utils.LoginManager
import com.bandyer.demo_communication_center_2.utils.networking.BandyerUsers
import com.bandyer.demo_communication_center_2.utils.networking.MockedNetwork
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.mikepenz.fastadapter.listeners.OnClickListener
import kotlinx.android.synthetic.main.activity_login.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * This activity will allow you to choose a user from your company to use to interact with other users.
 * <p>
 * The list of users you can choose from will be displayed using the FastAdapter library to populate the  RecyclerView
 * <p>
 * For more information about how it works FastAdapter:
 * https://github.com/mikepenz/FastAdapter
 */
class LoginActivity : BaseActivity(), OnClickListener<UserItem>, OnCallClientObserver {

    // the userAlias is the identifier of the created user via Bandyer-server restCall see https://docs.bandyer.com/Bandyer-RESTAPI/#create-user
    private var userAlias = ""
    private val itemAdapter = ItemAdapter<UserItem>()
    private val fastAdapter = FastAdapter.with<UserItem, ItemAdapter<UserItem>>(itemAdapter)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // set the recyclerView
        list_users.adapter = fastAdapter
        list_users.layoutManager = LinearLayoutManager(this)

        fastAdapter.withOnClickListener(this)

        //Once you have authenticated your user, you are ready to initialize the call client instance.
        //The call client instance is responsible for making outgoing calls and detecting incoming calls.
        //In order to do its job it must connect to Bandyer platform.

        //This statement is needed to register the current activity as an observer of the call client.
        //When the client has started successfully or it has stopped, it will notify its observers about its state changes.

        // Be aware that all the observers in this SDK, MUST NOT be defined as anonymous class because the call client will have a weak reference to them to avoid leaks and other scenarios.
        // If you do implement the observer anonymously the methods may not be called.
        CallClient.getInstance().addStatusObserver(this)
    }

    override fun onResume() {
        super.onResume()

        // the userAlias is the identifier of the created user via Bandyer-server restCall see https://docs.bandyer.com/Bandyer-RESTAPI/#create-user
        userAlias = LoginManager.getLoggedUser(this)

        // If the user is already logged init the call client and do not fetch the sample users again.
        if (LoginManager.isUserLogged(this)) {
            //This statement is needed to initialize the call client, establishing a secure connection with Bandyer platform.
            CallClient.getInstance().init(userAlias)
            return
        }

        itemAdapter.clear()

        // Fetch the sample users you can use to login with.
        MockedNetwork.getSampleUsers(this, object : Callback<BandyerUsers> {

            override fun onResponse(call: Call<BandyerUsers>, response: Response<BandyerUsers>) {
                if (response.body() == null || response.body()?.user_id_list == null) {
                    showErrorDialog("Please check if you have provided the correct keys in the configuration.xml")
                    return
                }

                // Add each user to the recyclerView adapter to be displayed in the list.
                for (user in response.body()?.user_id_list!!)
                    itemAdapter.add(UserItem(user))
            }

            override fun onFailure(call: Call<BandyerUsers>, t: Throwable) {
                // If contacts could not be fetched show error dialog
                showErrorDialog("${t.message}")
            }

        })
    }

    /**
     * On click on a user from the list initialize the call client for that user
     * save the userAlias to be used for login after the call client has been initialized
     */
    override fun onClick(v: View?, adapter: IAdapter<UserItem>, item: UserItem, position: Int): Boolean {
        userAlias = item.userAlias
        //This statement is needed to initialize the call client, establishing a secure connection with Bandyer platform.
        CallClient.getInstance().init(userAlias)
        return false
    }

    /**
     * Once the call client has established a secure connection with Bandyer platform and it has been authenticated by the back-end system
     * you are ready to make calls and receive incoming calls
     */
    override fun onCallClientInitialized(sessionUser: User) {
        Log.d("CallClient", "init $sessionUser")

        if (!LoginManager.isUserLogged(this))
            LoginManager.login(this, sessionUser.userAlias!!)

        MainActivity.show(this@LoginActivity)
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

    override fun onCallClientFailed() {
        Log.d("CallClient", "failed")
    }

    companion object {

        fun show(context: Activity) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }

}
