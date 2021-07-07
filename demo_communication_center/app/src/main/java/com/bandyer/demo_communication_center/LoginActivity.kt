/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.bandyer.communication_center.call_client.CallClient
import com.bandyer.communication_center.call_client.CallClientException
import com.bandyer.communication_center.call_client.OnCallClientObserver
import com.bandyer.communication_center.call_client.User
import com.bandyer.demo_communication_center.adapter_items.UserItem
import com.bandyer.demo_communication_center.databinding.ActivityLoginBinding
import com.bandyer.demo_communication_center.utils.LoginManager
import com.bandyer.demo_communication_center.utils.networking.BandyerUsers
import com.bandyer.demo_communication_center.utils.networking.MockedNetwork
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
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
class LoginActivity : BaseActivity(), OnCallClientObserver {

    // the userAlias is the identifier of the created user via Bandyer-server restCall see https://docs.bandyer.com/Bandyer-RESTAPI/#create-user
    private var userAlias = ""
    private val itemAdapter = ItemAdapter<UserItem>()
    private val fastAdapter = FastAdapter.with(itemAdapter)

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // set the recyclerView
        binding.listUsers.adapter = fastAdapter
        binding.listUsers.layoutManager = LinearLayoutManager(this)

        /**
         * On click on a user from the list initialize the call client for that user
         * save the userAlias to be used for login after the call client has been initialized
         */
        fastAdapter.onClickListener = { _, _, item, _ ->
            userAlias = item.userAlias
            //This statement is needed to initialize the call client, establishing a secure connection with Bandyer platform.
            CallClient.getInstance().init(userAlias)
            false
        }

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

    override fun onCallClientFailed(reason: CallClientException) {
        Log.e("CallClient", "failed" + reason.message)
    }

    companion object {

        fun show(context: Activity) {
            val intent = Intent(context, LoginActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
        }
    }

}
