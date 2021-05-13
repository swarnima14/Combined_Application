package com.app.combined.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
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
            if (cropName == "Invalid") {
                finalName = health.toUpperCase()
            } else if (health == "Invalid") {
                finalName = cropName.toUpperCase()
            } else {
                finalName = cropName.toUpperCase() + health.toUpperCase()
            }
        }
        else{
            if (cropName == "Invalid" || cropName == null) {
                finalName = "MANUAL"
            } else {
                finalName = cropName.toUpperCase() + "MANUAL"
            }
        }

        if(File(context.getExternalFilesDir(finalName).toString()).exists()){
           count = context.getExternalFilesDir(finalName)?.let { getNumberOfFiles(it) }!!
        }
        count++

        myExternalFile = File(context.getExternalFilesDir(finalName), "${finalName!!.toLowerCase()}${count}.jpg")

        val fileOutputStream = FileOutputStream(myExternalFile)
        try{
            var bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            if(s=="label"){
                extractTag(file, health)
            }
            fileOutputStream.close()
            file.delete()
            Toast.makeText(context, "Saved in your device.", Toast.LENGTH_SHORT).show()
        }catch (e: IOException){
            Toast.makeText(context, "Could not be saved: "+e.message, Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, "Error: "+e.message, Toast.LENGTH_SHORT).show()
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