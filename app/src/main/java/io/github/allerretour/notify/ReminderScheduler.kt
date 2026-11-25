package io.github.allerretour.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.allerretour.MainActivity
import io.github.allerretour.R
import io.github.allerretour.data.AppState
import io.github.allerretour.data.CountryPack
import io.github.allerretour.data.PackRepository
import io.github.allerretour.data.StateStore
import io.github.allerretour.logic.ReminderKind
import io.github.allerretour.logic.ReminderPlan
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Local reminders only. Alarms are set on this phone with AlarmManager and shown as
 * notifications by [ReminderReceiver]. Nothing is sent anywhere: the app has no internet permission.
 */
object ReminderScheduler {
    const val CHANNEL = "reminders"
    private const val EXTRA_KIND = "kind"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Trip reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Passport, packing, check-in and other reminders you switch on in Kit"
            })
        }
    }

    private fun pending(ctx: Context, kind: ReminderKind): PendingIntent =
        PendingIntent.getBroadcast(
            ctx, kind.ordinal,
            Intent(ctx, ReminderReceiver::class.java).putExtra(EXTRA_KIND, kind.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Cancels every alarm and sets the switched-on ones again. Safe to call on every change. */
    fun reschedule(ctx: Context, s: AppState, packs: Map<String, CountryPack>) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val now = LocalDateTime.now()
        ReminderKind.entries.forEach { am.cancel(pending(ctx, it)) }
        ReminderPlan.plan(s, packs, now.toLocalDate()).forEach { p ->
            if (!ReminderPlan.isOn(s, p.kind)) return@forEach
            val fireAt = when {
                p.daily != null -> now.toLocalDate().atTime(p.daily).let { if (it.isAfter(now)) it else it.plusDays(1) }
                p.at != null && p.at.isAfter(now) -> p.at
                else -> null
            } ?: return@forEach
            val millis = fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            // Inexact, battery-friendly alarm: needs no special permission and may arrive a few minutes late.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending(ctx, p.kind))
        }
    }

    fun show(ctx: Context, kind: ReminderKind, body: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel(ctx)
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(kind.title.substringBefore(":"))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(ctx).notify(kind.ordinal, n) }
    }

    fun extraKind(i: Intent): ReminderKind? = i.getStringExtra(EXTRA_KIND)?.let { k -> ReminderKind.entries.firstOrNull { it.name == k } }

    /** Reloads saved data and reschedules. Used after a reminder fires and after a restart. */
    fun reload(ctx: Context) {
        val s = StateStore(ctx).load()
        reschedule(ctx, s, PackRepository(ctx).packs)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val kind = ReminderScheduler.extraKind(intent) ?: return
        val s = StateStore(ctx).load()
        val packs = PackRepository(ctx).packs
        val p = ReminderPlan.plan(s, packs).firstOrNull { it.kind == kind }
        if (p != null && ReminderPlan.isOn(s, kind)) ReminderScheduler.show(ctx, kind, p.body)
        ReminderScheduler.reschedule(ctx, s, packs) // sets the next daily one
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) ReminderScheduler.reload(ctx)
    }
}
