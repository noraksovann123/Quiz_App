package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class QuizAdapter(
    private val quizzes: List<Quiz>,
    private val onQuizClick: (Quiz) -> Unit
) : RecyclerView.Adapter<QuizAdapter.QuizViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_quiz, parent, false)
        return QuizViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        holder.bind(quizzes[position])
    }

    override fun getItemCount(): Int = quizzes.size

    inner class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val quizImage: ImageView = itemView.findViewById(R.id.quizImage)
        private val quizTitle: TextView = itemView.findViewById(R.id.quizTitle)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val questionCount: TextView = itemView.findViewById(R.id.questionCount)
        private val playCount: TextView = itemView.findViewById(R.id.playCount)

        fun bind(quiz: Quiz) {
            // Set text values
            quizTitle.text = quiz.title
            authorName.text = quiz.authorName
            questionCount.text = "${quiz.questionCount} questions"
            
            // Format play count with K suffix if > 1000
            playCount.text = when {
                quiz.playCount >= 1000 -> String.format("%.1fK", quiz.playCount / 1000.0)
                else -> "${quiz.playCount}"
            } + " plays"

            // Load quiz image with Glide
            if (quiz.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(quiz.imageUrl)
                    .placeholder(R.drawable.quiz_placeholder)
                    .error(R.drawable.quiz_placeholder)
                    .centerCrop()
                    .into(quizImage)
            } else {
                // Set default image if no image URL
                quizImage.setImageResource(R.drawable.quiz_placeholder)
            }

            // Set click listener
            itemView.setOnClickListener {
                onQuizClick(quiz)
            }
        }
    }
}