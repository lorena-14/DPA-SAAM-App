package co.uan.epilepsy.dpa_saam.core.di

import android.content.Context
import co.uan.epilepsy.dpa_saam.data.ble.AnalizadorTramaBluetooth
import co.uan.epilepsy.dpa_saam.data.ble.GestorConexionBluetooth
import co.uan.epilepsy.dpa_saam.data.local.room.BaseDatosDpaSaam
import co.uan.epilepsy.dpa_saam.data.notification.GestorNotificacionesLocales
import co.uan.epilepsy.dpa_saam.data.repository.RepositorioAlertasImplementacion
import co.uan.epilepsy.dpa_saam.data.repository.RepositorioBluetoothImplementacion
import co.uan.epilepsy.dpa_saam.domain.repository.RepositorioAlertas
import co.uan.epilepsy.dpa_saam.domain.repository.RepositorioBluetooth

class ContenedorDependenciasApp(contexto: Context) {

    val contextoAplicacion: Context = contexto.applicationContext

    private val baseDatos: BaseDatosDpaSaam by lazy {
        BaseDatosDpaSaam.obtenerInstancia(contextoAplicacion)
    }

    private val gestorConexionBluetooth: GestorConexionBluetooth by lazy {
        GestorConexionBluetooth(contextoAplicacion, AnalizadorTramaBluetooth())
    }

    val repositorioAlertas: RepositorioAlertas by lazy {
        RepositorioAlertasImplementacion(baseDatos.daoAlertas())
    }

    val repositorioBluetooth: RepositorioBluetooth by lazy {
        RepositorioBluetoothImplementacion(gestorConexionBluetooth)
    }

    val gestorNotificacionesLocales: GestorNotificacionesLocales by lazy {
        GestorNotificacionesLocales(contextoAplicacion)
    }
}
