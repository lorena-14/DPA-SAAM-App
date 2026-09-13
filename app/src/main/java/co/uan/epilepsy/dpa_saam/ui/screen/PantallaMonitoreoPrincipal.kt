package co.uan.epilepsy.dpa_saam.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import co.uan.epilepsy.dpa_saam.BuildConfig
import co.uan.epilepsy.dpa_saam.R
import co.uan.epilepsy.dpa_saam.domain.model.EstadoConexionBluetooth
import co.uan.epilepsy.dpa_saam.presentation.main.EstadoUiMonitoreo
import co.uan.epilepsy.dpa_saam.presentation.main.EventoUiMonitoreo
import co.uan.epilepsy.dpa_saam.presentation.main.MonitoreoViewModel
import com.uan.designsystem.uikit.components.UanAppBar
import com.uan.designsystem.uikit.components.UanBadge
import com.uan.designsystem.uikit.components.UanBadgeEmphasis
import com.uan.designsystem.uikit.components.UanButton
import com.uan.designsystem.uikit.components.UanButtonSize
import com.uan.designsystem.uikit.components.UanButtonStyle
import com.uan.designsystem.uikit.components.UanCard
import com.uan.designsystem.uikit.components.UanLists
import com.uan.designsystem.uikit.components.UanModal
import com.uan.designsystem.uikit.components.UanModalAction
import com.uan.designsystem.uikit.components.UanProgressIndicator
import com.uan.designsystem.uikit.components.UanProgressIndicatorVariant
import com.uan.designsystem.uikit.components.UanProgressStep
import com.uan.designsystem.uikit.components.UanProgressStepState
import com.uan.designsystem.uikit.foundation.UanTone
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val formateadorFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    .withZone(ZoneId.systemDefault())

