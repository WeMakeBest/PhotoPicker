package com.softsun.photopicker.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.softsun.photopicker.R
import com.softsun.photopicker.activities.PhotoPicker
import com.softsun.photopicker.databinding.RowSmallCameraCardBinding
import com.softsun.photopicker.databinding.RowSmallVideoCardBinding
import com.softsun.photopicker.models.MediaItemModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImagesListAdapter(private val context: Context, private val layoutResId: Int) :
    RecyclerView.Adapter<ViewHolder>() {

    private val dataList: ArrayList<MediaItemModel> = ArrayList()
    val isSelectedList = ArrayList<Boolean>()

    private var isInActionMode = false
    var showCameraButton = false
    var showVideoButton = false

    val selectedFilesList = ArrayList<MediaItemModel>()

    fun clearSelections() {
        for (i in isSelectedList.indices) {
            if (isSelectedList[i]) {
                isSelectedList[i] = false
                notifyItemChanged(getUpdatedPosition(i))
            }
        }

        selectedFilesList.clear()
        isInActionMode = false

        (context as? PhotoPicker)?.updateTitle()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(dataList: List<MediaItemModel>, handler: () -> Unit) {
        this.dataList.clear()
        this.dataList.addAll(dataList)

        CoroutineScope(Dispatchers.IO).launch {
            for (item in dataList) {
                isSelectedList.add(false)
            }

            withContext(Dispatchers.Main) {
                notifyDataSetChanged()
                handler()
            }
        }
    }

    inner class MainViewHolder(itemView: View) : ViewHolder(itemView) {
        private val imageView = itemView.findViewById<ImageView>(R.id.imageView)
        private val imagebtn = itemView.findViewById<MaterialCardView>(R.id.imagebtn)
        private val ivSelected = itemView.findViewById<ImageView>(R.id.ivSelected)
        private val tvDuration = itemView.findViewById<TextView>(R.id.tvDuration)
        private val viewTransGradient = itemView.findViewById<View>(R.id.viewTransGradient)

        fun bindData(position: Int) {
            val data = dataList[position]

            Glide.with(context.applicationContext).load(data.uri).into(imageView)

            if (isSelectedList[position]) {
                ivSelected?.visibility = View.VISIBLE
            } else {
                ivSelected?.visibility = View.GONE
            }

            imagebtn.setOnLongClickListener {
                selectImage(position)
                true
            }

            imagebtn.setOnClickListener {
                if (isInActionMode) {
                    selectImage(position)
                } else {
                    selectedFilesList.add(data)
                    (context as? PhotoPicker)?.giveResult(selectedFilesList)
                }
            }

            data.duration.let {
                if (it != null) {
                    tvDuration.text = it
                    tvDuration.visibility = View.VISIBLE
                    viewTransGradient.visibility = View.VISIBLE
                } else {
                    tvDuration.visibility = View.GONE
                    viewTransGradient.visibility = View.GONE
                }
            }
        }

        private fun selectImage(position: Int) {
            (context as? PhotoPicker)?.let {
                if (isSelectedList[position]) {
                    isSelectedList[position] = false
                    selectedFilesList.removeAt(selectedFilesList.indexOf(dataList[position]))
                } else {
                    if (selectedFilesList.size == it.maxCount) {
                        Toast.makeText(context, "Max selected.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    isSelectedList[position] = true
                    selectedFilesList.add(dataList[position])
                }

                notifyItemChanged(getUpdatedPosition(position))
                (context as? PhotoPicker)?.updateTitle()
                checkActionMode()
            }
        }

        private fun checkActionMode() {
            if (!isSelectedList.contains(true)) {
                isInActionMode = false
            } else {
                if (!isInActionMode) {
                    isInActionMode = true
                }
            }
        }
    }

    inner class CameraViewHolder(itemView: View) : ViewHolder(itemView) {

        private val binding = RowSmallCameraCardBinding.bind(itemView)

        init {
            binding.imagebtn.setOnClickListener {
                (context as? PhotoPicker)?.openCamera()
            }
        }
    }

    inner class VideoViewHolder(itemView: View) : ViewHolder(itemView) {

        private val binding = RowSmallVideoCardBinding.bind(itemView)

        init {
            binding.imagebtn.setOnClickListener {
                (context as? PhotoPicker)?.openCamera()
            }
        }
    }

    private fun getUpdatedPosition(position: Int): Int {
        if (showCameraButton || showVideoButton) {
            return position + 1
        }

        return position
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        when (viewType) {
            VIEW_TYPE_CAMERA -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.row_small_camera_card, parent, false)
                return CameraViewHolder(view)
            }

            VIEW_TYPE_VIDEO -> {
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.row_small_video_card, parent, false)
                return VideoViewHolder(view)
            }

            else -> {
                val view = LayoutInflater.from(context).inflate(layoutResId, parent, false)
                return MainViewHolder(view)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (showCameraButton && position == 0) {
            return VIEW_TYPE_CAMERA
        }

        if (showVideoButton && position == 0) {
            return VIEW_TYPE_VIDEO
        }

        return VIEW_TYPE_IMAGE
    }

    override fun getItemCount(): Int {
        if (showCameraButton) {
            return dataList.size + 1
        }

        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is MainViewHolder -> {
                val finalPos = if (showCameraButton || showVideoButton) {
                    (position - 1).coerceAtLeast(0)
                } else {
                    position
                }

                holder.bindData(finalPos)
            }
        }
    }

    companion object {
        const val VIEW_TYPE_CAMERA = 0
        const val VIEW_TYPE_VIDEO = 2
        const val VIEW_TYPE_IMAGE = 1
    }
}