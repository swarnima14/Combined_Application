package com.app.combined

import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet_layout.*
import java.io.File
import java.io.IOException
import java.io.InputStream

class BottomSheet(val f: File): BottomSheetDialogFragment() {

     var label:String = "Not assigned"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnSave.setOnClickListener {

            label = etLabel.text.toString()

            if(TextUtils.isEmpty(label))
                Toast.makeText(context, "Enter label", Toast.LENGTH_SHORT).show()

            else {
                Toast.makeText(context, "$label", Toast.LENGTH_SHORT).show()

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
    }

}