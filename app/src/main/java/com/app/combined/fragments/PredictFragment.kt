package com.app.combined.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.combined.R
import com.app.combined.apiarea.MyAPI
import com.app.combined.apiarea.UploadRequestBody
import com.app.combined.mlmodel.Classify
import com.app.combined.mlmodel.DiseaseDetection
import com.app.combined.storage.SaveOffline
import kotlinx.android.synthetic.main.fragment_predict.*
import kotlinx.android.synthetic.main.fragment_predict.view.*
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

class PredictFragment : Fragment(),UploadRequestBody.UploadCallback {

    val FILENAME = "pic"
    var photoFile: File? = null
    lateinit var fileProvider: Uri
    var bitmap: Bitmap? = null
    lateinit var uri: Uri
    var health ="Invalid"
    var cropName = "Invalid"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val v = inflater.inflate(R.layout.fragment_predict, container, false)

        v.btnCamera.setOnClickListener {
            checkForPermission()
        }

        v.preOffline.setOnClickListener {
            if(health != "Invalid" || cropName != "Invalid") {
                val saveOffline = SaveOffline(photoFile!!, cropName, health, context!!, "predict")
                saveOffline.saveInDevice()
            }
            else
                Toast.makeText(context, "Could not save", Toast.LENGTH_SHORT).show()
        }

        return v
    }

    private fun getArea() {
        if(bitmap != null && photoFile != null){
            sendImage()
        }
        else
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
    }

    private fun getHealthStatus() {
        if(bitmap != null && photoFile!= null){
            val fileName = "disease.txt"
            val inpString = context!!.assets.open(fileName).bufferedReader().use { it.readText() }
            val cropList = inpString.split("\n")

            val check = DiseaseDetection(bitmap!!, context!!, cropList)
            health = check.predictName()
            tvHealth.text = "Health Description: "
            tvHealth.append(health)
        }
        else
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
    }

    private fun getCropName() {

        if(bitmap != null && photoFile!= null){
            val fileName = "cropname.txt"
            val inpString = context!!.assets.open(fileName).bufferedReader().use { it.readText() }
            val cropList = inpString.split("\n")

            val classify = Classify(bitmap!!, context!!, cropList)
            cropName = classify.predictName()
            tvCropName.text = "Crop Name: "
            tvCropName.append(cropName)
        }
        else
            Toast.makeText(context, "No image selected", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Error: "+t.message, Toast.LENGTH_SHORT).show()
                tvArea.text = "Area: Timeout"
            }

            override fun onResponse(
                    call: Call<Number>,
                    response: Response<Number>
            ) {
                progressBar.progress = 100
                progressBar.visibility = View.GONE
                Toast.makeText(context, "Uploaded", Toast.LENGTH_SHORT).show()
                tvArea.text = "Area: ${response.body()}"
            }

        })
    }

    private fun checkForPermission() {

        if(ContextCompat.checkSelfPermission(context!!, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString()) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
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

        if(requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
        else
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {
        reset()
        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = getFileName(FILENAME)
        fileProvider = FileProvider.getUriForFile(context!!, "com.app.combined.fileprovider", photoFile!!)
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
        val storageDirectory = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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

            getCropName()
            getHealthStatus()
            getArea()

        }
    }

    override fun onProgressUpdate(percentage: Int) {
        progressBar.progress = percentage
    }

}