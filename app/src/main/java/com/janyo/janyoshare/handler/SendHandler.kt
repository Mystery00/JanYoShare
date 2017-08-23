package com.janyo.janyoshare.handler

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.support.v4.app.ActivityOptionsCompat
import android.widget.Toast
import com.janyo.janyoshare.R
import com.janyo.janyoshare.activity.FileTransferActivity
import com.janyo.janyoshare.activity.FileTransferConfigureActivity
import com.janyo.janyoshare.util.FileTransferHelper
import dmax.dialog.SpotsDialog

class SendHandler : Handler()
{
	lateinit var progressDialog: SpotsDialog
	lateinit var context: Context

	override fun handleMessage(msg: Message)
	{
		when (msg.what)
		{
			FileTransferConfigureActivity.CREATE_SERVER ->
			{
				progressDialog.setMessage(context.getString(R.string.hint_socket_connecting))
			}
			FileTransferConfigureActivity.VERIFY_DEVICE ->
			{
				progressDialog.setMessage(context.getString(R.string.hint_socket_verifying))
			}
			FileTransferConfigureActivity.CONNECTED ->
			{
				progressDialog.dismiss()
				Toast.makeText(context, R.string.hint_socket_connected, Toast.LENGTH_SHORT)
						.show()
				FileTransferHelper.getInstance().tag = 1
				(context as Activity).finish()
				context.startActivity(Intent(context, FileTransferActivity::class.java), ActivityOptionsCompat.makeSceneTransitionAnimation(context as Activity).toBundle())
			}
		}
	}
}