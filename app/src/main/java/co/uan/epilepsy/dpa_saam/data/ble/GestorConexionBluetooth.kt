package co.uan.epilepsy.dpa_saam.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import co.uan.epilepsy.dpa_saam.core.result.ResultadoOperacionApp
import co.uan.epilepsy.dpa_saam.domain.model.EstadoConexionBluetooth
import co.uan.epilepsy.dpa_saam.domain.model.InformacionBateria
import co.uan.epilepsy.dpa_saam.domain.model.LecturaManilla
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class GestorConexionBluetooth(
    private val contexto: Context,
    private val analizadorTrama: AnalizadorTramaBluetooth,
) {

    private val manejadorHiloPrincipal = Handler(Looper.getMainLooper())
    private val administradorBluetooth =
        contexto.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adaptadorBluetooth: BluetoothAdapter? = administradorBluetooth.adapter

    private val _estadoConexion = MutableStateFlow(EstadoConexionBluetooth.Inactivo)
    val estadoConexion: StateFlow<EstadoConexionBluetooth> = _estadoConexion.asStateFlow()

    private val _flujoLecturas = MutableSharedFlow<LecturaManilla>(extraBufferCapacity = 16)
    val flujoLecturas: SharedFlow<LecturaManilla> = _flujoLecturas.asSharedFlow()

    private val _informacionBateria = MutableStateFlow(InformacionBateria(porcentajeBateria = null, esBateriaBaja = false))
    val informacionBateria: StateFlow<InformacionBateria> = _informacionBateria.asStateFlow()

    private val _flujoErroresBluetooth = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val flujoErroresBluetooth: SharedFlow<String> = _flujoErroresBluetooth.asSharedFlow()

    private var conexionGatt: BluetoothGatt? = null
    private var direccionMacConectada: String? = null
    private var estaEscaneando = false
    private var ejecutableTiempoEsperaEscaneo: Runnable? = null

    private val respuestaEscaneo = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                val dispositivo = result.device ?: return
                detenerEscaneo()
                conectarADispositivo(dispositivo)
            } catch (e: Exception) {
                emitirErrorYEstadoFallido("Error durante el escaneo BLE")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            emitirErrorYEstadoFallido("Escaneo BLE fallido (código $errorCode)")
        }
    }

    private val respuestaGatt = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    emitirErrorYEstadoFallido("Conexión GATT fallida (código $status)")
                    cerrarGatt()
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        direccionMacConectada = gatt.device.address
                        _estadoConexion.value = EstadoConexionBluetooth.Conectado
                        gatt.discoverServices()
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        direccionMacConectada = null
                        _estadoConexion.value = EstadoConexionBluetooth.Fallido
                        cerrarGatt()
                        emitirErrorYEstadoFallido("Conexión perdida. Reconectando...")
                        manejadorHiloPrincipal.postDelayed({ iniciarConexionAutomatica() }, 5000)
                    }
                }
            } catch (e: Exception) {
                emitirErrorYEstadoFallido("Error en cambio de estado GATT")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    emitirErrorYEstadoFallido("Descubrimiento de servicios fallido")
                    return
                }

                val caracteristicaAlerta = gatt
                    .getService(ConstantesBluetooth.UUID_SERVICIO)
                    ?.getCharacteristic(ConstantesBluetooth.UUID_CARACTERISTICA_ALERTA)

                if (caracteristicaAlerta == null) {
                    emitirErrorYEstadoFallido("Característica de alerta no encontrada")
                    return
                }

                gatt.setCharacteristicNotification(caracteristicaAlerta, true)
                val descriptor = caracteristicaAlerta.getDescriptor(
                    UUID.fromString(ConstantesBluetooth.UUID_CCCD),
                )
                descriptor?.let {
                    it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(it)
                }

                leerNivelBateria(gatt)
            } catch (e: Exception) {
                emitirErrorYEstadoFallido("Error al configurar notificaciones BLE")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            procesarValorCaracteristica(gatt.device.address, value)
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            @Suppress("DEPRECATION")
            procesarValorCaracteristica(gatt.device.address, characteristic.value)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                if (characteristic.uuid == ConstantesBluetooth.UUID_CARACTERISTICA_NIVEL_BATERIA && value.isNotEmpty()) {
                    actualizarBateria(value[0].toInt() and 0xFF)
                }
            } catch (_: Exception) {
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) return
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                if (characteristic.uuid == ConstantesBluetooth.UUID_CARACTERISTICA_NIVEL_BATERIA && value.isNotEmpty()) {
                    actualizarBateria(value[0].toInt() and 0xFF)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun iniciarConexionAutomatica() {
        try {
            if (_estadoConexion.value == EstadoConexionBluetooth.Conectado ||
                _estadoConexion.value == EstadoConexionBluetooth.Conectando ||
                _estadoConexion.value == EstadoConexionBluetooth.Escaneando
            ) {
                return
            }

            val adaptador = adaptadorBluetooth
            if (adaptador == null || !adaptador.isEnabled) {
                emitirErrorYEstadoFallido("Bluetooth no disponible o desactivado")
                return
            }

            _estadoConexion.value = EstadoConexionBluetooth.Escaneando
            iniciarEscaneo()
        } catch (e: Exception) {
            emitirErrorYEstadoFallido("No se pudo iniciar la conexión automática")
        }
    }

    fun reintentarConexion() {
        try {
            desconectar()
            iniciarConexionAutomatica()
        } catch (e: Exception) {
            emitirErrorYEstadoFallido("No se pudo reintentar la conexión")
        }
    }

    fun desconectar() {
        try {
            detenerEscaneo()
            conexionGatt?.disconnect()
            cerrarGatt()
            direccionMacConectada = null
            _estadoConexion.value = EstadoConexionBluetooth.Fallido
        } catch (_: Exception) {
            _estadoConexion.value = EstadoConexionBluetooth.Fallido
        }
    }

    private fun iniciarEscaneo() {
        try {
            val escaner = adaptadorBluetooth?.bluetoothLeScanner
            if (escaner == null) {
                emitirErrorYEstadoFallido("Escáner BLE no disponible")
                return
            }

            val filtros = listOf(
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(ConstantesBluetooth.UUID_SERVICIO))
                    .build(),
            )
            val configuracionEscaneo = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            estaEscaneando = true
            escaner.startScan(filtros, configuracionEscaneo, respuestaEscaneo)

            ejecutableTiempoEsperaEscaneo?.let { manejadorHiloPrincipal.removeCallbacks(it) }
            ejecutableTiempoEsperaEscaneo = Runnable {
                if (estaEscaneando) {
                    detenerEscaneo()
                    emitirErrorYEstadoFallido("No se encontró la manilla ESP32")
                }
            }.also { manejadorHiloPrincipal.postDelayed(it, ConstantesBluetooth.TIEMPO_ESPERA_ESCANEO_MS) }
        } catch (e: SecurityException) {
            emitirErrorYEstadoFallido("Permisos BLE insuficientes")
        } catch (e: Exception) {
            emitirErrorYEstadoFallido("Error al escanear dispositivos BLE")
        }
    }

    private fun detenerEscaneo() {
        try {
            ejecutableTiempoEsperaEscaneo?.let { manejadorHiloPrincipal.removeCallbacks(it) }
            ejecutableTiempoEsperaEscaneo = null
            if (estaEscaneando) {
                adaptadorBluetooth?.bluetoothLeScanner?.stopScan(respuestaEscaneo)
                estaEscaneando = false
            }
        } catch (_: Exception) {
            estaEscaneando = false
        }
    }

    private fun conectarADispositivo(dispositivo: BluetoothDevice) {
        try {
            _estadoConexion.value = EstadoConexionBluetooth.Conectando
            cerrarGatt()
            conexionGatt = dispositivo.connectGatt(contexto, false, respuestaGatt, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            emitirErrorYEstadoFallido("Permisos BLE insuficientes para conectar")
        } catch (e: Exception) {
            emitirErrorYEstadoFallido("Error al conectar con la manilla")
        }
    }

    private fun leerNivelBateria(gatt: BluetoothGatt) {
        try {
            val caracteristicaBateria = gatt
                .getService(ConstantesBluetooth.UUID_SERVICIO_BATERIA)
                ?.getCharacteristic(ConstantesBluetooth.UUID_CARACTERISTICA_NIVEL_BATERIA)
                ?: return
            gatt.readCharacteristic(caracteristicaBateria)
        } catch (_: Exception) {
        }
    }

    private fun procesarValorCaracteristica(direccionMac: String, valor: ByteArray?) {
        try {
            val textoTrama = valor?.toString(Charsets.UTF_8)?.trim().orEmpty()
            if (textoTrama.isEmpty()) return

            when (val resultado = analizadorTrama.analizarTrama(textoTrama, direccionMac)) {
                is ResultadoOperacionApp.Exito -> {
                    resultado.datos.porcentajeBateria?.let { actualizarBateria(it) }
                    _flujoLecturas.tryEmit(resultado.datos)
                }

                is ResultadoOperacionApp.Error -> {
                    _flujoErroresBluetooth.tryEmit(resultado.mensaje)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun actualizarBateria(porcentaje: Int) {
        _informacionBateria.value = InformacionBateria(
            porcentajeBateria = porcentaje,
            esBateriaBaja = porcentaje <= ConstantesBluetooth.UMBRAL_BATERIA_BAJA,
        )
    }

    private fun cerrarGatt() {
        try {
            conexionGatt?.disconnect()
            conexionGatt?.close()
        } catch (_: Exception) {
        } finally {
            conexionGatt = null
        }
    }

    private fun emitirErrorYEstadoFallido(mensaje: String) {
        detenerEscaneo()
        _flujoErroresBluetooth.tryEmit(mensaje)
        _estadoConexion.value = EstadoConexionBluetooth.Fallido
    }
}
