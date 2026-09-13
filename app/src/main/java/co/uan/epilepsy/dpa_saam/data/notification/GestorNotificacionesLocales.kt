package co.uan.epilepsy.dpa_saam.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import co.uan.epilepsy.dpa_saam.ActividadMonitoreoPrincipal
import co.uan.epilepsy.dpa_saam.R
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class GestorNotificacionesLocales(
    private val contexto: Context,
) {

    private val administradorNotificaciones =
        contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val formateadorHora = DateTimeFormatter.ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    init {
        crearCanalesNotificacion()
    }

    fun mostrarNotificacionAlerta(lectura: LecturaManilla) {
        try {
            val horaTexto = formateadorHora.format(lectura.fechaRecepcion)
            val cuerpoMensaje = "SpO2: ${lectura.spo2}% · BPM: ${lectura.bpm} · $horaTexto"
            val notificacion = NotificationCompat.Builder(contexto, CANAL_ALERTAS)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(contexto.getString(R.string.notification_alert_title))
                .setContentText(cuerpoMensaje)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(crearPendingIntentPrincipal())
                .build()

            administradorNotificaciones.notify(ID_NOTIFICACION_ALERTA, notificacion)
        } catch (_: Exception) {
        }
    }

    fun mostrarNotificacionBateriaBaja(porcentaje: Int) {
        try {
            val notificacion = NotificationCompat.Builder(contexto, CANAL_BATERIA)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(contexto.getString(R.string.notification_battery_title))
                .setContentText(
                    contexto.getString(R.string.notification_battery_body, porcentaje),
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(crearPendingIntentPrincipal())
                .build()

            administradorNotificaciones.notify(ID_NOTIFICACION_BATERIA, notificacion)
        } catch (_: Exception) {
        }
    }

    fun construirNotificacionPrimerPlano(): android.app.Notification {
        return NotificationCompat.Builder(contexto, CANAL_PRIMER_PLANO)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(contexto.getString(R.string.notification_foreground_title))
            .setContentText(contexto.getString(R.string.notification_foreground_body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(crearPendingIntentPrincipal())
            .build()
    }

    private fun crearCanalesNotificacion() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val canales = listOf(
            NotificationChannel(
                CANAL_ALERTAS,
                contexto.getString(R.string.channel_alerts_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
            NotificationChannel(
                CANAL_BATERIA,
                contexto.getString(R.string.channel_battery_name),
                NotificationManager.IMPORTANCE_HIGH,
            ),
            NotificationChannel(
                CANAL_PRIMER_PLANO,
                contexto.getString(R.string.channel_foreground_name),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        canales.forEach { administradorNotificaciones.createNotificationChannel(it) }
    }

    private fun crearPendingIntentPrincipal(): PendingIntent {
        val intencion = Intent(contexto, ActividadMonitoreoPrincipal::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            contexto,
            0,
            intencion,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val CANAL_ALERTAS = "alertas_manilla"
        const val CANAL_BATERIA = "bateria_manilla"
        const val CANAL_PRIMER_PLANO = "conexion_manilla"
        const val ID_NOTIFICACION_ALERTA = 1001
        const val ID_NOTIFICACION_BATERIA = 1002
        const val ID_NOTIFICACION_PRIMER_PLANO = 1003
    }
}
