package com.app.combined.mlmodel

import android.content.Context
import android.graphics.Bitmap
import com.app.combined.R
import com.app.combined.ml.Potatodiseasedetection
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DiseaseDetection(var bitmap: Bitmap, val context: Context, val list: List<String>) {

    lateinit var output: ByteBuffer

    fun predictName(): String {

        val model = Potatodiseasedetection.newInstance(context)

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 300, 300, 3), DataType.FLOAT32)

        val resized = Bitmap.createScaledBitmap(bitmap, 300, 300, false)

        output = convertBitmapToByteBuffer(resized)

        inputFeature0.loadBuffer(output)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        var max = getMax(outputFeature0.floatArray)

        if(max == -1)
            return context.getString(R.string.invalid)

        else
            return list[max]

        // Releases model resources if no longer used.
        model.close()
    }

    private fun convertBitmapToByteBuffer(bmp: Bitmap): ByteBuffer {

        // Specify the size of the byteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(300 * 300 * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        // Calculate the number of pixels in the image
        val pixels = IntArray(300 * 300)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        var pixel = 0
        // Loop through all the pixels and save them into the buffer
        for (i in 0 until 300) {
            for (j in 0 until 300) {
                val pixelVal = pixels[pixel++]
                // Do note that the method to add pixels to byteBuffer is different for quantized models over normal tflite models
                /* byteBuffer.put((pixelVal shr 16 and 0xFF).toByte())
                 byteBuffer.put((pixelVal shr 8 and 0xFF).toByte())
                 byteBuffer.put((pixelVal and 0xFF).toByte())*/

                byteBuffer.putFloat((pixelVal shr 16 and 0xFF) / 255f)
                byteBuffer.putFloat((pixelVal shr 8 and 0xFF) / 255f)
                byteBuffer.putFloat((pixelVal and 0xFF) / 255f)
            }
        }

        // Recycle the bitmap to save memory
        bmp.recycle()
        return byteBuffer
    }

    private fun getMax(arr: FloatArray): Int {
        var ind = -1
        var min = 0.0f

        for (i in 0..1) {

            if (arr[i] > min && arr[i] > 0.99999) { // can be changed

                ind = i
                min = arr[i]

            }

        }
        return ind
    }

}