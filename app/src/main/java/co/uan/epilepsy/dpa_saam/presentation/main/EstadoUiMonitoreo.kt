package co.uan.epilepsy.dpa_saam.presentation.main

import co.uan.epilepsy.dpa_saam.domain.model.EstadoConexionBluetooth
import co.uan.epilepsy.dpa_saam.domain.model.InformacionBateria
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla

data class EstadoUiMonitoreo(
    val estadoConexion: EstadoConexionBluetooth = EstadoConexionBluetooth.Inactivo,
    val ultimaLecturaRecibida: LecturaManilla? = null,
    val historialAlertas: List<LecturaManilla> = emptyList(),
    val informacionBateria: InformacionBateria = InformacionBateria(null, false),
    val permisosListos: Boolean = false,
    val estaCargando: Boolean = false,
    val mensajeNotificacionSnackbar: String? = null,
    val mostrarDialogoOptimizacionBateria: Boolean = false,
    val mostrarDialogoPermisoNotificaciones: Boolean = false,
)
