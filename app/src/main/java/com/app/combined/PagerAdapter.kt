package com.app.combined

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import com.app.combined.fragments.LabelFragment
import com.app.combined.fragments.PredictFragment

class PagerAdapter(fm: FragmentManager, lifecycle: Lifecycle): FragmentPagerAdapter(fm) {
    override fun getItem(position: Int): Fragment {
        when(position){
            0-> return PredictFragment()
            1-> return LabelFragment()
            else-> return Fragment()
        }
    }

    override fun getCount(): Int {
        return 2
    }
}