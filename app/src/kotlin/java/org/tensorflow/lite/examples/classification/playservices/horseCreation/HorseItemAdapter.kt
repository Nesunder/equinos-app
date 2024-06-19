package org.tensorflow.lite.examples.classification.playservices.horseCreation


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.tensorflow.lite.examples.classification.playservices.R


class HorseItemAdapter(
    private var items: List<HorseItem>, private val onItemClick: (HorseItem) -> Unit
) :
    RecyclerView.Adapter<HorseItemAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.item_image)
        val textView: TextView = view.findViewById(R.id.item_text)

        init {
            view.setOnClickListener {
                onItemClick(items[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.horse_item_dropdown, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageURI(item.imageUri)
        holder.textView.text = item.text
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<HorseItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun addItem(newItem: HorseItem) {
        val updatedItems = items.toMutableList()
        updatedItems.add(newItem)
        items = updatedItems
        notifyItemInserted(items.size - 1)
    }
}
