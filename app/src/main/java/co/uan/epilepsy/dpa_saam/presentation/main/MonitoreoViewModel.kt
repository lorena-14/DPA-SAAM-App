package co.uan.epilepsy.dpa_saam.presentation.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
            EventoUiMonitoreo.ConfirmarExencionBateria -> solicitarExencionOptimizacionBateria(contextoAplicacion)
            EventoUiMonitoreo.OcultarDialogoBateria -> _estadoUi.update { it.copy(mostrarDialogoOptimizacionBateria = false) }
            EventoUiMonitoreo.OcultarDialogoNotificaciones -> _estadoUi.update { it.copy(mostrarDialogoPermisoNotificaciones = false) }
            EventoUiMonitoreo.SimularAlertaPrueba -> {
                SimuladorAlertasPrueba.dispararAlertaSimulada(
                    repositorioAlertas = repositorioAlertas,
                    gestorNotificaciones = gestorNotificacionesLocales,
                    alcanceCorrutina = viewModelScope,
                )
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
        try {
            val intencion = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${contexto.packageName}")
            }
            contexto.startActivity(intencion)
            _estadoUi.update { it.copy(mostrarDialogoOptimizacionBateria = false) }
        } catch (_: Exception) {
            mostrarSnackbar(contexto.getString(R.string.error_battery_opt))
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
                            _estadoUi.update { it.copy(mostrarDialogoOptimizacionBateria = true) }
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
                    _estadoUi.update {
                        it.copy(
                            historialAlertas = historial,
                            ultimaLecturaRecibida = historial.firstOrNull(),
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
