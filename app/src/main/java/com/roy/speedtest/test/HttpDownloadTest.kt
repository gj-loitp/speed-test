package com.roy.speedtest.test

import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocketFactory

class HttpDownloadTest(private var fileURL: String) : Thread() {
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var downloadElapsedTime = 0.0
    private var downloadedByte = 0
    private var finalDownloadRate = 0.0
    var isFinished = false
    var instantDownloadRate = 0.0
    private var timeout = 8
    private var httpsConn: HttpsURLConnection? = null

    private fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd: BigDecimal = try {
            BigDecimal(value)
        } catch (ex: Exception) {
            return 0.0
        }
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    private fun setInstantDownloadRate(
        downloadedByte: Int,
        elapsedTime: Double
    ) {
        instantDownloadRate = if (downloadedByte >= 0) {
            round((downloadedByte * 8 / (1000 * 1000) / elapsedTime), 2)
        } else {
            0.0
        }
    }

    fun getFinalDownloadRate(): Double {
        return round(finalDownloadRate, 2)
    }

    override fun run() {
        var url: URL
        downloadedByte = 0
        var responseCode: Int? = null
        val fileUrls: MutableList<String> = ArrayList()
        fileUrls.add(fileURL + "random4000x4000.jpg")
        fileUrls.add(fileURL + "random3000x3000.jpg")
        startTime = System.currentTimeMillis()
        outer@ for (link in fileUrls) {
            try {
                url = URL(link)
                httpsConn = url.openConnection() as HttpsURLConnection?

                httpsConn?.let { c ->
                    c.sslSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    c.hostnameVerifier = HostnameVerifier { _: String?, _: SSLSession? -> true }
                    c.connect()
                    responseCode = c.responseCode
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                break
            }
            try {
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val buffer = ByteArray(10240)
                    val inputStream = httpsConn?.inputStream
                    var len: Int

                    inputStream?.let { input ->
                        while (input.read(buffer).also {
                                len = it
                            } != -1) {
                            downloadedByte += len
                            endTime = System.currentTimeMillis()
                            downloadElapsedTime = (endTime - startTime) / 1000.0
                            setInstantDownloadRate(downloadedByte, downloadElapsedTime)
                            if (downloadElapsedTime >= timeout) {
                                break
                            }
                        }
                        input.close()
                    }
                    httpsConn?.disconnect()
                } else {
                    println("Link not found...")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        endTime = System.currentTimeMillis()
        downloadElapsedTime = (endTime - startTime) / 1000.0
        finalDownloadRate = downloadedByte * 8 / (1000 * 1000.0) / downloadElapsedTime
        isFinished = true
    }
}
