package com.ilsecondodasinistra.workitout.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.ilsecondodasinistra.workitout.R
import com.ilsecondodasinistra.workitout.WorkItOutApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Buttons of the persistent notification, its minute tick and the swipe-away. */
class SessionNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as WorkItOutApp).container
        val action = intent.action ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (action) {
                    ACTION_TOGGLE_PAUSE -> container.sessionController.togglePause()
                    ACTION_EXIT -> container.sessionController.finish()?.let { entry ->
                        val text = context.getString(R.string.notif_exit_recorded, entry.totalWorkedTime)
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context.applicationContext, text, Toast.LENGTH_LONG).show()
                        }
                    }
                    ACTION_DISMISSED -> container.sessionNotifier.dismissed(intent.getLongExtra(EXTRA_ENTER_TIME, 0L))
                }
                container.sessionNotifier.sync()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TICK = "com.ilsecondodasinistra.workitout.SESSION_TICK"
        const val ACTION_TOGGLE_PAUSE = "com.ilsecondodasinistra.workitout.SESSION_TOGGLE_PAUSE"
        const val ACTION_EXIT = "com.ilsecondodasinistra.workitout.SESSION_EXIT"
        const val ACTION_DISMISSED = "com.ilsecondodasinistra.workitout.SESSION_DISMISSED"
        const val EXTRA_ENTER_TIME = "enter_time"
    }
}
