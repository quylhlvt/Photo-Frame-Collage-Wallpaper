package com.example.depthwallpaper.main.picker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.gallery.GalleryImage
import com.example.depthwallpaper.core.gallery.ThumbLoader
import com.example.depthwallpaper.databinding.ItemPhotoBinding
import com.example.depthwallpaper.databinding.ItemSelectedBinding

/** The 3-column photo grid: an "Add image" tile first, then the added photos (a tapped one gets the blue frame and check). */
class PhotoGridAdapter(
    private val isSelected: (GalleryImage) -> Boolean,
    private val onAdd: () -> Unit,
    private val onLongPress: (GalleryImage) -> Unit,
    private val onClick: (GalleryImage) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: List<GalleryImage> = emptyList()

    class Holder(val b: ItemPhotoBinding) : RecyclerView.ViewHolder(b.root)
    class AddHolder(view: android.view.View) : RecyclerView.ViewHolder(view)

    fun submit(list: List<GalleryImage>) { items = list; notifyDataSetChanged() }

    /** Redraws only the selection marks (the photos stay where they are). */
    fun refreshSelection() = notifyItemRangeChanged(1, items.size, PAYLOAD_SELECTION)

    override fun getItemCount() = items.size + 1
    override fun getItemViewType(position: Int) = if (position == 0) TYPE_ADD else TYPE_PHOTO

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_ADD) AddHolder(inflater.inflate(R.layout.item_add_photo, parent, false))
        else Holder(ItemPhotoBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is AddHolder) { holder.itemView.setOnClickListener { onAdd() }; return }
        holder as Holder
        val image = items[position - 1]
        val size = holder.b.root.resources.displayMetrics.widthPixels / 3
        ThumbLoader.load(holder.b.image, image.uri, size)
        bindSelection(holder, image)
        holder.b.root.setOnClickListener { onClick(image) }
        holder.b.root.setOnLongClickListener { onLongPress(image); true }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position)
        else if (holder is Holder) bindSelection(holder, items[position - 1])
    }

    private fun bindSelection(holder: Holder, image: GalleryImage) {
        val on = isSelected(image)
        holder.b.selectedBorder.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
        holder.b.check.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
    }

    private companion object {
        const val PAYLOAD_SELECTION = "selection"
        const val TYPE_ADD = 0
        const val TYPE_PHOTO = 1
    }
}

/** The chosen photos at the bottom, each with a small x to take it out. */
class SelectedAdapter(private val onRemove: (GalleryImage) -> Unit) : RecyclerView.Adapter<SelectedAdapter.Holder>() {
    private var items: List<GalleryImage> = emptyList()

    class Holder(val b: ItemSelectedBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<GalleryImage>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemSelectedBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val image = items[position]
        ThumbLoader.load(holder.b.image, image.uri, 200)
        holder.b.remove.setOnClickListener { onRemove(image) }
    }
}
