package com.app.combined.fragments

import android.Manifest
import android.R.attr
import android.app.Activity
import android.app.Activity.*
import android.app.Dialog
import android.content.Context
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
import android.text.TextUtils
import android.view.*
import android.widget.ArrayAdapter
import android.widget.TableLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.app.combined.FileUtil
import com.app.combined.PagerAdapter
import com.app.combined.R
import com.app.combined.activities.LauncherActivity
import com.app.combined.storage.SaveOffline
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_launcher.*
import kotlinx.android.synthetic.main.dialog_custom.*
import kotlinx.android.synthetic.main.fragment_label.*
import kotlinx.android.synthetic.main.fragment_label.view.*
import kotlinx.android.synthetic.main.fragment_predict.*
import pyxis.uzuki.live.mediaresizer.MediaResizer
import pyxis.uzuki.live.mediaresizer.data.ImageResizeOption
import pyxis.uzuki.live.mediaresizer.data.ResizeOption
import pyxis.uzuki.live.mediaresizer.model.ImageMode
import pyxis.uzuki.live.mediaresizer.model.MediaType
import pyxis.uzuki.live.mediaresizer.model.ScanRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class LabelFragment() : Fragment() {

    lateinit var bitmap: Bitmap
    lateinit var date: String
    lateinit var photoFile: File
    var f: String? = null
    lateinit var fileProvider: Uri

    var uri: Uri? = null
    lateinit var label: String
    val FILE_NAME = "pic"
    lateinit var name: String
    lateinit var currentLang: String

    var pressGal = false
    var pressCam = false

    var list: MutableList<String> = ArrayList()
    var labelList: MutableList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    private fun setDropdown(v: View)
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
                setAdapter(v)

            }

            override fun onCancelled(databaseError: DatabaseError) {

                Toast.makeText(context, databaseError.message, Toast.LENGTH_SHORT).show()
            }
        }
        reference.addValueEventListener(postListener)

    }

    fun setAdapter(v: View)
    {
        val adapter: ArrayAdapter<String> = ArrayAdapter<String>(
                context!!,
                android.R.layout.simple_expandable_list_item_1,
                list
        )

        v.etName.setAdapter(adapter)
        v.etName.setOnItemClickListener { parent, view, position, id ->

        }
    }

    private fun setLabelDropdown(v: View) {
        val reference = FirebaseDatabase.getInstance().reference.child("Labels")

        val postListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if(dataSnapshot.hasChildren()){

                    for (child in dataSnapshot.children) {

                        if(!labelList.contains(child.key.toString()) && child.key.toString()!=null)
                            labelList.add(child.key.toString())

                    }
                }
                setLabelAdapter(v)

            }

            override fun onCancelled(databaseError: DatabaseError) {

                Toast.makeText(context, databaseError.message, Toast.LENGTH_SHORT).show()
            }
        }
        reference.addValueEventListener(postListener)

    }

    private fun setLabelAdapter(v: View) {

        val listAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                context!!,
                android.R.layout.simple_expandable_list_item_1,
                labelList
        )

        v.etLabel.setAdapter(listAdapter)

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {



        val prefs = context!!.getSharedPreferences("MY_LANGUAGE", Context.MODE_PRIVATE)
        currentLang = prefs.getString("myLanguage", "eng").toString()

        val v = inflater.inflate(R.layout.fragment_label, container, false)
        v.etLabel.hint = getString(R.string.enter_label)
        v.etName.hint = getString(R.string.enter_crop_name)
        v.ibSave.text = getString(R.string.save_offline_btn)

        setDropdown(v)
        setLabelDropdown(v)

        val sdf = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)
        date = sdf.format(Date())
        
        v.btnCapture.setOnClickListener(View.OnClickListener {

            if (isPermissionGranted()) {
                //Toast.makeText(context, "Permission already granted", Toast.LENGTH_SHORT).show()
                openCamera()
            } else
                takePermission()
        })

        v.btnGallery.setOnClickListener {
            pressGal = false
            reset()

            var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            this.startActivityForResult(intent, 96)
        }

        v.ibSave.setOnClickListener {

            name = etName.text.toString()
            label = etLabel.text.toString()
            val prefs = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
            val fi = prefs.getString("myFile", "").toString()
            photoFile = File(fi)
            if(!TextUtils.isEmpty(name) && photoFile!=null)
            {
                saveImageInDevice(v)
            }
            else{
                Toast.makeText(context, getString(R.string.all_fields_toast), Toast.LENGTH_SHORT).show()
            }
        }

        return v
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveImageInDevice(v: View) {

        name = etName.text.toString()
        label = etLabel.text.toString()

        if(pressGal){
            val s = FileUtil.getPath(uri!!, context!!)
            photoFile = File(s)
        }

        if(pressCam){

        val prefs = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
        val fi = prefs.getString("myFile", "").toString()
        photoFile = File(fi)
        }


        val saveFile = SaveOffline(photoFile, name, label, context!!, "label")
        saveFile.saveInDevice()

        if(etLabel.text.isNotEmpty()) {
            var labelRef = FirebaseDatabase.getInstance().reference.child("Labels")
            labelRef.child(label.toUpperCase()).setValue(UUID.randomUUID().toString()).addOnSuccessListener {




                    if (!labelList.contains(label.toUpperCase()))
                        labelList.add(label.toUpperCase())

                    setLabelAdapter(v)

            }.addOnFailureListener {
                Toast.makeText(
                        context,
                        getString(R.string.error_toast) + it.message,
                        Toast.LENGTH_SHORT
                ).show()
            }
        }

        var reference = FirebaseDatabase.getInstance().reference.child("Types")
        reference.child(name.toUpperCase()).setValue(UUID.randomUUID().toString()).addOnSuccessListener {
            if(!list.contains(name.toUpperCase()))
                list.add(name.toUpperCase())

            setAdapter(v)
        }



    }

    private fun takePermission() {
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
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
        }
        else{
            requestPermissions(
                    arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA
                    ), 2
            )
        }
    }

    private fun isPermissionGranted(): Boolean {
        if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R)
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
        }
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
                openCamera()
            }
            else{
                takePermission()
            }
        }

    }

    private fun reset() {

        ivImg.setImageBitmap(null)
        etName.text = null
        etLabel.text = null
    }

    fun openCamera()
    {
        pressCam = false
        reset()
        var camIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        photoFile = getPhotoFile(FILE_NAME)

        val editor = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE).edit()
        editor.putString("myFile", photoFile.toString())
        editor.apply()
        editor.commit()

        fileProvider = FileProvider.getUriForFile(context!!, "com.app.combined.fileprovider", photoFile!!)

        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        val frag = LabelFragment()

       startActivityForResult(camIntent, 99)
    }

    private fun getPhotoFile(fileName: String): File {

        val storageDirectory = context!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        //super.onActivityResult(requestCode, resultCode, data)


        if(resultCode == Activity.RESULT_OK){
            if(requestCode == 1){
                if(Build.VERSION.SDK_INT == Build.VERSION_CODES.R){
                    if(Environment.isExternalStorageManager()){
                        //Toast.makeText(context, "perm granted", Toast.LENGTH_SHORT).show()
                        openCamera()
                    }
                }
            }
        }

        if(requestCode == 99 && resultCode == RESULT_OK)
        {
            pressCam = true

            setLayout(context!!.getSharedPreferences("MY_LANGUAGE", AppCompatActivity.MODE_PRIVATE).getString("myLanguage", "eng").toString())

            val editor = context!!.getSharedPreferences("MY_FILE", AppCompatActivity.MODE_PRIVATE)
            photoFile = File(editor.getString("myFile", "").toString())

            bitmap = BitmapFactory.decodeFile(photoFile!!.path)
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
                    .setTargetPath(photoFile!!.absolutePath)
                    .setOutputPath(photoFile!!.absolutePath)
                    .build()

            MediaResizer.process(option)
            //uri = Uri.fromFile(fi)

        }

        if(resultCode == RESULT_OK && requestCode == 96 && data != null) {

            pressGal = true

            ivImg.setImageURI(data.data)
            uri = data.data
           // bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, uri)

            photoFile = File(uri.toString())


        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.main_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId == R.id.menuReset){
            //Toast.makeText(context, "reset clicked", Toast.LENGTH_SHORT).show()

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
        etLabel.hint = getString(R.string.enter_label)
        etName.hint = getString(R.string.enter_crop_name)
        ibSave.text = getString(R.string.save_offline_btn)


    }

}