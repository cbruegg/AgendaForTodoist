package com.cbruegg.agendafortodoist.auth

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.support.wearable.phone.PhoneDeviceType
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.WearableActivity
import com.cbruegg.agendafortodoist.app
import com.cbruegg.agendafortodoist.projects.ProjectsActivity
import com.cbruegg.agendafortodoist.shared.auth.AuthDto
import com.cbruegg.agendafortodoist.shared.auth.AuthServiceApi
import com.google.android.wearable.intent.RemoteIntent
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.await
import ru.gildor.coroutines.retrofit.awaitResponse
import java.io.IOException
import java.util.UUID

class AuthActivity : WearableActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        val settings = app.applicationComponent.settings()
        val authService = app.netComponent.authService()

        val message = findViewById<TextView>(R.id.auth_message)
        val sendToPhone = findViewById<Button>(R.id.auth_send_to_phone)

        launch(UI) {
            val requestId = UUID.randomUUID().toString()
            val shortUrl = try {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                authService.requestRedirectShortUrl(requestId).await().string()
            } catch (e: HttpException) {
                e.printStackTrace()
                message.text = getString(R.string.http_error)
                return@launch
            } catch (e: IOException) {
                e.printStackTrace()
                message.text = getString(R.string.network_error)
                return@launch
            } finally {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }

            message.text = getString(R.string.auth_visit_template, shortUrl)
            if (PhoneDeviceType.getPhoneDeviceType(this@AuthActivity) != PhoneDeviceType.DEVICE_TYPE_ERROR_UNKNOWN) {
                sendToPhone.setOnClickListener { sendUrlToPhone(shortUrl) }
                sendToPhone.visibility = View.VISIBLE
            }

            val authDto = waitForAuth(authService, requestId)
            settings.auth = authDto.toAuth()
            startActivity(Intent(this@AuthActivity, ProjectsActivity::class.java))
            finish()
        }
    }

    private suspend fun waitForAuth(authService: AuthServiceApi, requestId: String): AuthDto {
        while (true) {
            delay(1000)
            val resp = try {
                authService.authCode(requestId).awaitResponse()
            } catch (e: IOException) {
                e.printStackTrace()
                continue
            }

            if (resp.isSuccessful) {
                return resp.body() ?: throw NullPointerException("Received invalid DTO!")
            }
        }
    }

    private fun sendUrlToPhone(url: String) {
        val intent = Intent(ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(url))

        RemoteIntent.startRemoteActivity(this@AuthActivity, intent, null)
    }
}

