package com.stylingandroid.christmasface

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_missing_permission.*

class MissingPermissionFragment : Fragment() {
    var navigateToSettings: () -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.fragment_missing_permission, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) =
            super.onViewCreated(view, savedInstanceState).run {
                camera_permission_settings.setOnClickListener {
                    navigateToSettings()
                }
            }
}
