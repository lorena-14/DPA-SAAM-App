package co.uan.epilepsy.dpa_saam

import android.app.Application
import co.uan.epilepsy.dpa_saam.core.di.ContenedorDependenciasApp

class DpaSaamApplication : Application() {

    lateinit var contenedor: ContenedorDependenciasApp
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = ContenedorDependenciasApp(this)
    }
}
