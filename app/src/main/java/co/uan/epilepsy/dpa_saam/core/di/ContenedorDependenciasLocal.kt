package co.uan.epilepsy.dpa_saam.core.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalContenedorDependenciasApp = staticCompositionLocalOf<ContenedorDependenciasApp> {
    error("ContenedorDependenciasApp no disponible")
}

@Composable
fun ProveerContenedorDependenciasApp(
    contenedor: ContenedorDependenciasApp,
    contenido: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalContenedorDependenciasApp provides contenedor) {
        contenido()
    }
}
