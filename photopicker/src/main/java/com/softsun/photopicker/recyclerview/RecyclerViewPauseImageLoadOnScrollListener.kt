package com.softsun.photopicker.recyclerview

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.bumptech.glide.Glide

class RecyclerViewPauseImageLoadOnScrollListener(
    private val context: Context,
    pauseScrollConfiguration: ImageLoadingScrollConfiguration? = ImageLoadingScrollConfiguration.PAUSE_ON_SETTLING,
    private val mExternalListener: OnScrollListener?
) : OnScrollListener() {

    private var stopped = false
    private var pauseOnDragging = false
    private var pauseOnSetting = true

    init {
        when(pauseScrollConfiguration) {
            ImageLoadingScrollConfiguration.PAUSE_ON_SETTLING -> {
                pauseOnDragging = false
                pauseOnSetting = true
            }

            ImageLoadingScrollConfiguration.PAUSE_ON_DRAGGING -> {
                pauseOnDragging = true
                pauseOnSetting = false
            }

            else -> {}
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)
        mExternalListener?.onScrolled(recyclerView, dx, dy)
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        super.onScrollStateChanged(recyclerView, newState)
        when (newState) {
            RecyclerView.SCROLL_STATE_IDLE -> {
                Glide.with(context.applicationContext).resumeRequests()
                stopped = false
            }

            RecyclerView.SCROLL_STATE_DRAGGING -> {
                if (pauseOnDragging) {
                    Glide.with(context.applicationContext).pauseRequests()
                    stopped = true
                } else if (stopped) {
                    Glide.with(context.applicationContext).resumeRequests()
                    stopped = false
                }
            }

            RecyclerView.SCROLL_STATE_SETTLING -> {
                if (pauseOnSetting) {
                    Glide.with(context.applicationContext).pauseRequests()
                    stopped = true
                } else if (stopped) {
                    Glide.with(context.applicationContext).resumeRequests()
                    stopped = false
                }
            }
        }
        mExternalListener?.onScrollStateChanged(recyclerView, newState)
    }

    companion object {

        fun getPauseScrollListener(context: Context, recyclerViewScrollListener: OnScrollListener?): OnScrollListener {
            return RecyclerViewPauseImageLoadOnScrollListener(
                context,
                ImageLoadingScrollConfiguration.PAUSE_ON_SETTLING,
                recyclerViewScrollListener
            )
        }
    }
}