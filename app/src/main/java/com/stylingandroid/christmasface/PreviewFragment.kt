package com.stylingandroid.christmasface

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.FileProvider
import android.support.v4.view.MenuItemCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.ShareActionProvider
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_preview.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File


private const val MIME_TYPE = "image/jpeg"

class PreviewFragment : Fragment() {

    private val cacheFile: File by lazyFast {
        File(context?.filesDir, ASSET_NAME)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_preview, container, false)

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity).apply {
            supportActionBar?.apply {
                setHomeButtonEnabled(true)
                setDisplayHomeAsUpEnabled(true)
            }
        }
        async(UI) {
            image?.setImageBitmap(readCacheFile())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.menu_preview, menu).also {
            menu?.findItem(R.id.menu_share)?.apply {
                (MenuItemCompat.getActionProvider(this) as ShareActionProvider).also { actionProvider ->
                    context?.apply {
                        Intent(Intent.ACTION_SEND).also {
                            it.type = MIME_TYPE
                            it.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, packageName, cacheFile))
                            it.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            actionProvider.setShareIntent(it)
                        }
                    }
                    actionProvider.setOnShareTargetSelectedListener { _,_ ->
                        false
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.itemId?.also {
            when (it) {
                android.R.id.home -> activity?.onBackPressed()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun readCacheFile(): Bitmap =
            async(CommonPool) {
                BitmapFactory.decodeFile(cacheFile.absolutePath)
            }.await()
}
