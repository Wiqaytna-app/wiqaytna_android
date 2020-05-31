package covid.trace.morocco.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import covid.trace.morocco.BuildConfig
import covid.trace.morocco.MainActivity
import covid.trace.morocco.R
import covid.trace.morocco.onboarding.OnboardingActivity
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_ACTIVITY
import covid.trace.morocco.services.BluetoothMonitoringService.Companion.PENDING_WIZARD_REQ_CODE

class NotificationTemplates {

    companion object {

        fun getStartupNotification(context: Context, channel: String): Notification {

            val builder = NotificationCompat.Builder(context, channel)
                .setContentText("Tracer is setting up its antennas")
                .setContentTitle("Setting things up")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_setting)
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

        fun getRunningNotification(context: Context, channel: String): Notification {

            var intent = Intent(context, MainActivity::class.java)

            val activityPendingIntent = PendingIntent.getActivity(
                context, PENDING_ACTIVITY,
                intent, 0
            )

            val builder = NotificationCompat.Builder(context, channel)
                .setContentTitle(context.resources.getString(R.string.service_ok_title))
                .setContentText(context.resources.getString(R.string.service_ok_body))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_service)
                .setContentIntent(activityPendingIntent)
                .setTicker(context.resources.getString(R.string.service_ok_body))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.resources.getString(R.string.service_ok_body))
                )
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }

        fun lackingThingsNotification(context: Context, channel: String): Notification {
            var intent = Intent(context, OnboardingActivity::class.java)
            intent.putExtra("page", 3)

            val activityPendingIntent = PendingIntent.getActivity(
                context, PENDING_WIZARD_REQ_CODE,
                intent, 0
            )

            val builder = NotificationCompat.Builder(context, channel)
                .setContentText(context.resources.getString(R.string.service_not_ok_title))
                .setContentTitle(context.resources.getString(R.string.service_not_ok_body))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_LOW)
                .setSmallIcon(R.drawable.ic_notification_warning)
                .setTicker(context.resources.getString(R.string.service_not_ok_body))
                .addAction(
                    R.drawable.ic_notification_setting,
                    context.resources.getString(R.string.service_not_ok_action),
                    activityPendingIntent
                )
                .setContentIntent(activityPendingIntent)
                .setWhen(System.currentTimeMillis())
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            return builder.build()
        }


        fun diskFullWarningNotification(context: Context, channel: String){

            val builder = NotificationCompat.Builder(context, channel)
                .setContentText(context.resources.getString(R.string.notif_storage_problem_sub))
                .setContentTitle(context.resources.getString(R.string.notif_storage_problem))
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_notification_warning)
                .setTicker(context.resources.getString(R.string.notif_storage_problem))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setSound(null)
                .setVibrate(null)
                .setColor(ContextCompat.getColor(context, R.color.notification_tint))

            val notif =  builder.build()

            with(NotificationManagerCompat.from(context)) {
                notify(BuildConfig.ERROR_NOTIFICATION_ID, notif)
            }
        }
    }
}
