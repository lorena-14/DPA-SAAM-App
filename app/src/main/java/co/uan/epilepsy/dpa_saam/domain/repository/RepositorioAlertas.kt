package co.uan.epilepsy.dpa_saam.domain.repository

import co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import kotlinx.coroutines.flow.Flow

interface RepositorioAlertas {
    fun observarTodasLasAlertas(): Flow<List<LecturaManilla>>
    suspend fun insertar(lectura: LecturaManilla): ResultadoOperacionApp<Unit>
}