@Composable
fun PantallaMonitoreoPrincipal(
    viewModel: MonitoreoViewModel,
    modifier: Modifier = Modifier,
) {
    val estadoUi by viewModel.estadoUi.collectAsState()
    val estadoSnackbar = remember { SnackbarHostState() }
    val contexto = LocalContext.current

    LaunchedEffect(estadoUi.mensajeNotificacionSnackbar) {
        val mensaje = estadoUi.mensajeNotificacionSnackbar ?: return@LaunchedEffect
        val resultado = estadoSnackbar.showSnackbar(
            message = mensaje,
            actionLabel = if (mensaje.contains("ajustes", ignoreCase = true)) "Abrir ajustes" else null,
        )
        if (resultado == SnackbarResult.ActionPerformed) {
            viewModel.abrirAjustesAplicacion(contexto)
        }
        viewModel.procesarEvento(EventoUiMonitoreo.OcultarSnackbar)
    }

    if (estadoUi.mostrarDialogoOptimizacionBateria) {
        UanModal(
            visible = true,
            onDismissRequest = { viewModel.procesarEvento(EventoUiMonitoreo.OcultarDialogoBateria) },
            title = stringResource(R.string.battery_opt_title),
            body = stringResource(R.string.battery_opt_body),
            primaryAction = UanModalAction(
                label = stringResource(R.string.battery_opt_configure),
                onClick = { viewModel.procesarEvento(EventoUiMonitoreo.ConfirmarExencionBateria) },
            ),
            secondaryAction = UanModalAction(
                label = stringResource(R.string.battery_opt_later),
                onClick = { viewModel.procesarEvento(EventoUiMonitoreo.OcultarDialogoBateria) },
            ),
            tone = UanTone.Warning,
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(estadoSnackbar) },
        topBar = {
            UanAppBar(
                title = stringResource(R.string.app_name),
                subtitle = obtenerSubtituloConexion(estadoUi.estadoConexion),
            )
        },
    ) { rellenoInterno ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(rellenoInterno),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TarjetaEstadoConexion(estadoUi.estadoConexion)
            }

            item {
                TarjetaUltimaAlerta(estadoUi)
            }

            item {
                TarjetaBateriaManilla(estadoUi)
            }

            if (estadoUi.estadoConexion == EstadoConexionBluetooth.Fallido) {
                item {
                    UanButton(
                        onClick = { viewModel.procesarEvento(EventoUiMonitoreo.ReintentarConexion) },
                        modifier = Modifier.fillMaxWidth(),
                        style = UanButtonStyle.Primary,
                        size = UanButtonSize.Regular,
                    ) {
                        Text(stringResource(R.string.btn_retry_connection))
                    }
                }
            }

            // TODO: Eliminar este botón y la lógica de simulación antes de producción
            if (BuildConfig.DEBUG) {
                item {
                    UanButton(
                        onClick = { viewModel.procesarEvento(EventoUiMonitoreo.SimularAlertaPrueba) },
                        modifier = Modifier.fillMaxWidth(),
                        style = UanButtonStyle.Secondary,
                        size = UanButtonSize.Regular,
                    ) {
                        Text(stringResource(R.string.btn_simulate_alert))
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.alert_history_title),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (estadoUi.historialAlertas.isEmpty()) {
                item {
                    UanCard(
                        title = stringResource(R.string.alert_history_empty_title),
                        body = stringResource(R.string.alert_history_empty_body),
                    )
                }
            } else {
                items(estadoUi.historialAlertas, key = { it.id }) { lectura ->
                    UanLists(
                        title = "SpO2 ${lectura.spo2}% · BPM ${lectura.bpm}",
                        supportingText = formateadorFechaHora.format(lectura.fechaRecepcion),
                        itemDescription = "Alerta del ${formateadorFechaHora.format(lectura.fechaRecepcion)}",
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaEstadoConexion(estado: EstadoConexionBluetooth) {
    val (etiquetaRes, tono) = when (estado) {
        EstadoConexionBluetooth.Conectado -> R.string.status_connected to UanTone.Success
        EstadoConexionBluetooth.Escaneando -> R.string.status_scanning to UanTone.Info
        EstadoConexionBluetooth.Conectando -> R.string.status_connecting to UanTone.Info
        EstadoConexionBluetooth.Fallido -> R.string.status_disconnected to UanTone.Danger
        EstadoConexionBluetooth.Inactivo -> R.string.status_idle to UanTone.Neutral
    }

    val etiqueta = stringResource(etiquetaRes)

    UanCard(
        title = stringResource(R.string.card_ble_title),
        body = stringResource(R.string.card_ble_body),
        tone = tono,
        supportingContent = {
            UanBadge(
                text = etiqueta,
                tone = tono,
                emphasis = UanBadgeEmphasis.Tonal,
                contentDescription = "Estado de conexión: $etiqueta",
            )
        },
    )
}

@Composable
private fun TarjetaUltimaAlerta(estadoUi: EstadoUiMonitoreo) {
    val lectura = estadoUi.ultimaLecturaRecibida
    UanCard(
        title = stringResource(R.string.card_alert_title),
        body = if (lectura != null) {
            formateadorFechaHora.format(lectura.fechaRecepcion)
        } else {
            stringResource(R.string.card_alert_empty)
        },
        supportingContent = if (lectura != null) {
            {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "SpO2: ${lectura.spo2}%")
                    Text(text = "BPM: ${lectura.bpm}")
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun TarjetaBateriaManilla(estadoUi: EstadoUiMonitoreo) {
    val porcentaje = estadoUi.informacionBateria.porcentajeBateria

    val tituloTexto = stringResource(R.string.card_battery_title)
    val vacioTexto = stringResource(R.string.card_battery_empty)
    val bajaTexto = stringResource(R.string.card_battery_low)

    UanCard(
        title = tituloTexto,
        body = if (porcentaje != null) "$porcentaje%" else vacioTexto,
        tone = if (estadoUi.informacionBateria.esBateriaBaja) UanTone.Warning else UanTone.Neutral,
        supportingContent = {
            if (porcentaje != null) {
                val estadoPaso = when {
                    estadoUi.informacionBateria.esBateriaBaja -> UanProgressStepState.Warning
                    porcentaje >= 50 -> UanProgressStepState.Completed
                    else -> UanProgressStepState.Current
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UanProgressIndicator(
                        steps = listOf(
                            UanProgressStep(
                                label = "$porcentaje%",
                                state = estadoPaso,
                                contentDescription = "Batería al $porcentaje por ciento",
                            ),
                        ),
                        variant = UanProgressIndicatorVariant.Status,
                        modifier = Modifier.fillMaxWidth(),
                        currentTone = if (estadoUi.informacionBateria.esBateriaBaja) UanTone.Warning else UanTone.Primary,
                    )
                    if (estadoUi.informacionBateria.esBateriaBaja) {
                        UanBadge(
                            text = bajaTexto,
                            tone = UanTone.Warning,
                            emphasis = UanBadgeEmphasis.Tonal,
                            contentDescription = bajaTexto,
                        )
                    }
                }
            } else {
                UanBadge(
                    text = vacioTexto,
                    tone = UanTone.Neutral,
                    emphasis = UanBadgeEmphasis.Tonal,
                    contentDescription = vacioTexto,
                )
            }
        },
    )
}

@Composable
private fun obtenerSubtituloConexion(estado: EstadoConexionBluetooth): String {
    return when (estado) {
        EstadoConexionBluetooth.Conectado -> stringResource(R.string.subtitle_connected)
        EstadoConexionBluetooth.Escaneando -> stringResource(R.string.subtitle_scanning)
        EstadoConexionBluetooth.Conectando -> stringResource(R.string.subtitle_connecting)
        EstadoConexionBluetooth.Fallido -> stringResource(R.string.subtitle_disconnected)
        EstadoConexionBluetooth.Inactivo -> stringResource(R.string.subtitle_idle)
    }
}
