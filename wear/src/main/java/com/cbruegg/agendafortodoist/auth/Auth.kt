package com.cbruegg.agendafortodoist.auth

import com.cbruegg.agendafortodoist.shared.auth.AuthDto

data class Auth(val tokenType: String, val accessToken: String)

fun AuthDto.toAuth() = Auth(tokenType, accessToken)