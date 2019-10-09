/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center.utils.networking

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * WARNING!!!
 * The networking package is used only to fetch the users, to make the demo app run out of the box.
 * With the least efforts.
 * <p>
 * RetroFit ApiClient used to make the rest calls
 */
object APIClient {

    private var retrofit: Retrofit? = null

    fun getClient(apikey: String): Retrofit {
        if (retrofit != null)
            return retrofit!!

        val client = OkHttpClient.Builder()
                .addInterceptor(authenticationHeaders(apikey))
                .build()

        retrofit = Retrofit.Builder()
                .baseUrl("https://sandbox.bandyer.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        return retrofit!!
    }


    private fun authenticationHeaders(apikey: String): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()

            val request = original.newBuilder()
                    .header("apikey", apikey)
                    .method(original.method(), original.body())
                    .build()

            chain.proceed(request)
        }
    }

}