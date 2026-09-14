package co.uan.epilepsy.dpa_saam.data.ble

import co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import java.time.Instant

class AnalizadorTramaBluetooth {

    // Regex para soportar "SpO2:98,BPM:72,BAT:100.0,VOL:4.10"
    private val patronConLlaves = Regex(
        """SpO2:(?<spo2>\d+(?:\.\d+)?),BPM:(?<bpm>\d+)(?:,BAT:(?<bat>\d+(?:\.\d+)?))?(?:,VOL:(?<vol>\d+(?:\.\d+)?))?""",
        RegexOption.IGNORE_CASE,
    )

    // Regex alternativa para soportar "78,166,100.0,0.00"
    private val patronSinLlaves = Regex(
        """(?<spo2>\d+(?:\.\d+)?),(?<bpm>\d+)(?:,(?<bat>\d+(?:\.\d+)?))?(?:,(?<vol>\d+(?:\.\d+)?))?""",
    )

    fun analizarTrama(tramaTexto: String, direccionMacDispositivo: String): ResultadoOperacionApp<LecturaManilla> {
        return try {
            val textoLimpio = tramaTexto.trim()

            // Intenta primero con el patrón oficial (el que tiene "SpO2:...")
            var coincidencia = patronConLlaves.matchEntire(textoLimpio)
            
            // Si falla, intenta con el patrón alternativo (solo comas)
            if (coincidencia == null) {
                coincidencia = patronSinLlaves.matchEntire(textoLimpio)
            }

            // Si ambos fallan, es un formato completamente desconocido
            if (coincidencia == null) {
                return ResultadoOperacionApp.Error("Formato de lectura no reconocido: $textoLimpio")
            }

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
