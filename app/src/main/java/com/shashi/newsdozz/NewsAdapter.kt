package com.shashi.newsdozz

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.w3c.dom.Text

class NewsAdapter(var context: Context, var newsList: ArrayList<NewsData>) :
    RecyclerView.Adapter<NewsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        var view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_layout_news, parent, false)

        view.setOnClickListener {
            // TODO: Implement on click
        }

        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {

        holder.tvNewsTitle.text = newsList.get(position).title
        holder.tvNewsDesc.text = newsList.get(position).desc

        Glide
            .with(context)
            .load(newsList.get(position).imageUrl)
            .placeholder(R.drawable.icon_placeholder)
            .into(holder.ivNewsImage)

    }

    override fun getItemCount(): Int {
        return newsList.size
    }

}

class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var ivNewsImage = itemView.findViewById<ImageView>(R.id.ivNewsIVN)
    var tvNewsTitle = itemView.findViewById<TextView>(R.id.tvNewsTitleIV)
    var tvNewsDesc = itemView.findViewById<TextView>(R.id.tvNewsDescIV)

}