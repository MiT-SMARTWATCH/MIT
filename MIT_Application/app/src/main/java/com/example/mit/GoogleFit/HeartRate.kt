package com.example.mit.GoogleFit

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mit.R
import com.example.mit.mainhealthcare.Health_main
import com.example.mit.mainhealthcare.Health_settings
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.material.snackbar.Snackbar
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit


const val tag = "Heart Rate"

class HeartRate : AppCompatActivity() {

    private val PERMISSION_REQUEST_CODE = 100 //κΆν λ³μ

    private val fitnessOptions = FitnessOptions.builder()
        .accessActivitySessions(FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_SPEED, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_LOCATION_SAMPLE, FitnessOptions.ACCESS_READ)
        .build()

    private val runningQOrLater =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_heart_rate)

        val button : Button = findViewById(R.id.button3)


        //κΆνμ΄ μλμ§ νμΈ
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) { //κΆνμμ
            //κΆν μμ²­ μ½λ
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), PERMISSION_REQUEST_CODE)
        } else {

            button.setOnClickListener {
                val sleepSec = 63

                // μκ° μΆλ ₯ ν¬λ§·
                val fmt = SimpleDateFormat("HH:mm:ss")
                // μ£ΌκΈ°μ μΈ μμμ μν
                val exec = ScheduledThreadPoolExecutor(1)
                exec.scheduleAtFixedRate({
                    try {
                        val cal = Calendar.getInstance()
                        // μ½μμ νμ¬ μκ° μΆλ ₯
                        println("νμ¬μκ° : " + fmt.format(cal.time))
                        fitSignIn(FitActionRequestCode.READ_DATA)
                        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

                    } catch (e: Exception) {
                        e.printStackTrace()
                        // μλ¬ λ°μμ Executorλ₯Ό μ€μ§μν¨λ€
                        exec.shutdown()
                    }
                }, 0, sleepSec.toLong(), TimeUnit.SECONDS)
            }

            val sleepSec = 65

            // μκ° μΆλ ₯ ν¬λ§·
            val fmt = SimpleDateFormat("HH:mm:ss")
            // μ£ΌκΈ°μ μΈ μμμ μν
            val exec = ScheduledThreadPoolExecutor(1)
            exec.scheduleAtFixedRate({
                try {
                    val cal = Calendar.getInstance()
                    // μ½μμ νμ¬ μκ° μΆλ ₯
                    println(fmt.format(cal.time))
                    fitSignIn(FitActionRequestCode.READ_DATA)
                    checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

                } catch (e: Exception) {
                    e.printStackTrace()
                    // μλ¬ λ°μμ Executorλ₯Ό μ€μ§μν¨λ€
                    exec.shutdown()
                }
            }, 0, sleepSec.toLong(), TimeUnit.SECONDS)


        }
    }
    /** κΆν νμ©
     * if ~ : νμ© νμ μ μ€νλλ μ½λ
     * else ~ - κ±°λΆ : alertDialog ν΄λ¦­ μ κΈ°λ³Έμ€μ  μ΄λ -> κΆν νμ© -> μ΄ν μ μμ€ν
     */

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    fitSignIn(FitActionRequestCode.READ_DATA)
                    checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)
                } else {
                    // νλλΌλ κ±°λΆνλ€λ©΄.
                    val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
                    alertDialog.setTitle("μ± κΆν")
                    alertDialog.setMessage("ν΄λΉ μ±μ μν ν κΈ°λ₯μ μ΄μ©νμλ €λ©΄ μ νλ¦¬μΌμ΄μ μ λ³΄>κΆν> μμ λͺ¨λ  κΆνμ νμ©ν΄ μ£Όμ­μμ€")
                    // κΆνμ€μ  ν΄λ¦­μ μ΄λ²€νΈ λ°μ
                    alertDialog.setPositiveButton("κΆνμ€μ ") { dialog, _ ->
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + applicationContext.packageName))

                        fitSignIn(FitActionRequestCode.READ_DATA)
                        checkPermissionsAndRun(FitActionRequestCode.SUBSCRIBE)

                        startActivity(intent)
                        dialog.cancel()
                    }
                    //μ·¨μ
                    alertDialog.setNegativeButton("μ·¨μ") { dialog, _ -> dialog.cancel() }
                    alertDialog.show()
                }
                return
            }
        }
    }

    private fun checkPermissionsAndRun(fitActionRequestCode: FitActionRequestCode) {
        if (permissionApproved()) { fitSignIn(fitActionRequestCode) }
        else { requestRuntimePermissions(fitActionRequestCode) }
    }

    /** κ΅¬κΈ νΌνΈλμ€ λ‘κ·ΈμΈ νμΈ
     * μ¬μ©μκ° λ‘κ·ΈμΈνλμ§ νμΈνκ³ , λ‘κ·ΈμΈλ κ²½μ° μ§μ λ κΈ°λ₯μ μ€νν©λλ€.
     * μ¬μ©μκ° λ‘κ·ΈμΈνμ§ μμ κ²½μ°, λ‘κ·ΈμΈ ν ν¨μλ₯Ό μ§μ νμ¬ λ‘κ·ΈμΈμ μμν©λλ€.
     */

    private fun fitSignIn(requestCode: FitActionRequestCode) {
        if (oAuthPermissionsApproved()) {
            performActionForRequestCode(requestCode)
        } else { requestCode.let { GoogleSignIn.requestPermissions(this, requestCode.ordinal, getGoogleAccount(), fitnessOptions) }
        }
    }

    /** λ‘κ·ΈμΈμ΄ λμμ κ²½μ°
     *  λ‘κ·ΈμΈ μ½λ°±μ μ²λ¦¬
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            RESULT_OK -> {
                val postSignInAction = FitActionRequestCode.values()[requestCode]
                performActionForRequestCode(postSignInAction)
            }else -> oAuthErrorMsg(requestCode, resultCode)
        }
    }

    /** κ΅¬κΈνΌνΈλμ€ λ‘κ·ΈμΈ μ λ¬λκ³  μ±κ³΅ μ½λ°±κ³Ό ν¨κ» λ°νλ©λλ€.
     * μ΄λ₯Ό ν΅ν΄ νΈμΆμλ λ‘κ·ΈμΈ λ°©λ²μ μ§μ ν  μ μμ΅λλ€.
     */

    private fun performActionForRequestCode(requestCode: FitActionRequestCode) =
        when (requestCode) {
            FitActionRequestCode.READ_DATA -> readData()
            FitActionRequestCode.SUBSCRIBE -> subscribe()
        }

    private fun oAuthErrorMsg(requestCode: Int, resultCode: Int) {
        val message = """
            There was an error signing into Fit. Check the troubleshooting section of the README
            for potential issues.
            Request code was: $requestCode
            Result code was: $resultCode
        """.trimIndent()
        Log.e(TAG, message)
    }

    private fun oAuthPermissionsApproved() = GoogleSignIn.hasPermissions(getGoogleAccount(), fitnessOptions)

    private fun getGoogleAccount() = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

    /** λ°μ΄ν° λ±λ‘ μμ²­ λ° μ¬λ°μ λ°μ΄ν° κΈ°λ‘*/
    private fun subscribe() {
        Fitness.getRecordingClient(this, getGoogleAccount())
            .subscribe(DataType.TYPE_HEART_RATE_BPM)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { Log.i(tag, "Successfully subscribed!") }
                else { Log.w(tag, "There was a problem subscribing.", task.exception) }
            }
    }

    /** μ¬λ° μ  λ°μ΄ν° κ° λΆλ¬μ€λ ν¨μ
     * νμ¬ μκ°μ 6μκ° μ λΆν° νμ¬κΉμ§μ μ¬λ° μλ₯Ό μ½μ΄μ΅λλ€
     * (μκ°μ μ€μ μ λ°λΌ μμ΄λ  μ μμ)
     */

    private fun readData() {

        val cal: Calendar = Calendar.getInstance()
        val now = Date()
        cal.time = now
//        cal.add(Calendar.DATE, -32)
//        cal.add(Calendar.HOUR, -5)
        val endTime: Long = cal.timeInMillis
        cal.add(Calendar.HOUR, -6)
        val startTime: Long = cal.timeInMillis

        val data_list = mutableListOf<String>()
        val heart_list =  mutableListOf<Int>()

        val dateFormat: DateFormat = getDateInstance()
        Log.d(tag, "---------------------------------")
        Log.d(tag, "Range Start: " + dateFormat.format(startTime))
        Log.d(tag, "Range End: " + dateFormat.format(endTime))
        Log.d(tag, "---------------------------------")

        val readRequest = DataReadRequest.Builder()
            .read(DataType.TYPE_HEART_RATE_BPM)
            .enableServerQueries()
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
            .readData(readRequest)
            .addOnSuccessListener { dataReadResult ->
                if (dataReadResult.dataSets.size > 0) {

                    for (dataSet in dataReadResult.dataSets) {
                        //Log.d(tag, "Data returned for Data type: " + dataSet.dataType.name)
                        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                        for (dataPoint in dataSet.dataPoints) {

                            for (field in dataPoint.dataType.fields) {
                                val mLastHeartBPM = dataPoint.getValue(field).asFloat().toInt()

                                data_list.add(dateFormat.format(dataPoint.getStartTime(TimeUnit.MILLISECONDS)).toString())
                                heart_list.add(mLastHeartBPM)
                            }
                        }
                    }
                }

                val avg : TextView = this.findViewById(R.id.textView16)
                val min_max : TextView = this.findViewById(R.id.textView20)
                println(heart_list)
                println(data_list)

                //MQTTμ μ¬λ°μμ κ°μ₯ μ΅κ·Ό κ°μ μκ°κ³Ό μ¬λ° μ κ°μ μ μ‘ν©λλ€.

                connect(this, heart_list[heart_list.size - 1], data_list[heart_list.size - 1],"ID")

                val heartdata = mutableListOf<Int>()


                /** μ¬λ° μ νλ©΄ μΆλ ₯ λ°©μ
                 * 1. heart_list 7κ° μ΄ν μ μ€λ₯κ° μκ²¨ λλ μ£Όμμ΅λλ€.
                 * 2. listκ°μ νκ· , μ΅λ, μ΅μ λ₯Ό λ§λ€μ΄ μΆλ ₯μμΌμ€λλ€.
                 */

                if (heart_list.size > 7) {
                    val reverse = heart_list.reversed()
                    for (i in 0..6 ) { heartdata.add(reverse[i]) }
                    val list_sorted = heartdata.sorted()
                    val list_size = heartdata.sorted().size
                    val min = list_sorted[0]
                    val max = list_sorted[list_size - 1]
                    var sum = 0
                    for(i in heartdata) sum += i
                    val list_avg = sum / list_size
                    avg.text = "νκ·  μ¬λ°μ : $list_avg"
                    min_max.text = "μ΅μ μ¬λ°μ : $min   |   μ΅λ μ¬λ°μ : $max"
                } else if (heart_list.size > 0){
                    val reverse = heart_list.reversed()
                    for (i in heart_list.indices ) { heartdata.add(reverse[i]) }
                    val list_sorted = heartdata.sorted()
                    val list_size = heartdata.sorted().size
                    val min = list_sorted[0]
                    val max = list_sorted[list_size - 1]
                    var sum = 0
                    for(i in heartdata) sum += i
                    val list_avg = sum / list_size
                    avg.text = "νκ·  μ¬λ°μ : $list_avg"
                    min_max.text = "μ΅μ μ¬λ°μ : $min   |   μ΅λ μ¬λ°μ : $max"
                }

                val lineChart : LineChart = findViewById(R.id.Chart1)
                val heart = ArrayList<Entry>()

                /** κ·Έλν
                 * heartdataμλ 7κ°μ κ°μ΄ λ€μ΄μμ΅λλ€.
                 * μ­μμΌλ‘ λ¦¬μ€νΈλ₯Ό λ°κΏ μ€ ν κ·Έλνλ‘ μΆλ ₯ μμΌμ€λλ€.
                 * (λ€μμλΆν° κ°μ reversedν΄μ£ΌμκΈ°μ κ·Έλνλ₯Ό κ·Έλ¦¬κΈ° μν΄μ  λ€μ reversed ν΄μ£Όμλλ€.
                 */

                for (i in heartdata.indices) {
                    val reverse_heartdata = heartdata.reversed()
                    heart.add(Entry(i.toFloat(), reverse_heartdata[i].toFloat()))
                }

                val lineDataSet = LineDataSet(heart, "μ€λμ μ¬λ° μ κ·Έλν")

                lineDataSet.setColors(*ColorTemplate.MATERIAL_COLORS)
                lineDataSet.valueTextColor = Color.BLACK
                lineDataSet.valueTextSize = 10f

                val lineData = LineData(lineDataSet)

                lineChart.data = lineData
                lineChart.invalidate()
                lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
                //barChart.description.text = "Bar Chart Example"
                lineChart.animateY(200)


            }
    }

    private fun connect(context: Context, total: Int, times : String, ID :String) {

        val topic = "topic μ΄λ¦"
        val mqttAndroidClient = MqttAndroidClient(context, "tcp://" + "ipμ£Όμ" + ":1883", MqttClient.generateClientId())

        try {
            val options = MqttConnectOptions()
            options.userName = "token"
            mqttAndroidClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    Log.d(TAG, "Connection success")
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd-kk-mm")
                        val Timestamp = sdf.parse(times).time
                        val msg = "{\"ts\":$Timestamp,\"values\":{\"heart_rate\":$total}}"
                        val message = MqttMessage()
                        message.payload = msg.toByteArray()
                        mqttAndroidClient.publish("$topic", message.payload, 0, false)
                        Log.d(TAG, "λ³΄λΈ κ° : $msg")
                        //mqttAndroidClient.subscribe("$topic", 0) //μ°κ²°μ μ±κ³΅νλ©΄ jmlee λΌλ ν ν½μΌλ‘ subscribeν¨
                    } catch (e: MqttException) {
                        e.printStackTrace()
                        Log.d(TAG, "Connection Fail : $e")
                    }
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {   //μ°κ²°μ μ€ν¨νκ²½μ°
                    Log.e("connect_fail", "Failure $exception")
                }
            })
        } catch (e: MqttException) {
            e.printStackTrace()
            Log.e("connect_fail", "Failure $e")

        }

        mqttAndroidClient.setCallback(object : MqttCallback {
            //ν΄λΌμ΄μΈνΈμ μ½λ°±μ μ²λ¦¬νλλΆλΆ
            override fun connectionLost(cause: Throwable) {}

            @Throws(Exception::class)
            override fun messageArrived(
                topic: String,
                message: MqttMessage
            ) {    //λͺ¨λ  λ©μμ§κ° μ¬λ Callback method
                if (topic == "$topic") {     //topic λ³λ‘ λΆκΈ°μ²λ¦¬νμ¬ μμμ μνν μλμμ
                    val msg = String(message.payload)
                    Log.e("arrive message : ", msg)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken) {}

        })
//        try {
//            mqttAndroidClient.publish("$topic", "$total".toByteArray(), 0, false)
//
//        } catch (e: MqttException) {
//            e.printStackTrace()
//        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main2, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_logout) {
            val logout_intent = Intent(this, Health_main::class.java)
            Toast.makeText(this, "λ‘κ·Έμμ λμμ΅λλ€.", Toast.LENGTH_SHORT)
            startActivity(logout_intent)
        }
        if (id == R.id.action_settings){
            val settings = Intent(this, Health_settings::class.java)
            startActivity(settings)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun permissionApproved(): Boolean {
        return if (runningQOrLater) { PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) }
        else { true }
    }

    /** νμ© κΆν κ΄λ ¨ ν¨μ
     * μ΄μ μ μμ²­μ κ±°λΆνμ§λ§ "λ€μ λ¬»μ§ μμ" νμΈλμ μ ννμ§ μμ κ²½μ° μ΄ λ¬Έμ κ° λ°μν©λλ€.
     */
    private fun requestRuntimePermissions(requestCode: FitActionRequestCode) {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.BODY_SENSORS)
        requestCode.let {
            if (shouldProvideRationale) {
                Log.i(tag, "Displaying permission rationale to provide additional context.")
                Snackbar.make(
                    findViewById(R.id.main_activity_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.ok) { ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), requestCode.ordinal) }
                    .show()
            } else {
                Log.i(tag, "Requesting permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BODY_SENSORS), requestCode.ordinal)
            }
        }
    }
}
