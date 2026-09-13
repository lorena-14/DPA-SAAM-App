package co.uan.epilepsy.dpa_saam.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alertas")
data class EntidadAlerta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spo2: Float,
    val bpm: Int,
    val porcentajeBateria: Int?,
    val fechaRecepcionMillis: Long,
    val direccionMacDispositivo: String,
)
