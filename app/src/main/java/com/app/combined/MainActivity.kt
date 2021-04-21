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
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.MultipartBody
import pyxis.uzuki.live.mediaresizer.MediaResizer
import pyxis.uzuki.live.mediaresizer.data.ImageResizeOption
import pyxis.uzuki.live.mediaresizer.data.ResizeOption
import pyxis.uzuki.live.mediaresizer.model.ImageMode
import pyxis.uzuki.live.mediaresizer.model.ScanRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File


class MainActivity : AppCompatActivity(), UploadRequestBody.UploadCallback {

    val FILENAME = "pic"
    var photoFile: File? = null
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

        btnDiseased.setOnClickListener {

            if(bitmap != null && photoFile!= null){
                val fileName = "disease.txt"
                val inpString = application.assets.open(fileName).bufferedReader().use { it.readText() }
                val cropList = inpString.split("\n")

                val check = DiseaseDetection(bitmap!!, this, cropList)
                var name = check.predictName()
                tvHealth.text = "Health Description: "
                tvHealth.append(name)
            }
            else
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
        }

        btnArea.setOnClickListener {

            if(bitmap != null && photoFile != null){
                sendImage()
            }
            else
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()

        }

    }

    private fun sendImage() {
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE
        val body = UploadRequestBody(photoFile!!, "multipart/form-data", this)

        MyAPI().uploadImage(
            MultipartBody.Part.createFormData("image", photoFile!!.name, body)
        ).enqueue(object: Callback<Number> {
            override fun onFailure(call: Call<Number>, t: Throwable) {
                progressBar.progress = 0
                progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Error: "+t.message, Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(
                call: Call<Number>,
                response: Response<Number>
            ) {
                progressBar.progress = 100
                progressBar.visibility = View.GONE
                Toast.makeText(this@MainActivity, "Uploaded", Toast.LENGTH_SHORT).show()
                tvArea.text = "Area: ${response.body()}"
            }

        })
    }

    /*override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.me, menu)
        return true
    }*/

    private fun checkForPermission() {
        if(ActivityCompat.checkSelfPermission(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString()) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
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
        reset()

        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = getFileName(FILENAME)
        fileProvider = FileProvider.getUriForFile(this, "com.app.combined.fileprovider", photoFile!!)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)
        startActivityForResult(camIntent, 1)
    }

    private fun reset() {
        photoFile = null
        imageView.setImageBitmap(null)
        tvCropName.text = "Crop Name: "
        tvHealth.text = "Health description: "
        tvArea.text = "Area: "
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

            val resizeOption = ImageResizeOption.Builder()
                .setImageProcessMode(ImageMode.ResizeAndCompress)
                .setImageResolution(1280, 720)
                .setBitmapFilter(false)
                .setCompressFormat(Bitmap.CompressFormat.JPEG)
                .setCompressQuality(75)
                .setScanRequest(ScanRequest.TRUE)
                .build()

            val option = ResizeOption.Builder()
                .setMediaType(pyxis.uzuki.live.mediaresizer.model.MediaType.IMAGE)
                .setImageResizeOption(resizeOption)
                .setTargetPath(photoFile!!.absolutePath)
                .setOutputPath(photoFile!!.absolutePath)
                .build()

            MediaResizer.process(option)

            uri = Uri.fromFile(photoFile)
        }
    }

    override fun onProgressUpdate(percentage: Int) {
        progressBar.progress = percentage
    }

    override fun onBackPressed() {
        super.onBackPressed()

        progressBar.visibility = View.GONE
        //imageView.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.no_image))

    }

}