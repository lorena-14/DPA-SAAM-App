package co.uan.epilepsy.dpa_saam.data.repository

import android.database.sqlite.SQLiteException
import co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp
import co.uan.epilepsy.dpa_saam.data.local.room.DaoAlertas
import co.uan.epilepsy.dpa_saam.data.local.room.EntidadAlerta
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import co.uan.epilepsy.dpa_saam.domain.repository.RepositorioAlertas
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RepositorioAlertasImplementacion(
    private val daoAlertas: DaoAlertas,
) : RepositorioAlertas {

    override fun observarTodasLasAlertas(): Flow<List<LecturaManilla>> {
        return daoAlertas.observarTodasLasAlertas().map { listaEntidades ->
            listaEntidades.map { it.aModeloDominio() }
        }
    }

    override suspend fun insertar(lectura: LecturaManilla): ResultadoOperacionApp<Unit> {
        return try {
            daoAlertas.insertar(lectura.aEntidadRoom())
            ResultadoOperacionApp.Exito(Unit)
        } catch (e: SQLiteException) {
            ResultadoOperacionApp.Error("No se pudo guardar la alerta", e)
        } catch (e: Exception) {
            ResultadoOperacionApp.Error("Error al guardar la alerta", e)
        }
    }

    private fun EntidadAlerta.aModeloDominio(): LecturaManilla {
        return LecturaManilla(
            id = id,
            spo2 = spo2,
            bpm = bpm,
            porcentajeBateria = porcentajeBateria,
            fechaRecepcion = Instant.ofEpochMilli(fechaRecepcionMillis),
            direccionMacDispositivo = direccionMacDispositivo,
        )
    }

    private fun LecturaManilla.aEntidadRoom(): EntidadAlerta {
        return EntidadAlerta(
            id = id,
            spo2 = spo2,
            bpm = bpm,
            porcentajeBateria = porcentajeBateria,
            fechaRecepcionMillis = fechaRecepcion.toEpochMilli(),
            direccionMacDispositivo = direccionMacDispositivo,
        )
    }
}
