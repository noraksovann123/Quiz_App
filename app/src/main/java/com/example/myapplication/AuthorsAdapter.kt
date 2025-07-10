package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide



class AuthorsAdapter(
    private var authors: List<Author>,
    private val onAuthorClickListener: (String) -> Unit
) : RecyclerView.Adapter<AuthorsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val authorImage: ImageView = itemView.findViewById(R.id.authorImage)
        val authorName: TextView = itemView.findViewById(R.id.authorName)
        val quizCount: TextView = itemView.findViewById(R.id.quizCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_author, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val author = authors[position]

        holder.authorName.text = author.username
        holder.quizCount.text = "${author.quizCount} quizzes"

        if (author.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(author.photoUrl)
                .placeholder(R.drawable.default_profile_image)
                .circleCrop()
                .into(holder.authorImage)
        } else {
            holder.authorImage.setImageResource(R.drawable.default_profile_image)
        }

        holder.itemView.setOnClickListener {
            onAuthorClickListener(author.id)
        }
    }

    override fun getItemCount(): Int = authors.size
}