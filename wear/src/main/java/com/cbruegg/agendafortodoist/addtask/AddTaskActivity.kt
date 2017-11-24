package com.cbruegg.agendafortodoist.addtask

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.support.wearable.activity.WearableActivity
import android.widget.Toast
import com.cbruegg.agendafortodoist.R
import com.cbruegg.agendafortodoist.app
import com.cbruegg.agendafortodoist.auth.AuthActivity
import com.cbruegg.agendafortodoist.shared.todoist.NewTaskDto
import com.cbruegg.agendafortodoist.util.UniqueRequestIdGenerator
import com.cbruegg.agendafortodoist.util.retry
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import retrofit2.HttpException
import ru.gildor.coroutines.retrofit.awaitResponse
import java.io.IOException

private const val EXTRA_PROJECT_ID = "project_id"

fun newAddTaskActivityIntent(context: Context, projectId: Long) =
        Intent(context, AddTaskActivity::class.java).apply {
            putExtra(EXTRA_PROJECT_ID, projectId)
        }

class AddTaskActivity : WearableActivity() {

    private val requestCodeSpeech = 0

    private fun displaySpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        startActivityForResult(intent, requestCodeSpeech)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent) {
        if (requestCode == requestCodeSpeech) {
            if (resultCode == Activity.RESULT_OK) {
                val results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results[0]
                onReceiveText(spokenText)
            } else {
                Toast.makeText(this@AddTaskActivity, R.string.speech_input_error, Toast.LENGTH_LONG).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun onReceiveText(spokenText: String) {
        launch(UI) {
            try {
                val requestId = UniqueRequestIdGenerator.nextRequestId()
                val projectId = intent.getLongExtra(EXTRA_PROJECT_ID, -1)
                retry(HttpException::class.java, IOException::class.java) {
                    val resp = app.netComponent.todoist().addTask(requestId, NewTaskDto(spokenText, projectId))
                            .awaitResponse()
                    if (resp.code() !in 200..299) throw HttpException(resp)
                }
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    startActivity(
                            Intent(this@AddTaskActivity, AuthActivity::class.java)
                                    .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) }
                    )
                }

                e.printStackTrace()
                System.err.println(e.response().errorBody()?.string())
                Toast.makeText(this@AddTaskActivity, R.string.http_error, Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this@AddTaskActivity, R.string.network_error, Toast.LENGTH_LONG).show()
            }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        displaySpeechRecognizer()
    }
}