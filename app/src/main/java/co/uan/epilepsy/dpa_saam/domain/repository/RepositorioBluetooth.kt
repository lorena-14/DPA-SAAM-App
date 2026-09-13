package co.uan.epilepsy.dpa_saam.domain.repository

import co.uan.epilepsy.dpa_saam.domain.model.EstadoConexionBluetooth
import co.uan.epilepsy.dpa_saam.domain.model.InformacionBateria
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RepositorioBluetooth {
    val estadoConexion: StateFlow<EstadoConexionBluetooth>
    val flujoLecturas: SharedFlow<LecturaManilla>
    val informacionBateria: StateFlow<InformacionBateria>
    val flujoErroresBluetooth: SharedFlow<String>

    fun iniciarConexionAutomatica()
    fun reintentarConexion()
    fun desconectar()
}
