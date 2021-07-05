package com.app.combined.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.app.combined.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class SaveOffline(
    val file: File,
    val cropName: String,
    val health: String,
    val context: Context,
    val s: String
) {

    var count = 0
    internal var myExternalFile: File?=null
    internal var finalName: String? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    fun saveInDevice() {

        if(s != "label") {
            if (cropName == context.getString(R.string.invalid)) {
                finalName = health.toUpperCase()
            } else if (health == context.getString(R.string.invalid)) {
                finalName = cropName.toUpperCase()
            } else {
                finalName = cropName.toUpperCase() + health.toUpperCase()
            }
        }
        else{
            if (cropName == context.getString(R.string.invalid) || cropName == null) {
                finalName = context.getString(R.string.manual)
            } else {
                finalName = cropName.toUpperCase() + context.getString(R.string.manual)
            }
        }

        if(File(context.getExternalFilesDir(finalName).toString()).exists()){
           count = context.getExternalFilesDir(finalName)?.let { getNumberOfFiles(it) }!!
        }
        count++

        myExternalFile = File(context.getExternalFilesDir(finalName), "${finalName!!.toLowerCase()}${count}.jpg")

        val fileOutputStream = FileOutputStream(myExternalFile)
        try{
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)

            if(s=="label"){
                extractTag(file, health)
            }
            fileOutputStream.close()
            file.delete()
            Toast.makeText(context, context.getString(R.string.saved_device_toast), Toast.LENGTH_SHORT).show()
        }catch (e: IOException){
            Toast.makeText(context, context.getString(R.string.could_not_save_toast)+" ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getNumberOfFiles(path: File): Int {
        var numberOfFiles = 0
        if (path.exists()) {
            val files = path.listFiles() ?: return numberOfFiles
            return files.size
        }
        return 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun extractTag(f: File, label: String){
        val inpS: InputStream?
        inpS = context!!.contentResolver.openInputStream(Uri.fromFile(f))

        try {

            val exifInterface =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ExifInterface(f)
            } else {
                TODO("VERSION.SDK_INT < N")
            }

            exifInterface.setAttribute(ExifInterface.TAG_MAKER_NOTE,label)
            exifInterface.saveAttributes()

        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.error_toast)+" ${e.message}", Toast.LENGTH_SHORT).show()
            // Handle any errors
        } finally {
            if (inpS != null) {
                try {
                    inpS.close()
                } catch (ignored: IOException) {
                }
            }
        }

    }
}