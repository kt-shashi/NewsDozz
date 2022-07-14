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

class NewsAdapter(
    var context: Context,
    private val listner: NewsItemClicked
) :
    RecyclerView.Adapter<NewsViewHolder>() {

    private var newsList = ArrayList<NewsData>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        var view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_layout_news, parent, false)

        var viewHolder = NewsViewHolder(view)

        view.setOnClickListener {
            listner.onItemClicked(newsList.get(viewHolder.adapterPosition))
        }

        return viewHolder
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

    fun updateNewsList(updatedNews: ArrayList<NewsData>) {
        newsList.clear()
        newsList.addAll(updatedNews)

        notifyDataSetChanged()
    }

}

// View Holder class for Recycler View
class NewsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var ivNewsImage = itemView.findViewById<ImageView>(R.id.ivNewsIVN)
    var tvNewsTitle = itemView.findViewById<TextView>(R.id.tvNewsTitleIV)
    var tvNewsDesc = itemView.findViewById<TextView>(R.id.tvNewsDescIV)

}

// Interface to handle News click
interface NewsItemClicked {
    fun onItemClicked(item: NewsData)
}