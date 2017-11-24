package com.cbruegg.agendafortodoist.auth

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.support.wearable.phone.PhoneDeviceType
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.Settings
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.projects.ProjectsActivity
import com.cbruegg.agendafortodoist.shared.auth.authService
import com.google.android.wearable.intent.RemoteIntent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import ru.gildor.coroutines.retrofit.awaitResponse
import java.util.UUID


class AuthActivity : WearableActivity() {

    // TODO Also handle IOExceptions everywhere
    // TODO Handle token expiry

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val settings = Settings(this)

        val message = findViewById<TextView>(R.id.auth_message)
        val sendToPhone = findViewById<Button>(R.id.auth_send_to_phone)

        launch(UI) {
            val requestId = UUID.randomUUID().toString()
            try {
                val shortUrl = authService.requestRedirectShortUrl(requestId).await().string()
                message.text = getString(R.string.auth_visit_template, shortUrl)
                if (PhoneDeviceType.getPhoneDeviceType(this@AuthActivity) != PhoneDeviceType.DEVICE_TYPE_ERROR_UNKNOWN) {
                    sendToPhone.setOnClickListener {
                        val intent = Intent(ACTION_VIEW)
                                .addCategory(Intent.CATEGORY_BROWSABLE)
                                .setData(Uri.parse(shortUrl))

                        RemoteIntent.startRemoteActivity(this@AuthActivity, intent, null)
                    }
                    sendToPhone.visibility = View.VISIBLE
                }

                while (true) {
                    delay(1000)
                    val resp = authService.authCode(requestId).awaitResponse()
                    if (resp.isSuccessful) {
                        val authDto = resp.body() ?: throw NullPointerException("Received invalid DTO!")
                        settings.storeAuth(authDto.toAuth())
                        startActivity(Intent(this@AuthActivity, ProjectsActivity::class.java))
                        finish()
                        break
                    }
                }
            } catch (e: HttpException) {

            }
        }
    }
}

