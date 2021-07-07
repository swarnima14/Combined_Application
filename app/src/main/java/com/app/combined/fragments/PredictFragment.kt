package com.app.combined.fragments

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.app.combined.FileUtil
import com.app.combined.R
import com.app.combined.activities.LauncherActivity
import com.app.combined.apiarea.MyAPI
import com.app.combined.apiarea.UploadRequestBody
import com.app.combined.mlmodel.Classify
import com.app.combined.mlmodel.DiseaseDetection
import com.app.combined.storage.SaveOffline
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_launcher.*
import kotlinx.android.synthetic.main.dialog_custom.*
import kotlinx.android.synthetic.main.fragment_label.*
import kotlinx.android.synthetic.main.fragment_predict.*
import kotlinx.android.synthetic.main.fragment_predict.view.*
import okhttp3.MultipartBody
import pyxis.uzuki.live.mediaresizer.MediaResizer
import pyxis.uzuki.live.mediaresizer.data.ImageResizeOption
import pyxis.uzuki.live.mediaresizer.data.ResizeOption
import pyxis.uzuki.live.mediaresizer.model.ImageMode
import pyxis.uzuki.live.mediaresizer.model.MediaType
import pyxis.uzuki.live.mediaresizer.model.ScanRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*

class PredictFragment() : Fragment(),UploadRequestBody.UploadCallback {
    val FILENAME = "pic"
    var photoFile: File? = null
    lateinit var fileProvider: Uri
    var bitmap: Bitmap? = null
    var health ="INVALID"
    var cropName = "INVALID"
    lateinit var fileName: String
    var currentLang: String? = null
    var uri: Uri? = null

    var pressGal = false
    var pressCam = false

    var btnCam = false
    var btnGal = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val prefs = context!!.getSharedPreferences("MY_LANGUAGE", MODE_PRIVATE)
        currentLang = prefs.getString("myLanguage", "eng").toString()

        val v = inflater.inflate(R.layout.fragment_predict, container, false)

        v.imgPredict.setImageResource(R.drawable.no_image)
        v.tvCropName.text = getString(R.string.crop_name)
        v.tvHealth.text = getString(R.string.health_status_text)
        v.tvArea.text = getString(R.string.area)

        v.btnCamera.setOnClickListener {

            btnCam = true
            btnGal = false

            if(isPermissionGranted()){
                //Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show()
                openCamera()
            } else
                takePermission()
        }

        v.btnPreGallery.setOnClickListener {

            btnGal = true
            btnCam = false
            pressGal = false

            if(isPermissionGranted())
                openGallery()

            else
            {
                takePermission()
            }



        }

        v.preOffline.setOnClickListener {

            if(pressGal) {
                val s = FileUtil.getPath(uri!!, context!!)
                photoFile = File(s)

                if(health != getString(R.string.invalid) || cropName != getString(R.string.invalid)) {

                    val saveOffline = SaveOffline(photoFile!!, cropName, health, context!!, "predict")
                    saveOffline.saveInDevice()
                }
                else
                    Toast.makeText(context, getString(R.string.could_not_save_toast), Toast.LENGTH_SHORT).show()
            }
            else {
                val prefs = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
                val fi = prefs.getString("myFile", "").toString()
                photoFile = File(fi)

                if(health != getString(R.string.invalid) || cropName != getString(R.string.invalid)) {
                    val saveOffline = SaveOffline(photoFile!!, cropName, health, context!!, "predict")
                    saveOffline.saveInDevice()
                }
                else
                    Toast.makeText(context, getString(R.string.could_not_save_toast), Toast.LENGTH_SHORT).show()
            }


        }



