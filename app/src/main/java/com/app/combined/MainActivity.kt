package com.app.combined

import android.Manifest
import android.R
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    val FILENAME = "pic"
    lateinit var photoFile: File
    lateinit var fileProvider: Uri
    var bitmap: Bitmap? = null
    lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.app.combined.R.layout.activity_main)

        toolbar.title = "Combined App"


        btnCamera.setOnClickListener {
            checkForPermission()
        }

        btnClassify.setOnClickListener {

            if(bitmap != null && photoFile!= null){
            val fileName = "cropname.txt"
            val inpString = application.assets.open(fileName).bufferedReader().use { it.readText() }
            val cropList = inpString.split("\n")

            val classify = Classify(bitmap!!, this, cropList)
            var name = classify.predictName()
            tvCropName.text = "Crop Name: "
            tvCropName.append(name)
            }
            else
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }

    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.me, menu)
        return true
    }*/

    private fun checkForPermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
        else
        openCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            openCamera()
        else
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {

        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = getFileName(FILENAME)
       // Toast.makeText(this, "cam", Toast.LENGTH_SHORT).show()
        fileProvider = FileProvider.getUriForFile(this, "com.app.combined.fileprovider", photoFile!!)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
        startActivityForResult(camIntent, 1)
    }

    private fun getFileName(filename: String): File {
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", storageDirectory)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == Activity.RESULT_OK){

            bitmap = BitmapFactory.decodeFile(photoFile!!.path)
            imageView.setImageBitmap(bitmap)

            uri = Uri.fromFile(photoFile)
        }
    }

}