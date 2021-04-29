package com.app.combined.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.app.combined.R
import com.app.combined.storage.SaveOffline
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_label.*
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


class LabelActivity : AppCompatActivity(), androidx.appcompat.widget.Toolbar.OnMenuItemClickListener {

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

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label)

        toolbar.title = "Combined App"
        setSupportActionBar(toolbar)
        toolbar.setOnMenuItemClickListener(this)

        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)
        date = sdf.format(Date())

        setDropdown()
        setLabelDropdown()


        btnCapture.setOnClickListener(View.OnClickListener {

            askForPermission()
        })

        ibUpload.setOnClickListener{
            //fetchTag()
           // uploadAsync(this, list, date).execute()

        }

        ibSave.setOnClickListener {

            name = etName.text.toString()
            label = etLabel.text.toString()

            if(!TextUtils.isEmpty(name) && photoFile!=null)
            {
                saveImageInDevice()
            }
            else{
                Toast.makeText(this, "All fields required.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item!!.itemId){
            R.id.menuReset -> Toast.makeText(this, "reset", Toast.LENGTH_SHORT).show()
            /*R.id.menuUpload -> Toast.makeText(this, "upload", Toast.LENGTH_SHORT).show()
            R.id.menuSaveOffline -> {
                if(health != "Invalid" || cropName != "Invalid") {
                    val saveOffline = SaveOffline(photoFile!!, cropName, health, this)
                    saveOffline.saveInDevice()
                }
                else
                    Toast.makeText(this, "Could not save", Toast.LENGTH_SHORT).show()
            }*/
            R.id.menuPredict -> {
                startActivity(Intent(this, MainActivity::class.java))
                Toast.makeText(this, "predict", Toast.LENGTH_SHORT).show()
            }
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun extractTag(f: File){
        val inpS: InputStream?
        inpS = contentResolver.openInputStream(Uri.fromFile(f))

        try {

            val exifInterface =  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ExifInterface(f)
            } else {
                TODO("VERSION.SDK_INT < N")
            }

            exifInterface.setAttribute(ExifInterface.TAG_MAKER_NOTE,label)
            exifInterface.saveAttributes()

        } catch (e: IOException) {
            Toast.makeText(this, "Error: "+e.message, Toast.LENGTH_SHORT).show()
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

    fun getNumberOfFiles(path: File): Int {
        var numberOfFiles = 0
        if (path.exists()) {
            val files = path.listFiles() ?: return numberOfFiles
            return files.size
        }
        return 0
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInDevice() {

        name = etName.text.toString()
        label = etLabel.text.toString()

        val saveFile = SaveOffline(photoFile, name, label, this, "label")
        saveFile.saveInDevice()

        /*if(File(getExternalFilesDir(name.toUpperCase()).toString()).exists()) {
            count = getExternalFilesDir(name.toUpperCase())?.let { getNumberOfFiles(it) }!!
        }
        count++
        myExternalFile = File(getExternalFilesDir(name.toUpperCase()), "${name.toLowerCase()}${count}.jpg")



        val fileOutPutStream = FileOutputStream(myExternalFile)

        try {
            bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutPutStream)

            extractTag(myExternalFile!!)
            fileOutPutStream.close()
            photoFile.delete()
            ivImg.setImageBitmap(null)

            var reference = FirebaseDatabase.getInstance().reference.child("Types")


            if(etLabel.text.isNotEmpty()) {
                var labelRef = FirebaseDatabase.getInstance().reference.child("Labels")
                labelRef.child(label.toUpperCase()).setValue(UUID.randomUUID().toString()).addOnSuccessListener {
                    if (!labelList.contains(label.toUpperCase()))
                        labelList.add(label.toUpperCase())

                    setLabelAdapter()
                }.addOnFailureListener {
                    Toast.makeText(applicationContext, "Error: " + it.message, Toast.LENGTH_SHORT).show()
                }
            }

            reference.child(name.toUpperCase()).setValue(UUID.randomUUID().toString()).addOnSuccessListener {
                if(!list.contains(name.toUpperCase()))
                    list.add(name.toUpperCase())

                setAdapter()
            }
            Toast.makeText(applicationContext,"Saved in your device.",Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(applicationContext,"Could not be saved: "+e.message,Toast.LENGTH_SHORT).show()

        }*/



    }

    fun setAdapter()
    {
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                this,
                android.R.layout.simple_expandable_list_item_1,
                list
        )

        etName.setAdapter(adapter)
        etName.setOnItemClickListener { parent, view, position, id ->

        }
    }

    fun askForPermission()
    {
        if((ActivityCompat.checkSelfPermission(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 11)
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
            Toast.makeText(this, "Camera and storage permissions are necessary.", Toast.LENGTH_SHORT).show()
        }

    }

    fun openCamera()
    {
        var camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        name=etName.text.toString()

        photoFile = getPhotoFile(FILE_NAME)

        fileProvider = FileProvider.getUriForFile(this,"com.app.combined.fileprovider", photoFile)
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        startActivityForResult(camIntent, 99)
    }

    private fun getPhotoFile(fileName: String): File {
        name = etName.text.toString()
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(fileName,".jpg", storageDirectory)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 100 && resultCode == Activity.RESULT_OK)
        {
            ivImg.setImageURI(data?.data)
            uri = data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)

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

    @RequiresApi(Build.VERSION_CODES.N)
    fun fetchTag(){
        for(l in list)
        {
            var file = File(getExternalFilesDir(l).toString()).listFiles()
            if(file.isNotEmpty())
            {
                for(f in file)
                {
                    var i = contentResolver.openInputStream(Uri.fromFile(f))

                    var e = ExifInterface(i!!)
                    var att = e.getAttribute(ExifInterface.TAG_MAKER_NOTE)
                }
            }
        }
    }

    private fun setDropdown()
    {

        val reference = FirebaseDatabase.getInstance().reference.child("Types")

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChildren()){

                    for (child in dataSnapshot.children) {

                        if(!list.contains(child.key.toString()))
                            list.add(child.key.toString())

                    }
                }
                setAdapter()

            }

            override fun onCancelled(databaseError: DatabaseError) {

                Toast.makeText(applicationContext, databaseError.message,Toast.LENGTH_SHORT).show()
            }
        }
        reference.addValueEventListener(postListener)

    }

    private fun setLabelDropdown(){
        val reference = FirebaseDatabase.getInstance().reference.child("Labels")

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChildren()){

                    for (child in dataSnapshot.children) {

                        if(!labelList.contains(child.key.toString()) && child.key.toString()!=null)
                            labelList.add(child.key.toString())

                    }
                }
                setLabelAdapter()

            }

            override fun onCancelled(databaseError: DatabaseError) {

                Toast.makeText(applicationContext, databaseError.message,Toast.LENGTH_SHORT).show()
            }
        }
        reference.addValueEventListener(postListener)

    }

    private fun setLabelAdapter() {

        val listAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                this,
                android.R.layout.simple_expandable_list_item_1,
                labelList
        )

        etLabel.setAdapter(listAdapter)

    }

}