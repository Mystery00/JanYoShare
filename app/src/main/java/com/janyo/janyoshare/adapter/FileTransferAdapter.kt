package com.janyo.janyoshare.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.bumptech.glide.Glide
import com.janyo.janyoshare.R
import com.janyo.janyoshare.classes.TransferFile
import com.janyo.janyoshare.util.JYFileUtil
import com.mystery0.tools.FileUtil.FileUtil
import com.mystery0.tools.Logs.Logs
import java.io.File
import java.io.Serializable

class FileTransferAdapter(val context: Context,
						  val list: List<TransferFile>) : RecyclerView.Adapter<FileTransferAdapter.ViewHolder>(), Serializable
{
	private val TAG = "FileTransferAdapter"

	override fun onBindViewHolder(holder: ViewHolder, position: Int)
	{
		val transferFile = list[position]
		holder.fileName.text = transferFile.fileName
		holder.filePath.text = transferFile.filePath
		holder.fileSize.text = FileUtil.FormatFileSize(transferFile.fileSize)
		holder.progressBar.max = 100
		holder.progressBar.progress = transferFile.transferProgress
		when (File(transferFile.filePath).extension)
		{
			"png", "jpg", "jpeg" ->
			{
				Glide.with(context)
						.load(transferFile.filePath)
						.into(holder.fileImg)
			}
			"apk" ->
			{
				Glide.with(context)
						.load(JYFileUtil.getApkIconPath(context, transferFile.filePath))
						.into(holder.fileImg)
			}
			else ->
			{
				holder.fileImg.setImageResource(R.mipmap.ic_file)
			}
		}
	}

	override fun getItemCount(): Int
	{
		return list.size
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder
	{
		val view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false)
		return ViewHolder(view)
	}

	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
	{
		var fileImg: ImageView = itemView.findViewById(R.id.fileImg)
		var fileName: TextView = itemView.findViewById(R.id.fileName)
		var filePath: TextView = itemView.findViewById(R.id.filePath)
		var fileSize: TextView = itemView.findViewById(R.id.fileSize)
		var progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
	}
}