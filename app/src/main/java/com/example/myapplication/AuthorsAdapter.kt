package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

// Define an Author data class outside of HomeFragment for better reusability
data class Author(
    val id: String,
    val name: String,
    val photoUrl: String,
    val quizCount: Int
)

class AuthorsAdapter(
    private val authors: List<Author>,
    private val onAuthorClick: (String) -> Unit
) : RecyclerView.Adapter<AuthorsAdapter.AuthorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuthorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_author, parent, false)
        return AuthorViewHolder(view)
    }

    override fun onBindViewHolder(holder: AuthorViewHolder, position: Int) {
        holder.bind(authors[position])
    }

    override fun getItemCount(): Int = authors.size

    inner class AuthorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val authorImage: ImageView = itemView.findViewById(R.id.authorImage)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val quizCountText: TextView = itemView.findViewById(R.id.quizCount)

        fun bind(author: Author) {
            // Set name and quiz count
            authorName.text = author.name
            quizCountText.text = "${author.quizCount} ${if (author.quizCount == 1) "quiz" else "quizzes"}"

            // Load author image with Glide
            if (author.photoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(author.photoUrl)
                    .placeholder(R.drawable.author_placeholder)
                    .error(R.drawable.author_placeholder)
                    .circleCrop()
                    .into(authorImage)
            } else {
                // Set default image if no photo URL
                authorImage.setImageResource(R.drawable.author_placeholder)
            }

            // Set click listener
            itemView.setOnClickListener {
                onAuthorClick(author.id)
            }
        }
    }
}