package io.github.allerretour.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Native date picker. [min] stops the leave date going before the arrival date. */
fun pickDate(context: Context, current: String, min: String? = null, onPicked: (String) -> Unit) {
    val d = runCatching { LocalDate.parse(current) }.getOrNull() ?: LocalDate.now()
    val dlg = DatePickerDialog(context, { _, y, m, day -> onPicked(LocalDate.of(y, m + 1, day).toString()) }, d.year, d.monthValue - 1, d.dayOfMonth)
    runCatching { LocalDate.parse(min) }.getOrNull()?.let { dlg.datePicker.minDate = it.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() }
    dlg.show()
}

fun pickTime(context: Context, current: LocalTime, onPicked: (LocalTime) -> Unit) {
    TimePickerDialog(context, { _, h, m -> onPicked(LocalTime.of(h, m)) }, current.hour, current.minute, true).show()
}

fun dial(context: Context, telUri: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(telUri))) }
        .onFailure { toast(context, "No phone app available") }
}

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(send, null))
}

/** Opens a link in the user's browser. The app itself never connects to anything. */
fun openLink(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { toast(context, "No browser available") }
}

fun toast(context: Context, msg: String) = Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
