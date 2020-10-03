/* 
* This file is part of Life Hacking
* 
* Life Hacking is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Life Hacking is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Life Hacking.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.samco.trackandgraph.reminders.RemindersHelper
import com.samco.trackandgraph.tutorial.TutorialPagerAdapter
import com.samco.trackandgraph.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var navView: NavigationView

    private val navFragments = setOf(
        R.id.selectGroupFragment,
        R.id.FAQFragment,
        R.id.aboutPageFragment,
        R.id.remindersFragment,
        R.id.notesFragment,
        R.id.backupAndRestoreFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readThemeValue()
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_fragment)!! as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(navFragments, drawerLayout)
        navView = findViewById(R.id.nav_view)

        setSupportActionBar(findViewById(R.id.toolbar))
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        onDrawerHideKeyboard()
        initDrawerSpinners()
        RemindersHelper.syncAlarms(this)

        if (isFirstRun()) showTutorial()
        else destroyTutorial()
    }

    private fun initDrawerSpinners() {
        setUpThemeSpinner()
        setUpDateFormatSpinner()
    }

    private fun setUpDateFormatSpinner() {
        val spinner = navView.menu.findItem(R.id.dateFormatSpinner).actionView as AppCompatSpinner
        val formatNames = resources.getStringArray(R.array.date_formats)
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            formatNames
        )
        spinner.setSelection(getDateFormatValue())
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(av: AdapterView<*>?, v: View?, position: Int, id: Long) {
                onDateFormatSelected(position)
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }

    private fun onDateFormatSelected(index: Int) {
        getPrefs(applicationContext).edit().putInt(DATE_FORMAT_SETTING_PREF_KEY, index).apply()
    }

    private fun getDateFormatValue() = getPrefs(applicationContext).getInt(
        DATE_FORMAT_SETTING_PREF_KEY, DateFormatSetting.DMY.ordinal
    )

    private fun setUpThemeSpinner() {
        val spinner = navView.menu.findItem(R.id.themeSpinner).actionView as AppCompatSpinner
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            getThemeNames()
        )
        when (getThemeValue()) {
            AppCompatDelegate.MODE_NIGHT_NO -> spinner.setSelection(1)
            AppCompatDelegate.MODE_NIGHT_YES -> spinner.setSelection(2)
            else -> spinner.setSelection(0)
        }
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(av: AdapterView<*>?, v: View?, position: Int, id: Long) {
                onThemeSelected(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun onThemeSelected(position: Int) {
        when (position) {
            0 -> setThemeValue(getDefaultThemeValue())
            1 -> setThemeValue(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> setThemeValue(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun getDefaultThemeValue() =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        else AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY

    private fun getThemeValue() =
        getPrefs(applicationContext).getInt(THEME_SETTING_PREF_KEY, getDefaultThemeValue())

    private fun setThemeValue(themeValue: Int) {
        AppCompatDelegate.setDefaultNightMode(themeValue)
        getPrefs(applicationContext).edit().putInt(THEME_SETTING_PREF_KEY, themeValue).apply()
    }

    private fun readThemeValue() {
        val themeValue = getThemeValue()
        AppCompatDelegate.setDefaultNightMode(themeValue)
    }

    private fun getThemeNames() =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
            resources.getStringArray(R.array.theme_names_Q)
        else resources.getStringArray(R.array.theme_names_pre_Q)

    private fun onDrawerHideKeyboard() {
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerStateChanged(newState: Int) {}
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(
                    window.decorView.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }

            override fun onDrawerClosed(drawerView: View) {}
            override fun onDrawerOpened(drawerView: View) {
                val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(
                    window.decorView.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        })
    }

    private fun destroyTutorial() {
        val tutorialLayout = findViewById<ViewGroup>(R.id.tutorialOverlay)
        tutorialLayout.removeAllViews()
        getPrefs(applicationContext).edit().putBoolean(FIRST_RUN_PREF_KEY, false).apply()
    }

    private fun showTutorial() {
        val pips = listOf(
            findViewById<ImageView>(R.id.pip1),
            findViewById(R.id.pip2),
            findViewById(R.id.pip3)
        )
        val viewPager = findViewById<ViewPager>(R.id.tutorialViewPager)
        val refreshPips = { position: Int ->
            pips.forEachIndexed { i, p -> p.alpha = if (i == position) 1f else 0.5f }
        }
        refreshPips.invoke(0)
        viewPager.visibility = View.VISIBLE
        viewPager.adapter = TutorialPagerAdapter(applicationContext, this::destroyTutorial)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                refreshPips.invoke(position)
            }
        })
    }


    private fun isFirstRun() = getPrefs(applicationContext).getBoolean(FIRST_RUN_PREF_KEY, true)

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
