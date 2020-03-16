package com.mamoon.app.photoweather.adapters

import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mamoon.app.photoweather.room.Post


@BindingAdapter("postListData")
fun bindPostRecyclerView(recyclerView: RecyclerView, data: List<Post>?) {
    val adapter = recyclerView.adapter as PostsRecyclerViewAdapter
    adapter.submitList(data)
}


@BindingAdapter("postImageUri")
fun bindPostImage(imgView: ImageView, imgUri: String?) {
    if (!imgUri.isNullOrEmpty()) {
        val uri = Uri.parse(imgUri)
        Glide.with(imgView.context)
            .load(uri)
            .into(imgView)
    } else {
        imgView.visibility = View.INVISIBLE
    }
}
