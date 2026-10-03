package co.uan.epilepsy.dpa_saam.presentation.main

sealed interface EventoUiMonitoreo {
    data object ReintentarConexion : EventoUiMonitoreo
    data object OcultarSnackbar : EventoUiMonitoreo
    data object OcultarDialogoBateria : EventoUiMonitoreo
    data object OcultarDialogoNotificaciones : EventoUiMonitoreo
    data object SimularAlertaPrueba : EventoUiMonitoreo
    data class AlternarSeleccionAlerta(val id: Long) : EventoUiMonitoreo
    data object SolicitarEliminacionAlerta : EventoUiMonitoreo
    data object CancelarEliminacionAlerta : EventoUiMonitoreo
    data object ConfirmarEliminacionAlerta : EventoUiMonitoreo
}
