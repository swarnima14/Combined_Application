package com.app.combined.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.app.combined.R
import com.app.combined.storage.SaveOffline
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.fragment_label.*
import kotlinx.android.synthetic.main.fragment_label.view.*
import pyxis.uzuki.live.mediaresizer.MediaResizer
import pyxis.uzuki.live.mediaresizer.data.ImageResizeOption
import pyxis.uzuki.live.mediaresizer.data.ResizeOption
import pyxis.uzuki.live.mediaresizer.model.ImageMode
import pyxis.uzuki.live.mediaresizer.model.MediaType
import pyxis.uzuki.live.mediaresizer.model.ScanRequest
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class LabelFragment : Fragment() {

    lateinit var bitmap: Bitmap
    lateinit var date: String
    lateinit var photoFile: File
    lateinit var fileProvider: Uri

    var uri: Uri? = null
    lateinit var label: String
    val FILE_NAME = "pic"
    lateinit var name: String
    var count =0

    var list: MutableList<String> = ArrayList()
    var labelList: MutableList<String> = ArrayList()

    internal var myExternalFile: File?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val v = inflater.inflate(R.layout.fragment_label, container, false)

        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)
        date = sdf.format(Date())
        
        v.btnCapture.setOnClickListener(View.OnClickListener {

            askForPermission()
        })

        v.ibUpload.setOnClickListener{

        }

        v.ibSave.setOnClickListener {

            name = etName.text.toString()
            label = etLabel.text.toString()

            if(!TextUtils.isEmpty(name) && photoFile!=null)
            {
                saveImageInDevice()
            }
            else{
                Toast.makeText(context, "All fields required.", Toast.LENGTH_SHORT).show()
            }
        }

        return v
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInDevice() {

        name = etName.text.toString()
        label = etLabel.text.toString()

        val saveFile = SaveOffline(photoFile, name, label, context!!, "label")
        saveFile.saveInDevice()

    }

    fun askForPermission()
    {
        if((ContextCompat.checkSelfPermission(context!!, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())) !=
            PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 11)
        }
        else
        {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == 11 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            openCamera()
        }
        else
        {
            Toast.makeText(context, "Camera and storage permissions are necessary.", Toast.LENGTH_SHORT).show()
        }

    }

    fun openCamera()
    {
        var camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        name=etName.text.toString()

        photoFile = getPhotoFile(FILE_NAME)

        fileProvider = FileProvider.getUriForFile(context!!,"com.app.combined.fileprovider", photoFile)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        startActivityForResult(camIntent, 99)
    }

    private fun getPhotoFile(fileName: String): File {
        name = etName.text.toString()
        val storageDirectory = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(fileName,".jpg", storageDirectory)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 100 && resultCode == Activity.RESULT_OK)
        {
            ivImg.setImageURI(data?.data)
            uri = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, uri)

        }
        else if(requestCode == 99 && resultCode == Activity.RESULT_OK)
        {
            bitmap = BitmapFactory.decodeFile(photoFile.path)

            ivImg.setImageBitmap(bitmap)

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
                .setTargetPath(photoFile.absolutePath)
                .setOutputPath(photoFile.absolutePath)
                .build()

            MediaResizer.process(option)

            uri = Uri.fromFile(photoFile)

        }

    }

}