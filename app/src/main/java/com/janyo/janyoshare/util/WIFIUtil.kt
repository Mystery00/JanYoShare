package com.janyo.janyoshare.util

import android.content.Context
import android.os.Build
import com.janyo.janyoshare.R
import com.mystery0.tools.Logs.Logs
import java.net.NetworkInterface
import java.net.SocketException

class WIFIUtil(var context: Context, val port: Int)
{
	private val TAG = "WIFIUtil"
	private var localAddress = ""//存储本机ip，例：本地ip ：192.168.1.1
	private val ping = "ping -c 1 -w 0.5 "//其中 -c 1为发送的次数，-w 表示发送后等待响应的时间

	fun getLocalAddress()
	{
		try
		{
			val en = NetworkInterface.getNetworkInterfaces()
			// 遍历所用的网络接口
			while (en.hasMoreElements())
			{
				val networks = en.nextElement()
				// 得到每一个网络接口绑定的所有ip
				val address = networks.inetAddresses
				// 遍历每一个接口绑定的所有ip
				while (address.hasMoreElements())
				{
					val ip = address.nextElement()
					if (!ip.isLoopbackAddress && isIpv4(ip.hostAddress))
					{
						localAddress = ip.hostAddress
					}
				}
			}
		}
		catch (e: SocketException)
		{
			e.printStackTrace()
		}
	}

	fun scanIP(scanListener: ScanListener)
	{
		getLocalAddress()
		Logs.i(TAG, "scanIP: " + localAddress)
		val localAddressIndex = localAddress.substring(0, localAddress.lastIndexOf(".") + 1)
		if (localAddressIndex == "")
		{
			Logs.i(TAG, "scanIP: 扫描失败")
			return
		}

		for (i in 0..255)
		{
			Thread(Runnable {
				val p = ping + localAddressIndex + i
				val currentIP = localAddressIndex + i
				if (currentIP == localAddress)
				{
					return@Runnable
				}
				try
				{
					val result = Runtime.getRuntime().exec(p).waitFor()
					if (result == 0)
					{
						val socketUtil = SocketUtil()
						if (socketUtil.createSocketConnection(currentIP, port))
						{
							scanListener.onScanFinish(currentIP, socketUtil)
						}
					}
				}
				catch (e: Exception)
				{
					scanListener.onNoting()
					e.printStackTrace()
				}
			}).start()
		}
	}

	fun isIpv4(ipv4: String): Boolean
	{
		if (ipv4.isEmpty())
		{
			return false//字符串为空或者空串
		}
		val parts = ipv4.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()//因为java doc里已经说明, split的参数是reg, 即正则表达式, 如果用"|"分割, 则需使用"\\|"
		if (parts.size != 4)
		{
			return false//分割开的数组根本就不是4个数字
		}
		for (i in parts.indices)
		{
			try
			{
				val n = Integer.parseInt(parts[i])
				if (n < 0 || n > 255)
				{
					return false//数字不在正确范围内
				}
			}
			catch (e: NumberFormatException)
			{
				return false//转换数字不正确
			}

		}
		return true
	}

	interface ScanListener
	{
		fun onScanFinish(ipv4: String, socketUtil: SocketUtil)
		fun onNoting()
	}
}