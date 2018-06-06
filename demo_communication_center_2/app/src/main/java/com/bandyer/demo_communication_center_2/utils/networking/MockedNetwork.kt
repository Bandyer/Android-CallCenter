package com.bandyer.demo_communication_center_2.utils.networking

import android.content.Context
import com.bandyer.demo_communication_center_2.R
import retrofit2.Callback

/**
 * WARNING!!!
 * The networking package is used only to fetch the users, to make the demo app run out of the box.
 * With the least efforts.
 * <p>
 * MockedNetwork
 *
 * @author kristiyan
 */
object MockedNetwork {

    fun getSampleUsers(context: Context, callback: Callback<BandyerUsers>) {
        val apikey = context.getString(R.string.api_key)
        val call = APIClient.getClient(apikey).create(APIInterface::class.java).users
        call.enqueue(callback)
    }
}
