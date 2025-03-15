package com.example.wearoslux.presentation

import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import android.util.Log

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val lightLevel = mutableStateOf("초기화 중...") // 조도 데이터 상태 관리
    private val sensorDataList = JSONArray() // JSON 데이터 리스트

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        // SensorManager 초기화
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

        // 센서 지원 여부 확인
        if (lightSensor == null) {
            lightLevel.value = "조도 센서를 지원하지 않습니다."
        }

        setContent {
            WearApp(lightLevel.value)
        }
    }

    override fun onResume() {
        super.onResume()
        lightSensor?.also {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lightValue = event.values[0]
            lifecycleScope.launch {
                lightLevel.value = "조도 레벨: $lightValue lx"
            }

            // JSON 데이터 생성
            val sensorData = JSONObject().apply {
                put("timestamp", System.currentTimeMillis())
                put("sensor_name", "Light Sensor")
                put("lux", lightValue) // 조도 값 (Lux)
            }

            // JSON 배열에 추가
            sensorDataList.put(sensorData)

            // JSON 파일로 저장
            saveJsonToFile(sensorDataList)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveJsonToFile(jsonArray: JSONArray) {
        val fileName = "light_sensor_data.json"
        val file = File(getExternalFilesDir(null), fileName) // 내부 저장소 대신 외부 저장소 사용

        try {
            FileWriter(file).use { writer ->
                writer.write(jsonArray.toString(4))
            }
            Log.d("SensorData", "JSON 파일 저장 완료: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("FileError", "JSON 저장 실패: ${e.message}")
        }
    }
}