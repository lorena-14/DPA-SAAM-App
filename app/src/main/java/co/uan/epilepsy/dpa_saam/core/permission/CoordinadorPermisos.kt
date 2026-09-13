package co.uan.epilepsy.dpa_saam.core.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.uan.epilepsy.dpa_saam.R
import co.uan.epilepsy.dpa_saam.presentation.main.EventoUiMonitoreo
import co.uan.epilepsy.dpa_saam.presentation.main.MonitoreoViewModel
import com.uan.designsystem.uikit.components.UanModal
import com.uan.designsystem.uikit.components.UanModalAction
import com.uan.designsystem.uikit.foundation.UanTone

private enum class PasoPermisos {
    Notificaciones,
    Bluetooth,
    Listo,
}

@Composable
fun CoordinadorPermisos(
    viewModel: MonitoreoViewModel,
    alEstarPermisosListos: () -> Unit,
    contenido: @Composable () -> Unit,
) {
    val contexto = LocalContext.current
    val actividad = LocalActivity.current
    var pasoActual by remember { mutableStateOf(PasoPermisos.Notificaciones) }
    val estadoUi by viewModel.estadoUi.collectAsState()

    fun tienePermisoNotificaciones(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun tienePermisosBluetooth(): Boolean {
        return ContextCompat.checkSelfPermission(
            contexto,
            Manifest.permission.BLUETOOTH_SCAN,
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.BLUETOOTH_CONNECT,
            ) == PackageManager.PERMISSION_GRANTED
    }

    val lanzadorNotificaciones = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { concedido ->
        if (concedido) {
            pasoActual = PasoPermisos.Bluetooth
        } else {
            viewModel.mostrarDialogoPermisoNotificaciones()
            viewModel.mostrarSnackbar(contexto.getString(R.string.permissions_notifications_snackbar))
        }
    }

    val lanzadorBluetooth = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { resultados ->
        val todosConcedidos = resultados.values.all { it }
        if (todosConcedidos) {
            pasoActual = PasoPermisos.Listo
            alEstarPermisosListos()
        } else {
            val denegadoPermanentemente = actividad?.let { act ->
                resultados.entries.any { (permiso, concedido) ->
                    !concedido && !ActivityCompat.shouldShowRequestPermissionRationale(act, permiso)
                }
            } ?: false

            viewModel.mostrarSnackbar(
                if (denegadoPermanentemente) {
                    contexto.getString(R.string.permissions_bluetooth_denied_permanently)
                } else {
                    contexto.getString(R.string.permissions_bluetooth_required)
                },
            )
        }
    }

    LaunchedEffect(pasoActual) {
        when (pasoActual) {
            PasoPermisos.Notificaciones -> {
                if (tienePermisoNotificaciones()) {
                    pasoActual = PasoPermisos.Bluetooth
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    lanzadorNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    pasoActual = PasoPermisos.Bluetooth
                }
            }

            PasoPermisos.Bluetooth -> {
                if (tienePermisosBluetooth()) {
                    pasoActual = PasoPermisos.Listo
                    alEstarPermisosListos()
                } else {
                    lanzadorBluetooth.launch(
                        arrayOf(
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT,
                        ),
                    )
                }
            }

            PasoPermisos.Listo -> Unit
        }
    }

    if (estadoUi.mostrarDialogoPermisoNotificaciones) {
        UanModal(
            visible = true,
            onDismissRequest = { viewModel.procesarEvento(EventoUiMonitoreo.OcultarDialogoNotificaciones) },
            title = stringResource(R.string.permissions_notifications_title),
            body = stringResource(R.string.permissions_notifications_body),
            primaryAction = UanModalAction(
                label = stringResource(R.string.permissions_notifications_grant),
                onClick = {
                    viewModel.procesarEvento(EventoUiMonitoreo.OcultarDialogoNotificaciones)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        lanzadorNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            ),
            secondaryAction = UanModalAction(
                label = stringResource(R.string.permissions_open_settings),
                onClick = {
                    viewModel.abrirAjustesAplicacion(contexto)
                    viewModel.procesarEvento(EventoUiMonitoreo.OcultarDialogoNotificaciones)
                },
            ),
            tone = UanTone.Info,
        )
    }

    contenido()
}
