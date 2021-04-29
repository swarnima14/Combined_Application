package com.app.combined.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.app.combined.PagerAdapter
import com.app.combined.R
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_launcher.*

class LauncherActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        setSupportActionBar(toolbar)
        toolbar.title = "Combined"

        tabLayout.addTab(tabLayout.newTab().setText("Predict"))
        tabLayout.addTab(tabLayout.newTab().setText("Label"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = PagerAdapter(supportFragmentManager, lifecycle)
        viewPager.adapter = adapter

        tabLayout.addOnTabSelectedListener(object :TabLayout.OnTabSelectedListener{
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.currentItem = tab!!.position
            }

        })
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

                Toast.makeText(this, "label", Toast.LENGTH_SHORT).show()
            }
        }
        return true

    }
}