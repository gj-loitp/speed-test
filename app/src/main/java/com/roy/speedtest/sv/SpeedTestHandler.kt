package com.roy.speedtest.sv

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class SpeedTestHandler : Thread() {
    var mapKey = HashMap<Int, String>()
    var mapValue = HashMap<Int, List<String>>()
    var selfLat = 0.0
    var selfLon = 0.0
    var isFinished = false

    override fun run() {
        //Get latitude, longitude
        try {
            val url = URL("https://www.speedtest.net/speedtest-config.php")
            val urlConnection = url.openConnection() as HttpURLConnection
            val code = urlConnection.responseCode
            if (code == 200) {
                val br = BufferedReader(
                    InputStreamReader(
                        urlConnection.inputStream
                    )
                )
                var line: String
                while (br.readLine().also { line = it } != null) {
                    if (!line.contains("isp=")) {
                        continue
                    }
                    selfLat = line.split("lat=\"".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0].replace("\"", "").toDouble()
                    selfLon = line.split("lon=\"".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[1].split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()[0].replace("\"", "").toDouble()
                    break
                }
                br.close()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            return
        }
        var uploadAddress: String
        var name: String
        var country: String
        var cc: String
        var sponsor: String
        var lat: String
        var lon: String
        var host: String


        //Best server
        var count = 0
        try {
            val url = URL("https://www.speedtest.net/speedtest-servers-static.php")
            val urlConnection = url.openConnection() as HttpURLConnection
            val code = urlConnection.responseCode
            if (code == 200) {
                val br = BufferedReader(
                    InputStreamReader(
                        urlConnection.inputStream
                    )
                )
                var line: String
                while (br.readLine().also { line = it } != null) {
                    if (line.contains("<server url")) {
                        uploadAddress =
                            line.split("server url=\"".toRegex()).dropLastWhile { it.isEmpty() }
                                .toTypedArray()[1].split("\"".toRegex())
                                .dropLastWhile { it.isEmpty() }
                                .toTypedArray()[0]
                        lat = line.split("lat=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        lon = line.split("lon=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        name = line.split("name=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        country = line.split("country=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        cc = line.split("cc=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        sponsor = line.split("sponsor=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        host = line.split("host=\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[1].split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()[0]
                        val ls = listOf(lat, lon, name, country, cc, sponsor, host)
                        mapKey[count] = uploadAddress
                        mapValue[count] = ls
                        count++
                    }
                }
                br.close()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        isFinished = true
    }
}
