package com.cbruegg.agendafortodoist.shared.todoist

import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

private const val API_BASE = "https://beta.todoist.com/API/v8/"

interface AccessTokenGetter {
    val accessToken: String
}

fun todoist(accessTokenGetter: AccessTokenGetter): TodoistApi {
    val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    val okHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val curUrl = chain.request().url()
                val newUrl = curUrl.newBuilder()
                        .addQueryParameter("token", accessTokenGetter.accessToken)
                        .build()

                chain.proceed(
                        chain.request().newBuilder()
                                .url(newUrl)
                                .build()
                )
            }
            .build()

    val retrofit = Retrofit.Builder()
            .baseUrl(API_BASE)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(okHttp)
            .build()

    return retrofit.create(TodoistApi::class.java)
}

private const val REQ_ID_HEADER = "X-Request-Id"

interface TodoistApi {

    @GET("projects")
    fun projects(): Call<List<ProjectDto>>

    @GET("tasks")
    fun tasks(@Query("project_id") projectId: Long? = null, @Query("label_id") labelId: Long? = null): Call<List<TaskDto>>

    @POST("tasks/{id}/close")
    fun closeTask(@Path("id") taskId: Long, @Header(REQ_ID_HEADER) requestId: Int): Call<Void>

    @POST("tasks/{id}/reopen")
    fun reopenTask(@Path("id") taskId: Long, @Header(REQ_ID_HEADER) requestId: Int): Call<Void>

    @POST("tasks") @Headers("Content-Type: application/json")
    fun addTask(@Header(REQ_ID_HEADER) requestId: Int, @Body taskDto: NewTaskDto): Call<Void>
}

