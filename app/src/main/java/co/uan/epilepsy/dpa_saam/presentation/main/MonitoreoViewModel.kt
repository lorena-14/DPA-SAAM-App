package co.uan.epilepsy.dpa_saam.presentation.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import co.uan.epilepsy.dpa_saam.R
import co.uan.epilepsy.dpa_saam.core.di.ContenedorDependenciasApp
import co.uan.epilepsy.dpa_saam.data.notification.ServicioPrimerPlanoManilla
import co.uan.epilepsy.dpa_saam.debug.SimuladorAlertasPrueba
import co.uan.epilepsy.dpa_saam.domain.model.EstadoConexionBluetooth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MonitoreoViewModel(
    contenedor: ContenedorDependenciasApp,
) : ViewModel() {

    private val repositorioBluetooth = contenedor.repositorioBluetooth
    private val repositorioAlertas = contenedor.repositorioAlertas
    private val gestorNotificacionesLocales = contenedor.gestorNotificacionesLocales
    private val contextoAplicacion = contenedor.contextoAplicacion

    private val _estadoUi = MutableStateFlow(EstadoUiMonitoreo())
    val estadoUi: StateFlow<EstadoUiMonitoreo> = _estadoUi.asStateFlow()

    private var yaSeConectoUnaVez = false

    init {
        observarEstadoConexion()
        observarHistorialAlertas()
        observarInformacionBateria()
        observarErroresBluetooth()
    }

    fun alEstarPermisosListos() {
        _estadoUi.update { it.copy(permisosListos = true) }
        repositorioBluetooth.iniciarConexionAutomatica()
    }

    fun procesarEvento(evento: EventoUiMonitoreo) {
        when (evento) {
            EventoUiMonitoreo.ReintentarConexion -> repositorioBluetooth.reintentarConexion()
            EventoUiMonitoreo.OcultarSnackbar -> _estadoUi.update { it.copy(mensajeNotificacionSnackbar = null) }
            EventoUiMonitoreo.OcultarDialogoBateria -> _estadoUi.update { it.copy(mostrarDialogoOptimizacionBateria = false) }
            EventoUiMonitoreo.OcultarDialogoNotificaciones -> _estadoUi.update { it.copy(mostrarDialogoPermisoNotificaciones = false) }
            EventoUiMonitoreo.SimularAlertaPrueba -> {
                SimuladorAlertasPrueba.dispararAlertaSimulada(
                    repositorioAlertas = repositorioAlertas,
                    gestorNotificaciones = gestorNotificacionesLocales,
                    alcanceCorrutina = viewModelScope,
                )
            }
            is EventoUiMonitoreo.AlternarSeleccionAlerta -> {
                val nuevoId = if (_estadoUi.value.alertaSeleccionadaId == evento.id) null else evento.id
                _estadoUi.update { it.copy(alertaSeleccionadaId = nuevoId) }
            }
            EventoUiMonitoreo.SolicitarEliminacionAlerta -> _estadoUi.update { it.copy(mostrarDialogoEliminarAlerta = true) }
            EventoUiMonitoreo.CancelarEliminacionAlerta -> _estadoUi.update { it.copy(mostrarDialogoEliminarAlerta = false) }
            EventoUiMonitoreo.ConfirmarEliminacionAlerta -> eliminarAlertaSeleccionada()
        }
    }

    private fun eliminarAlertaSeleccionada() {
        val id = _estadoUi.value.alertaSeleccionadaId ?: return
        _estadoUi.update { it.copy(mostrarDialogoEliminarAlerta = false) }
        viewModelScope.launch {
            when (val resultado = repositorioAlertas.eliminarPorId(id)) {
                is co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp.Exito -> {
                    _estadoUi.update { it.copy(alertaSeleccionadaId = null) }
                    mostrarSnackbar(contextoAplicacion.getString(R.string.msg_alert_deleted))
                }
                is co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp.Error -> {
                    mostrarSnackbar(resultado.mensaje)
                }
            }
        }
    }

    fun mostrarDialogoPermisoNotificaciones() {
        _estadoUi.update { it.copy(mostrarDialogoPermisoNotificaciones = true) }
    }

    fun mostrarSnackbar(mensaje: String) {
        _estadoUi.update { it.copy(mensajeNotificacionSnackbar = mensaje) }
    }

    fun abrirAjustesAplicacion(contexto: Context) {
        try {
            val intencion = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", contexto.packageName, null),
            )
            contexto.startActivity(intencion)
        } catch (_: Exception) {
            mostrarSnackbar(contexto.getString(R.string.error_app_settings))
        }
    }

    fun solicitarExencionOptimizacionBateria(contexto: Context) {
        _estadoUi.update { it.copy(mostrarDialogoOptimizacionBateria = false) }
        try {
            val intencion = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${contexto.packageName}")
            }
            contexto.startActivity(intencion)
        } catch (_: Exception) {
            mostrarSnackbar(contextoAplicacion.getString(R.string.error_battery_opt))
        }
    }

    private fun observarEstadoConexion() {
        viewModelScope.launch {
            repositorioBluetooth.estadoConexion.collect { estado ->
                _estadoUi.update {
                    it.copy(
                        estadoConexion = estado,
                        estaCargando = (estado == EstadoConexionBluetooth.Escaneando || estado == EstadoConexionBluetooth.Conectando),
                    )
                }

                when (estado) {
                    EstadoConexionBluetooth.Conectado -> {
                        ServicioPrimerPlanoManilla.iniciar(contextoAplicacion)
                        if (!yaSeConectoUnaVez) {
                            yaSeConectoUnaVez = true
                            val gestorEnergia = contextoAplicacion.getSystemService(Context.POWER_SERVICE) as PowerManager
                            val estaIgnorandoOptimizacion = gestorEnergia.isIgnoringBatteryOptimizations(contextoAplicacion.packageName)
                            if (!estaIgnorandoOptimizacion) {
                                _estadoUi.update { it.copy(mostrarDialogoOptimizacionBateria = true) }
                            }
                        }
                    }

                    EstadoConexionBluetooth.Fallido -> {
                        ServicioPrimerPlanoManilla.detener(contextoAplicacion)
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun observarHistorialAlertas() {
        viewModelScope.launch {
            repositorioAlertas.observarTodasLasAlertas().collect { historial ->
                try {
                    _estadoUi.update { estadoActual ->
                        val idSeleccionado = estadoActual.alertaSeleccionadaId
                        val sigueExistiendo = idSeleccionado != null && historial.any { it.id == idSeleccionado }
                        estadoActual.copy(
                            historialAlertas = historial,
                            ultimaLecturaRecibida = historial.firstOrNull(),
                            alertaSeleccionadaId = if (sigueExistiendo) idSeleccionado else null,
                            mostrarDialogoEliminarAlerta = if (sigueExistiendo) estadoActual.mostrarDialogoEliminarAlerta else false
                        )
                    }
                } catch (e: Exception) {
                    mostrarSnackbar("Error al cargar historial: ${e.message}")
                }
            }
        }
    }

    private fun observarInformacionBateria() {
        viewModelScope.launch {
            repositorioBluetooth.informacionBateria.collect { bateria ->
                _estadoUi.update { it.copy(informacionBateria = bateria) }
            }
        }
    }

    private fun observarErroresBluetooth() {
        viewModelScope.launch {
            repositorioBluetooth.flujoErroresBluetooth.collect { errorMsg ->
                mostrarSnackbar(errorMsg)
            }
        }
    }
}

class FabricaMonitoreoViewModel(
    private val contenedor: ContenedorDependenciasApp,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MonitoreoViewModel::class.java)) {
            return MonitoreoViewModel(contenedor) as T
        }
        throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
    }
}
