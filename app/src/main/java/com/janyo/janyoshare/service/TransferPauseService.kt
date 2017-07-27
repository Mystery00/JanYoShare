package com.janyo.janyoshare.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.janyo.janyoshare.util.TransferFileNotification

class TransferPauseService : Service()
{
	override fun onBind(intent: Intent): IBinder?
	{
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		TransferFileNotification.notify(this, 0, "pause")
		stopSelf()
		return super.onStartCommand(intent, flags, startId)
	}
}