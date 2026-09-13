package co.uan.epilepsy.dpa_saam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import co.uan.epilepsy.dpa_saam.core.di.ProveerContenedorDependenciasApp
import co.uan.epilepsy.dpa_saam.core.permission.CoordinadorPermisos
import co.uan.epilepsy.dpa_saam.presentation.main.FabricaMonitoreoViewModel
import co.uan.epilepsy.dpa_saam.presentation.main.MonitoreoViewModel
import co.uan.epilepsy.dpa_saam.ui.screen.PantallaMonitoreoPrincipal
import com.uan.designsystem.uikit.theme.UanTheme

class ActividadMonitoreoPrincipal : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as DpaSaamApplication
        val contenedor = app.contenedor

        setContent {
            UanTheme {
                ProveerContenedorDependenciasApp(contenedor) {
                    val viewModel: MonitoreoViewModel = viewModel(
                        factory = FabricaMonitoreoViewModel(contenedor),
                    )

                    CoordinadorPermisos(
                        viewModel = viewModel,
                        alEstarPermisosListos = { viewModel.alEstarPermisosListos() },
                    ) {
                        PantallaMonitoreoPrincipal(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
