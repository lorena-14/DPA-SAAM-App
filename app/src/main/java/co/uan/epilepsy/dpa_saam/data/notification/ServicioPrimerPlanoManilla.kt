package co.uan.epilepsy.dpa_saam.data.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.content.ContextCompat
import co.uan.epilepsy.dpa_saam.DpaSaamApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ServicioPrimerPlanoManilla : Service() {

    private val trabajoServicio = Job()
    private val alcanceServicio = CoroutineScope(Dispatchers.IO + trabajoServicio)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return try {
            val contenedorApp = (application as DpaSaamApplication).contenedor
            val gestorNotificaciones = contenedorApp.gestorNotificacionesLocales
            val repositorioBluetooth = contenedorApp.repositorioBluetooth
            val repositorioAlertas = contenedorApp.repositorioAlertas

            val notificacion = gestorNotificaciones.construirNotificacionPrimerPlano()
            startForeground(
                GestorNotificacionesLocales.ID_NOTIFICACION_PRIMER_PLANO,
                notificacion,
            )

            alcanceServicio.launch {
                repositorioBluetooth.flujoLecturas.collect { lectura ->
                    repositorioAlertas.insertar(lectura)
                    gestorNotificaciones.mostrarNotificacionAlerta(lectura)

                    lectura.porcentajeBateria?.let { porcentaje ->
                        if (porcentaje <= 20) {
                            gestorNotificaciones.mostrarNotificacionBateriaBaja(porcentaje)
                        }
                    }
                }
            }
            START_STICKY
        } catch (_: Exception) {
            stopSelf()
            START_NOT_STICKY
        }
    }

    override fun onDestroy() {
        trabajoServicio.cancel()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    companion object {
        fun iniciar(contexto: Context) {
            try {
                val intencion = Intent(contexto, ServicioPrimerPlanoManilla::class.java)
                ContextCompat.startForegroundService(contexto, intencion)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun detener(contexto: Context) {
            try {
                contexto.stopService(Intent(contexto, ServicioPrimerPlanoManilla::class.java))
            } catch (_: Exception) {
            }
        }
    }
}
