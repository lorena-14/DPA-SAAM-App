package co.uan.epilepsy.dpa_saam.data.repository

import co.uan.epilepsy.dpa_saam.data.ble.GestorConexionBluetooth
import co.uan.epilepsy.dpa_saam.domain.model.EstadoConexionBluetooth
import co.uan.epilepsy.dpa_saam.domain.model.InformacionBateria
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import co.uan.epilepsy.dpa_saam.domain.repository.RepositorioBluetooth
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class RepositorioBluetoothImplementacion(
    private val gestorConexionBluetooth: GestorConexionBluetooth,
) : RepositorioBluetooth {

    override val estadoConexion: StateFlow<EstadoConexionBluetooth> = gestorConexionBluetooth.estadoConexion
    override val flujoLecturas: SharedFlow<LecturaManilla> = gestorConexionBluetooth.flujoLecturas
    override val informacionBateria: StateFlow<InformacionBateria> = gestorConexionBluetooth.informacionBateria
    override val flujoErroresBluetooth: SharedFlow<String> = gestorConexionBluetooth.flujoErroresBluetooth

    override fun iniciarConexionAutomatica() = gestorConexionBluetooth.iniciarConexionAutomatica()
    override fun reintentarConexion() = gestorConexionBluetooth.reintentarConexion()
    override fun desconectar() = gestorConexionBluetooth.desconectar()
}
