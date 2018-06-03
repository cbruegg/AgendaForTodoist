package com.cbruegg.agendafortodoist.shared.auth

import com.cbruegg.agendafortodoist.shared.moshi
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val API_BASE = "https://agendafortodoist-auth.cbruegg.com/"

val authService: AuthServiceApi by lazy {
    val retrofit = Retrofit.Builder()
        .baseUrl(API_BASE)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    retrofit.create(AuthServiceApi::class.java)
}

interface AuthServiceApi {
    @GET("request-redirect-shorturl")
    fun requestRedirectShortUrl(@Query("requestId") requestId: String): Call<ResponseBody>

    @GET("auth-code")
    fun authCode(@Query("requestId") requestId: String): Call<AuthDto>
}

