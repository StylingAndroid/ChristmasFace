package com.stylingandroid.christmasface

import android.content.Context
import com.google.android.gms.vision.CameraSource

private const val KEY_CAMERA_FACING = "KEY_CAMERA_FACING"
private const val KEY_OVERLAY = "KEY_OVERLAY"

const val OVERLAY_SANTA = 0
const val OVERLAY_ELF = 1

class PersistentState(context: Context) {
    var cameraFacing: Int by bindSharedPreference(context, KEY_CAMERA_FACING, CameraSource.CAMERA_FACING_FRONT)
    private var overlay: Int by bindSharedPreference(context, KEY_OVERLAY, OVERLAY_SANTA)

    var drawableId: Int
        get() = if (overlay == OVERLAY_ELF) R.drawable.ic_elf_ear_hat else R.drawable.ic_santa_beard_hat
        set(value) {
            overlay = if (value == R.drawable.ic_elf_ear_hat) OVERLAY_ELF else OVERLAY_SANTA
        }

    interface Consumer {
        var persistentState: PersistentState?
    }
}
