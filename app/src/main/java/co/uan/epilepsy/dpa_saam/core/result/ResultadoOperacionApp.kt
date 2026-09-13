package co.uan.epilepsy.dpa_saam.core.result

sealed class ResultadoOperacionApp<out T> {
    data class Exito<out T>(val datos: T) : ResultadoOperacionApp<T>()
    data class Error(val mensaje: String, val excepcion: Throwable? = null) : ResultadoOperacionApp<Nothing>()
}
