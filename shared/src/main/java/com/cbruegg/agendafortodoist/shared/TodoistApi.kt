package com.cbruegg.agendafortodoist.shared

import com.squareup.moshi.Json
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val API_BASE = "https://beta.todoist.com/API/v8/"

val todoist: TodoistApi by lazy {
    val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

    val okHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val curUrl = chain.request().url()
                val newUrl = curUrl.newBuilder()
                        .addQueryParameter("token", BuildConfig.API_ID)
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

    retrofit.create(TodoistApi::class.java)
}

interface TodoistApi {
    // TODO Provide X-Request-Id header

    @GET("projects")
    fun projects(): Call<List<ProjectDto>>

    @GET("tasks")
    fun tasks(@Query("project_id") projectId: Long? = null, @Query("label_id") labelId: Long? = null): Call<List<TaskDto>>
}

data class ProjectDto(
        @Json(name = "id") val id: Long,
        @Json(name = "name") val name: String,
        @Json(name = "order") val order: Int,
        @Json(name = "indent") val indent: Int,
        @Json(name = "comment_count") val commentCount: Int
)

data class TaskDto(
        @Json(name = "id") val id: Long,
        @Json(name = "project_id") val projectId: Long,
        @Json(name = "content") val content: String,
        @Json(name = "completed") val isCompleted: Boolean,
        @Json(name = "label_ids") val labelIds: List<Int>?,
        @Json(name = "order") val order: Int,
        @Json(name = "indent") val indent: Int,
        @Json(name = "priority") val priority: Int,
        @Json(name = "url") val url: String,
        @Json(name = "comment_count") val commentCount: Int
)