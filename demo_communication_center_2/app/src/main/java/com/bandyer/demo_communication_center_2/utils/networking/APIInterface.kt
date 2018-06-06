package com.bandyer.demo_communication_center_2.utils.networking

import retrofit2.Call
import retrofit2.http.GET

/**
 * WARNING!!!
 * The networking package is used only to fetch the users, to make the demo app run out of the box.
 * With the least efforts.
 * <p>
 * Defines Rest calls available to be used
 */
interface APIInterface {

    /**
     * Method returns the mocked users that have been created by us or you via server side rest calls.
     *
     * @return BandyerUsers
     */
    @get:GET("rest/user/list")
    val users: Call<BandyerUsers>

}