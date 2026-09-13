package co.uan.epilepsy.dpa_saam.domain.model

import java.time.Instant

data class LecturaManilla(
    val id: Long = 0,
    val spo2: Float,
    val bpm: Int,
    val porcentajeBateria: Int? = null,
    val fechaRecepcion: Instant = Instant.now(),
    val direccionMacDispositivo: String = "",
)
