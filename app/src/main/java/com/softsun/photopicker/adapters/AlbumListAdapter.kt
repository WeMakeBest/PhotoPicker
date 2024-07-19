package com.softsun.photopicker.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.softsun.photopicker.R
import com.softsun.photopicker.databinding.RowAlbumBinding
import com.softsun.photopicker.models.Album

class AlbumListAdapter(private val context: Context, private val onAlbumClickListener: OnAlbumClickListener) : RecyclerView.Adapter<ViewHolder>() {

    interface OnAlbumClickListener {
        fun onAlbumClick(album: Album)
    }

    private val dataList : ArrayList<Album> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(dataList: List<Album>) {
        this.dataList.clear()
        this.dataList.addAll(dataList)
        notifyDataSetChanged()
    }

    inner class MainViewHolder(itemView: View) : ViewHolder(itemView) {
        private val binding = RowAlbumBinding.bind(itemView)

        fun bindData(position: Int) {
            val data = dataList[position]

            binding.tvAlbumName.text = data.name
            binding.tvCount.text = data.count

            Glide.with(context.applicationContext).load(data.files[0].uri).into(binding.imageView)

            binding.mainCardView.setOnClickListener {
                onAlbumClickListener.onAlbumClick(data)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_album, parent, false)

        return MainViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MainViewHolder) {
            holder.bindData(position)
        }
    }
}