        return v
    }

    fun openGallery(){
        reset()
        var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        this.startActivityForResult(intent, 97)
    }

    fun getArea() {

        if(pressGal) {
            //photoFile = File(uri.toString())
            val s = FileUtil.getPath(uri!!, context!!)
            photoFile = File(s)
        }
        else {

            val prefs = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
            val fi = prefs.getString("myFile", "").toString()
            photoFile = File(fi)
        }
        // bitmap = BitmapFactory.decodeFile(photoFile!!.path)

        if(bitmap != null && photoFile != null){
            sendImage()
        }
        else
            Toast.makeText(context, getString(R.string.no_image_sel_toast), Toast.LENGTH_SHORT).show()
    }

    fun getHealthStatus() {

        if(pressGal)
            photoFile = File(uri.toString())
        else {

            val prefs = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
            val fi = prefs.getString("myFile", "").toString()
            photoFile = File(fi)
        }
        // bitmap = BitmapFactory.decodeFile(photoFile!!.path)

        if (bitmap != null && photoFile != null) {
            if(currentLang == "hin")
                fileName = "diseasedHindi.txt"
            else
                fileName = "disease.txt"
            val inpString = context!!.assets.open(fileName).bufferedReader().use { it.readText() }
            val cropList = inpString.split("\n")

            val check = DiseaseDetection(bitmap!!, context!!, cropList)
            health = check.predictName()
            tvHealth.text = getString(R.string.health_status_text)
            tvHealth.append(" $health")

        } else
            Toast.makeText(context, getString(R.string.no_image_sel_toast), Toast.LENGTH_SHORT).show()


    }

    fun getCropName() {

        if(pressGal)
            photoFile = File(uri.toString())
        else{
            val prefs = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
            val fi = prefs.getString("myFile", "").toString()
            photoFile = File(fi)
        }

        if(bitmap != null && photoFile!= null){

            if(currentLang == "hin") {

                fileName = "cropnameHindi.txt"
            }
            else {

                fileName = "cropname.txt"
            }
            val inpString = context!!.assets.open(fileName).bufferedReader().use { it.readText() }
            val cropList = inpString.split("\n")

            val classify = Classify(bitmap!!, context!!, cropList)
            cropName = classify.predictName()
            view!!.tvCropName.text = getString(R.string.crop_name)
            view!!.tvCropName.append(" $cropName")
        }
        else
            Toast.makeText(context, getString(R.string.no_image_sel_toast), Toast.LENGTH_SHORT).show()
    }

    fun sendImage() {
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE
        val body = UploadRequestBody(photoFile!!, "multipart/form-data", this)

        MyAPI().uploadImage(
                MultipartBody.Part.createFormData("image", photoFile!!.name, body)
        ).enqueue(object : Callback<Number> {
            override fun onFailure(call: Call<Number>, t: Throwable) {
                progressBar.progress = 0
                progressBar.visibility = View.GONE
                Toast.makeText(context, getString(R.string.error_toast) + t.message, Toast.LENGTH_SHORT).show()
                tvArea.text = "${getString(R.string.area_timeout)}"
            }

            override fun onResponse(
                    call: Call<Number>,
                    response: Response<Number>
            ) {
                progressBar.progress = 100
                progressBar.visibility = View.GONE
                // Toast.makeText(context, getString(R.string.uploaded_toast), Toast.LENGTH_SHORT).show()
                if (response.body() == 0)
                    tvArea.text = getString(R.string.area) + " 1"
                else
                    tvArea.text = getString(R.string.area) + " ${response.body()}"
            }

        })
    }


    private fun takePermission() {


        requestPermissions(
                arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                ), 2
        )


        /*if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.setData(Uri.parse(String.format("package:%s", context!!.packageName)))
                startActivityForResult(intent, 1)
            }catch (e: Exception){
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, 1)
            }
        } else{
            requestPermissions(
                    arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                    ), 2
            )
        }*/
    }

    private fun isPermissionGranted(): Boolean {
        /* if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R)
             return Environment.isExternalStorageManager()
         else{
             val readExternalStoragePermission = ContextCompat.checkSelfPermission(
                     context!!,
                     arrayOf(
                             Manifest.permission.READ_EXTERNAL_STORAGE,
                             Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                     ).toString()
             )


             return readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
         }*/

        val readExternalStoragePermission = ContextCompat.checkSelfPermission(
                context!!,
                arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ).toString()
        )

        return readExternalStoragePermission == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(grantResults.size > 0 && requestCode ==2){
            val readExtStorage = grantResults[0] == PackageManager.PERMISSION_GRANTED
            if(readExtStorage){
                //Toast.makeText(context, "Perm granted", Toast.LENGTH_SHORT).show()
                    if(btnCam)
                openCamera()
                if(btnGal)
                    openGallery()
            }
            else{
                takePermission()
            }
        }
    }

    fun openCamera() {

        pressCam = false

        reset()
        val camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        photoFile = getFileName(FILENAME)

        val editor = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE).edit()
        editor.putString("myFile", photoFile.toString())
        editor.apply()
        editor.commit()

        fileProvider = FileProvider.getUriForFile(context!!, "com.app.combined.fileprovider", photoFile!!)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        startActivityForResult(camIntent, 98)
    }

    private fun reset() {
        photoFile = null
        imgPredict.setImageBitmap(null)
        tvCropName.text = getString(R.string.crop_name)
        tvHealth.text = getString(R.string.health_status_text)
        tvArea.text = getString(R.string.area)
    }

    private fun getFileName(filename: String): File {
        val storageDirectory = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", storageDirectory)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == 1){
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
                    if(Environment.isExternalStorageManager()){
                        // Toast.makeText(context, "perm granted", Toast.LENGTH_SHORT).show()
                        openCamera()
                    }
                }
            }
        }

        if(requestCode == 98 && resultCode == RESULT_OK) {

            pressCam = true

            setLayout(context!!.getSharedPreferences("MY_LANGUAGE", AppCompatActivity.MODE_PRIVATE).getString("myLanguage", "eng").toString())


            val editor = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
            photoFile = File(editor.getString("myFile", "").toString())

            bitmap = BitmapFactory.decodeFile(photoFile!!.path)
            imgPredict.setImageBitmap(bitmap)

            val resizeOption = ImageResizeOption.Builder()
                    .setImageProcessMode(ImageMode.ResizeAndCompress)
                    .setImageResolution(1280, 720)
                    .setBitmapFilter(false)
                    .setCompressFormat(Bitmap.CompressFormat.JPEG)
                    .setCompressQuality(75)
                    .setScanRequest(ScanRequest.TRUE)
                    .build()


            val option = ResizeOption.Builder()
                    .setMediaType(MediaType.IMAGE)
                    .setImageResizeOption(resizeOption)
                    .setTargetPath(photoFile!!.absolutePath)
                    .setOutputPath(photoFile!!.absolutePath)
                    .build()

            MediaResizer.process(option)

            getCropName()
            getHealthStatus()
            getArea()
        }

        if(resultCode == RESULT_OK && requestCode == 97 && data != null) {

            pressGal = true

            imgPredict.setImageURI(data.data)
            uri = data.data
            bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, uri)

            photoFile = File(uri.toString())

            getCropName()
            getHealthStatus()
            getArea()

        }
    }

    override fun onProgressUpdate(percentage: Int) {
        progressBar.progress = percentage
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.menuReset){
            // Toast.makeText(context, "reset clicked", Toast.LENGTH_SHORT).show()
            /*val intent = Intent(activity, LangSelActivity::class.java)
            startActivity(intent)*/

            val customDialog = Dialog(context!!)

            customDialog.setContentView(R.layout.dialog_custom)
            customDialog.customBtnHin.setOnClickListener {
                changeLang("hi", context!!)
                saveLanguage("hin")
                customDialog.dismiss()
                onStart()
                val i = Intent(activity, LauncherActivity::class.java)
                activity!!.overridePendingTransition(0, 0)
                startActivity(i)
                activity!!.overridePendingTransition(0, 0)
                activity!!.finish()
            }

            customDialog.customBtnEng.setOnClickListener {
                changeLang("en", context!!)
                saveLanguage("eng")
                customDialog.dismiss()
                onStart()
                val i = Intent(activity, LauncherActivity::class.java)
                activity!!.overridePendingTransition(0, 0)
                startActivity(i)
                activity!!.overridePendingTransition(0, 0)
                activity!!.finish()
            }

            customDialog.show()

        }
        return true
    }

    fun changeLang(str: String, context: Context){

        val locale = Locale(str)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.locale = locale
        context.createConfigurationContext(configuration)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)

    }

    fun saveLanguage(type: String?) {
        val editor = context!!.getSharedPreferences("MY_LANGUAGE", AppCompatActivity.MODE_PRIVATE).edit()
        editor.putString("myLanguage", type)
        editor.apply()
        editor.commit()
    }

    fun setLayout(str: String){
        if(str == "eng"){
            changeLang("en", context!!)

        }
        else{
            changeLang("hi", context!!)
        }

        tvArea.text = getString(R.string.area)
        tvCropName.text = getString(R.string.crop_name)
        tvHealth.text = getString(R.string.health_status_text)
        preOffline.text = getString(R.string.save_offline_btn)

    }



}