package co.uan.epilepsy.dpa_saam.data.ble

import co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import java.time.Instant

class AnalizadorTramaBluetooth {

    private val patronExpresionRegular = Regex(
        """SpO2:(?<spo2>\d+(?:\.\d+)?),BPM:(?<bpm>\d+)(?:,BAT:(?<bat>\d+(?:\.\d+)?))?(?:,VOL:(?<vol>\d+(?:\.\d+)?))?""",
        RegexOption.IGNORE_CASE,
    )

    fun analizarTrama(tramaTexto: String, direccionMacDispositivo: String): ResultadoOperacionApp<LecturaManilla> {
        return try {
            val textoLimpio = tramaTexto.trim()

            if (!textoLimpio.contains("SpO2", ignoreCase = true)) {
                return ResultadoOperacionApp.Error("Formato incorrecto. La trama debe contener 'SpO2:98,BPM:72', pero envió: $textoLimpio")
            }

            val coincidencia = patronExpresionRegular.matchEntire(textoLimpio)
                ?: return ResultadoOperacionApp.Error("Formato de lectura no reconocido: $textoLimpio")

            val spo2 = coincidencia.groups["spo2"]?.value?.toFloatOrNull()
                ?: return ResultadoOperacionApp.Error("SpO2 inválido en la lectura")
            val bpm = coincidencia.groups["bpm"]?.value?.toIntOrNull()
                ?: return ResultadoOperacionApp.Error("BPM inválido en la lectura")
            val porcentajeBateria = coincidencia.groups["bat"]?.value?.toFloatOrNull()?.toInt()

            ResultadoOperacionApp.Exito(
                LecturaManilla(
                    spo2 = spo2,
                    bpm = bpm,
                    porcentajeBateria = porcentajeBateria,
                    fechaRecepcion = Instant.now(),
                    direccionMacDispositivo = direccionMacDispositivo,
                ),
            )
        } catch (e: Exception) {
            ResultadoOperacionApp.Error("Error al interpretar la lectura BLE", e)
        }
    }
}
