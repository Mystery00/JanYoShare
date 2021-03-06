package com.janyo.janyoshare.util

import android.content.Context
import android.net.Uri
import com.janyo.janyoshare.classes.TransferFile
import vip.mystery0.tools.logs.Logs
import java.io.*
import java.net.ConnectException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class SocketUtil
{
	private val TAG = "SocketUtil"
	lateinit var socket: Socket
	private lateinit var serverSocket: ServerSocket
	lateinit var ip: String

	fun tryCreateSocketConnection(host: String, port: Int): Boolean
	{
		return try
		{
			val socket = Socket(host, port)
			if (socket.isConnected)
			{
				socket.keepAlive = true
				ip = host
				this.socket = socket
			}
			socket.isConnected
		}
		catch (e: ConnectException)
		{
			e.printStackTrace()
			false
		}
	}

	fun createSocketConnection(host: String, port: Int): Boolean
	{
		while (true)
		{
			try
			{
				val socket = Socket(host, port)
				if (socket.isConnected)
				{
					this.socket = socket
					break
				}
			}
			catch (e: Exception)
			{
				Thread.sleep(100)
				continue
			}
		}
		socket.keepAlive = true
		ip = host
		return socket.isConnected
	}

	fun createServerConnection(port: Int): Boolean
	{
		try
		{
			val singleThreadPool = Executors.newSingleThreadExecutor()
			val task = singleThreadPool.submit {
				serverSocket = ServerSocket(port)
				socket = serverSocket.accept()
			}
			try
			{
				task.get(20, TimeUnit.SECONDS)
			}
			catch (e: TimeoutException)
			{
				serverDisconnect()
				Logs.wtf(TAG, "createServerConnection: 超时", e)
				return false
			}
			finally
			{
				task.cancel(true)
			}
			socket.keepAlive = true
			this.socket = socket
		}
		catch (e: Exception)
		{
			serverDisconnect()
			Logs.wtf(TAG, "createServerConnection", e)
			return false
		}
		return socket.isConnected
	}

	fun receiveMessage(): String
	{
		var response = "null"
		val executorService = Executors.newSingleThreadExecutor()
		val task = executorService.submit {
			val inputStream = socket.getInputStream()
			response = BufferedReader(InputStreamReader(inputStream)).readLine()
		}
		try
		{
			task.get(20, TimeUnit.SECONDS)
		}
		catch (e: TimeoutException)
		{
			Logs.wtf(TAG, "receiveMessage: 超时", e)
		}
		finally
		{
			task.cancel(true)
		}
		return response
	}

	fun sendMessage(message: String)
	{
		try
		{
			val outputStream = socket.getOutputStream()
			outputStream.write((message + "\n").toByteArray())
			outputStream.flush()
		}
		catch (e: Exception)
		{
			Logs.wtf(TAG, "sendMessage", e)
		}
	}

	fun receiveFile(fileSize: Long, path: String, fileTransferListener: FileTransferListener): File?
	{
		createSocketConnection(FileTransferHelper.getInstance().ip, FileTransferHelper.getInstance().transferPort)
		val file = File(path)
		if (file.exists())
		{
			clientDisconnect()
			fileTransferListener.onError(1, RuntimeException("file is exists!"))
			return null
		}
		try
		{
			Logs.i(TAG, "receiveFile: 文件大小" + fileSize)
			val dataInputStream: DataInputStream? = DataInputStream(BufferedInputStream(socket.getInputStream()))
			val bytes = ByteArray(1024 * 1024)
			var transferredSize = 0L
			val dataOutputStream = DataOutputStream(BufferedOutputStream(FileOutputStream(file)))
			fileTransferListener.onStart()
			while (true)
			{
				val read = dataInputStream!!.read(bytes)
				transferredSize += read
				Logs.i(TAG, "receiveFile: " + read)
				if (read <= 0)
				{
					Logs.i(TAG, "receiveFile: 退出循环")
					break
				}
				fileTransferListener.onProgress((transferredSize * 100 / fileSize).toInt())
				dataOutputStream.write(bytes, 0, read)
				Logs.i(TAG, "receiveFile: 写入" + read + "个数据")
				if (fileSize == transferredSize)
				{
					Logs.i(TAG, "receiveFile: 退出循环")
					break
				}
				Thread.sleep(100)
				if (socket.isClosed)
				{
					fileTransferListener.onError(3, Exception("连接关闭"))
				}
			}
		}
		catch (e: Exception)
		{
			fileTransferListener.onError(2, e)
		}
		Logs.i(TAG, "receiveFile: 文件完成")
		clientDisconnect()
		fileTransferListener.onFinish()
		return file
	}

	fun sendFile(context: Context, transferFile: TransferFile,
				 fileTransferListener: FileTransferListener)
	{
		createServerConnection(FileTransferHelper.getInstance().transferPort)
		try
		{
			Logs.i(TAG, "sendFile: fileSize" + transferFile.fileSize)
			val dataOutputStream = DataOutputStream(socket.getOutputStream())
			val parcelFileDescriptor = context.contentResolver.openFileDescriptor(Uri.parse(transferFile.fileUri), "r")
			val fileDescriptor = parcelFileDescriptor.fileDescriptor
			val dataInputStream = DataInputStream(BufferedInputStream(FileInputStream(fileDescriptor)))
			var transferredSize = 0L
			val bytes = ByteArray(1024 * 1024)
			fileTransferListener.onStart()
			while (true)
			{
				val read = dataInputStream.read(bytes)
				Logs.i(TAG, "sendFile: " + read)
				transferredSize += read
				fileTransferListener.onProgress((transferredSize * 100 / transferFile.fileSize).toInt())
				if (read <= 0)
				{
					Logs.i(TAG, "sendFile: 退出循环")
					break
				}
				dataOutputStream.write(bytes, 0, read)
				if (socket.isClosed)
				{
					fileTransferListener.onError(3, Exception("连接关闭"))
				}
			}
			dataOutputStream.flush()
			Logs.i(TAG, "sendFile: 推流")
			fileTransferListener.onFinish()
		}
		catch (e: Exception)
		{
			fileTransferListener.onError(2, e)
		}
		serverDisconnect()
	}

	fun receiveObject(): Any?
	{
		var obj: Any? = null
		try
		{
			val objectInputStream = ObjectInputStream(BufferedInputStream(socket.getInputStream()))
			obj = objectInputStream.readObject()
		}
		catch (e: Exception)
		{
			Logs.wtf(TAG, "receiveObject", e)
		}
		clientDisconnect()
		return obj
	}

	fun sendObject(obj: Any?): Boolean
	{
		if (obj == null)
		{
			return false
		}
		try
		{
			val objectOutputStream = ObjectOutputStream(socket.getOutputStream())
			objectOutputStream.writeObject(obj)
			objectOutputStream.flush()
		}
		catch (e: Exception)
		{
			Logs.wtf(TAG, "sendObject", e)
			return false
		}
		serverDisconnect()
		return true
	}

	fun clientDisconnect()
	{
		Logs.i(TAG, "clientDisconnect: 客户端终止连接")
		try
		{
			socket.close()
		}
		catch (e: Exception)
		{
			Logs.wtf(TAG, "clientDisconnect: ", e)
		}
	}

	fun serverDisconnect()
	{
		Logs.i(TAG, "serverDisconnect: 服务端终止连接")
		try
		{
			serverSocket.close()
		}
		catch (e: Exception)
		{
			Logs.wtf(TAG, "serverDisconnect: ", e)
		}
	}

	interface FileTransferListener
	{
		fun onStart()
		fun onProgress(progress: Int)
		fun onFinish()
		fun onError(code: Int, e: Exception)
	}
}