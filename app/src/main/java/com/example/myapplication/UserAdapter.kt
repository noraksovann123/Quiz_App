package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class UserAdapter(
    private var users: List<User>,
    private val onUserClickListener: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: ImageView = itemView.findViewById(R.id.userImage)
        val userName: TextView = itemView.findViewById(R.id.userName)
        val userUsername: TextView = itemView.findViewById(R.id.userUsername)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        
        // Use fullName instead of name
        holder.userName.text = user.fullName
        
        // Use string resource instead of concatenation
        holder.userUsername.text = holder.itemView.context.getString(
            R.string.username_format, 
            user.username
        )
        
        // Use photoUrl instead of profileImage
        if (user.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.photoUrl)
                .placeholder(R.drawable.default_profile_image)
                .circleCrop()
                .into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.default_profile_image)
        }
        
        holder.itemView.setOnClickListener {
            onUserClickListener(user)
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
