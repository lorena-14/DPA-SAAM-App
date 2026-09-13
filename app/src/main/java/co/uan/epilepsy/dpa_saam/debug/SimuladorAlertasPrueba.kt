package co.uan.epilepsy.dpa_saam.debug

import co.uan.epilepsy.dpa_saam.data.notification.GestorNotificacionesLocales
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import co.uan.epilepsy.dpa_saam.domain.repository.RepositorioAlertas
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// CÓDIGO TEMPORAL - SOLO PRUEBAS
// TODO: Eliminar este archivo y su uso en MonitoreoViewModel y PantallaMonitoreoPrincipal antes de salir a producción.
object SimuladorAlertasPrueba {
    fun dispararAlertaSimulada(
        repositorioAlertas: RepositorioAlertas,
        gestorNotificaciones: GestorNotificacionesLocales,
        alcanceCorrutina: CoroutineScope,
    ) {
        val lecturaFalsa = LecturaManilla(
            id = 0,
            spo2 = 98.5f,
            bpm = 75,
            porcentajeBateria = 80,
            fechaRecepcion = Instant.now(),
            direccionMacDispositivo = "SIMULADOR_DEBUG",
        )

        alcanceCorrutina.launch(Dispatchers.IO) {
            repositorioAlertas.insertar(lecturaFalsa)
            gestorNotificaciones.mostrarNotificacionAlerta(lecturaFalsa)

            lecturaFalsa.porcentajeBateria?.let { porcentaje ->
                if (porcentaje <= 20) {
                    gestorNotificaciones.mostrarNotificacionBateriaBaja(porcentaje)
                }
            }
        }
    }
}
