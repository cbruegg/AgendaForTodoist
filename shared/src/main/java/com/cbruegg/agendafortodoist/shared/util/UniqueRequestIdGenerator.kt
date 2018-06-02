package com.cbruegg.agendafortodoist.shared.util

import java.util.Random

object UniqueRequestIdGenerator {

    private val rand = Random()

    fun nextRequestId() = rand.nextInt()
}