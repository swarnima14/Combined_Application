package com.app.combined

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

class UploadRequestBody( private val file: File, private val contentType: String, private val callback: UploadCallback): RequestBody(){

    interface UploadCallback{
        fun onProgressUpdate(percentage: Int)
    }

    override fun contentType(): MediaType? = MediaType.parse("$contentType/")
    override fun contentLength(): Long = file.length()

    override fun writeTo(sink: BufferedSink) {
        val length = file.length()
        val buffer = ByteArray(1024)
        val fileInputStream = FileInputStream(file)
        var uploaded = 0L

        fileInputStream.use { fileIS ->
            var read: Int
            val handler = Handler(Looper.getMainLooper())

            while (fileIS.read(buffer).also { read = it } != -1){
                handler.post(ProgressUpdate(uploaded, length))
                uploaded += read
                sink.write(buffer, 0, read)
            }
        }
    }

    inner class ProgressUpdate(private val uploaded: Long, private var total: Long): Runnable{
        override fun run() {
            callback.onProgressUpdate((100 * uploaded/total).toInt())
        }

    }
}