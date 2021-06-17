package com.app.combined.activities

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View.OnTouchListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.app.combined.PagerAdapter
import com.app.combined.R
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_launcher.*
import kotlinx.android.synthetic.main.fragment_label.*
import kotlinx.android.synthetic.main.fragment_predict.*
import kotlinx.android.synthetic.main.fragment_predict.view.*
import java.io.File
import java.util.*


class LauncherActivity : AppCompatActivity() {

    lateinit var lang: String
    lateinit var health: String
    lateinit var cropName: String
    lateinit var file: File

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

       // loadLanguage()

        lang = intent.getStringExtra("lang").toString()



        setSupportActionBar(toolbar)
        toolbar.title = "Combined"

        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.predict_fragment_name)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.label_fragment_name)))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = PagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter

        viewPager.setOnTouchListener(OnTouchListener { v, event -> true })


        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {
                viewPager.currentItem = tab!!.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.currentItem = tab!!.position
            }

        })



    }

    fun changeLang(str: String, context: Context){

        val locale = Locale(str)
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        configuration.locale = locale
        context.createConfigurationContext(configuration)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)


    }

}