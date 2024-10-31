package com.equinos.tips

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.equinos.R

class TipsAdapter(private var context: Context, private var items: List<TipsDomain>) :
    RecyclerView.Adapter<TipsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var imageView: ImageView = itemView.findViewById(R.id.tipIv)
        var title: TextView = itemView.findViewById(R.id.tipTitle)
        var textContent: TextView = itemView.findViewById(R.id.textContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.viewholder_tips_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = items[position].title
        holder.textContent.text = items[position].textContent
        Glide.with(holder.itemView.context)
            .load(items[position].picId)
            .transform(GranularRoundedCorners(30F, 30F, 0F, 0F))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}