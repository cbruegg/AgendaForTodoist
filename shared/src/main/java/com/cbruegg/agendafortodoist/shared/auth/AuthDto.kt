package com.cbruegg.agendafortodoist.shared.auth

import com.squareup.moshi.Json

data class AuthDto(
        @Json(name = "access_token") val accessToken: String,
        @Json(name = "token_type") val tokenType: String
)