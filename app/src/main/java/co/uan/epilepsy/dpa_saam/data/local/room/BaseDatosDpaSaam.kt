package co.uan.epilepsy.dpa_saam.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EntidadAlerta::class],
    version = 2,
    exportSchema = false,
)
abstract class BaseDatosDpaSaam : RoomDatabase() {

    abstract fun daoAlertas(): DaoAlertas

    companion object {
        @Volatile
        private var instancia: BaseDatosDpaSaam? = null

        fun obtenerInstancia(context: Context): BaseDatosDpaSaam {
            return instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDatosDpaSaam::class.java,
                    "dpa_saam.db",
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build().also { instancia = it }
            }
        }
    }
}
