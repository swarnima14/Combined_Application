package com.app.combined

import android.content.Context
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import com.app.combined.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Classify(var bitmap: Bitmap, val context: Context, val cropList: List<String>) {


    lateinit var output: ByteBuffer

    fun predictName(): String {
        val model = Model.newInstance(context)

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 200, 200, 1), DataType.FLOAT32)

        var resized = Bitmap.createScaledBitmap(bitmap, 200, 200, false)

        output = convertBitmapToByteBuffer(resized)

        inputFeature0.loadBuffer(output)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        var max = getMax(outputFeature0.floatArray)

        // Releases model resources if no longer used.
        model.close()

        if(max == -1)
            return "Invalid"

        else
            return cropList[max]
    }

    private fun getMax(arr: FloatArray): Int {
        var ind = -1
        var min = 0.0f

        for (i in 0..3) {

            if (arr[i] > min && arr[i] > 0.7) { // can be changed

                ind = i
                min = arr[i]

            }

        }
        return ind
    }

    private fun convertBitmapToByteBuffer(bmp: Bitmap?): ByteBuffer {
        // Specify the size of the byteBuffer
        val byteBuffer = ByteBuffer.allocateDirect(200 * 200 * 1 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        // Calculate the number of pixels in the image
        val pixels = IntArray(200 * 200)
        bmp!!.getPixels(pixels, 0, bmp!!.width, 0, 0, bmp.width, bmp.height)
        var pixel = 0
        // Loop through all the pixels and save them into the buffer
        for (i in 0 until 200) {
            for (j in 0 until 200) {
                val pixelVal = pixels[pixel++]

                byteBuffer.putFloat((pixelVal and 0xFF).toFloat())
            }
        }

        // Recycle the bitmap to save memory
        bmp.recycle()
        return byteBuffer
    }
}