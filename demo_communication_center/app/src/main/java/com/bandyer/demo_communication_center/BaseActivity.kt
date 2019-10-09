/*
 * Copyright (C) 2018 Bandyer S.r.l. All Rights Reserved.
 * See LICENSE.txt for licensing information
 */

package com.bandyer.demo_communication_center

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * BaseActivity shared among all activities
 *
 * @author kristiyan
 */
abstract class BaseActivity : AppCompatActivity() {
    private var dialog: AlertDialog? = null

    protected fun showErrorDialog(text: String) {
        dialog = AlertDialog.Builder(this, R.style.MyAlertDialogStyle)
                .setTitle(R.string.dialog_error_title)
                .setMessage(text)
                .setPositiveButton(R.string.dialog_ok, null)
                .create()
        dialog?.show()
    }

    override fun onStop() {
        dialog?.dismiss()
        super.onStop()
    }
}