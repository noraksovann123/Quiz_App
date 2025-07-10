package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.models.QuizQuestion

class QuestionAdapter(
    private var questions: List<QuizQuestion>,
    private val onEditQuestion: (QuizQuestion) -> Unit,
    private val onDeleteQuestion: (QuizQuestion) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val questionNumber: TextView = view.findViewById(R.id.questionNumber)
        val questionText: TextView = view.findViewById(R.id.questionText)
        val questionImage: ImageView = view.findViewById(R.id.questionImage)
        val editButton: ImageButton = view.findViewById(R.id.editButton)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
        val optionsCount: TextView = view.findViewById(R.id.optionsCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]
        holder.questionNumber.text = "Question ${position + 1}"
        holder.questionText.text = question.text
        
        // Show image if available
        if (question.imageUrl.isNotEmpty()) {
            holder.questionImage.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .load(question.imageUrl)
                .centerCrop()
                .into(holder.questionImage)
        } else {
            holder.questionImage.visibility = View.GONE
        }
        
        // Show options count
        holder.optionsCount.text = "${question.options.size} options"
        
        // Set click listeners
        holder.editButton.setOnClickListener {
            onEditQuestion(question)
        }
        
        holder.deleteButton.setOnClickListener {
            onDeleteQuestion(question)
        }
    }

    override fun getItemCount() = questions.size

    fun updateQuestions(newQuestions: List<QuizQuestion>) {
        questions = newQuestions
        notifyDataSetChanged()
    }
}