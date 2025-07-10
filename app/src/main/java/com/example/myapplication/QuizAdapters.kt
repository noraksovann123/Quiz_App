package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class QuizAdapters(
    private var quizzes: List<Quiz>,
    private val onClick: (Quiz) -> Unit
) : RecyclerView.Adapter<QuizAdapters.QuizViewHolder>() {

    class QuizViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.quizTitle)
        val description: TextView = itemView.findViewById(R.id.quizDescription)
        val image: ImageView = itemView.findViewById(R.id.quizImage)
        val authorName: TextView = itemView.findViewById(R.id.authorName)
        val authorImage: ImageView = itemView.findViewById(R.id.authorImage)
        val questionCount: TextView = itemView.findViewById(R.id.questionCount)
        val playCount: TextView = itemView.findViewById(R.id.playCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quiz, parent, false)
        return QuizViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuizViewHolder, position: Int) {
        val quiz = quizzes[position]
        
        holder.title.text = quiz.title
        holder.description.text = quiz.description
        holder.authorName.text = quiz.authorName
        holder.questionCount.text = "${quiz.questionCount} questions"
        holder.playCount.text = "${quiz.playCount} plays"
        
        // Load quiz cover image
        Glide.with(holder.itemView.context)
            .load(quiz.imageUrl)
            .placeholder(R.drawable.placeholder_quiz)
            .into(holder.image)
        
        // Load author image
        Glide.with(holder.itemView.context)
            .load(quiz.authorImageUrl)
            .placeholder(R.drawable.default_profile_image)
            .circleCrop()
            .into(holder.authorImage)
            
        holder.itemView.setOnClickListener {
            onClick(quiz)
        }
    }
    
    override fun getItemCount(): Int = quizzes.size
    
    fun updateData(newQuizzes: List<Quiz>) {
        quizzes = newQuizzes
        notifyDataSetChanged()
    }
}