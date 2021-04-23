package com.app.combined.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SaveOffline(val file:File, val cropName: String, val health: String, val context: Context) {

    var count = 0
    internal var myExternalFile: File?=null
    internal var finalName: String? = null

    fun saveInDevice() {

        if(cropName == "Invalid"){
            finalName = health.toUpperCase()
        }
        else if(health == "Invalid"){
            finalName = cropName.toUpperCase()
        }
        else{
            finalName = cropName.toUpperCase() + health.toUpperCase()
        }

        if(File(context.getExternalFilesDir(finalName).toString()).exists()){
           count = getNumberOfFiles(file)
        }
        count++

        myExternalFile = File(context.getExternalFilesDir(finalName), "${finalName!!.toLowerCase()}${count}.jpg")

        val fileOutputStream = FileOutputStream(myExternalFile)
        try{
            var bitmap = BitmapFactory.decodeFile(file.absolutePath)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
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
}