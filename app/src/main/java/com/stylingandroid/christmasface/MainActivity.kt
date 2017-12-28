package com.stylingandroid.christmasface

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), ScannerFragment.BitmapConsumer {

    private val requestCode = 0
    private val permissions = arrayOf(Manifest.permission.CAMERA)

    private var requiresPermission = false
    private val persistentState: PersistentState by lazyFast {
        PersistentState(applicationContext)
    }

    private fun startSettings(): () -> Unit = {
        with(Intent()) {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
            startActivity(this)
        }
    }

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
        }
        lacksPermissions(*permissions) {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        requiresPermission = true
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    override fun onResume() {
        super.onResume()
        hasPermissions(*permissions) {
            if (requiresPermission) {
                requiresPermission = false
            }
            supportFragmentManager.createOrReturnFragment(ScannerFragment::class.java.canonicalName)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.any { it == PackageManager.PERMISSION_DENIED }) {
            supportFragmentManager.createOrReturnFragment(MissingPermissionFragment::class.java.canonicalName).also {
                (it as MissingPermissionFragment).navigateToSettings = startSettings()
            }
        } else {
            requiresPermission = false
        }
    }

    private fun FragmentManager.createOrReturnFragment(className: String,
                                                       addToBackStack: Boolean = false,
                                                       initialize: (fragment: Fragment) -> Unit = {}): Fragment =
            findFragmentByTag(className) ?: createFragment(className, addToBackStack, initialize)

    private fun createFragment(className: String, addToBackStack: Boolean, initialize: (fragment: Fragment) -> Unit) =
            Fragment.instantiate(this, className).also {
                initialize(it)
                (it as? PersistentState.Consumer)?.apply {
                    it.persistentState = this@MainActivity.persistentState
                }
                supportFragmentManager.beginTransaction().apply {
                    if (addToBackStack) {
                        addToBackStack(className)
                    }
                    replace(R.id.fragment_container, it, className)
                }.commit()
            }

    override fun handleBitmap(bitmap: Bitmap) {
        supportFragmentManager.createOrReturnFragment(PreviewFragment::class.java.canonicalName, true)
    }
}
