package co.uan.epilepsy.dpa_saam.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DaoAlertas {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(entidad: EntidadAlerta)

    @Query("SELECT * FROM alertas ORDER BY fechaRecepcionMillis DESC")
    fun observarTodasLasAlertas(): Flow<List<EntidadAlerta>>
}
