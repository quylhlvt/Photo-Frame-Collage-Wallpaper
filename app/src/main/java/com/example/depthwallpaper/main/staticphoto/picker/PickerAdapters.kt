package com.example.depthwallpaper.main.staticphoto.picker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.gallery.GalleryFolder
import com.example.depthwallpaper.core.gallery.GalleryImage
import com.example.depthwallpaper.core.gallery.ThumbLoader
import com.example.depthwallpaper.databinding.ItemFolderBinding
import com.example.depthwallpaper.databinding.ItemPhotoBinding
import com.example.depthwallpaper.databinding.ItemSelectedBinding

/** The 3-column photo grid; a tapped photo gets the blue frame and check. */
class PhotoGridAdapter(
    private val isSelected: (GalleryImage) -> Boolean,
    private val onClick: (GalleryImage) -> Unit,
) : RecyclerView.Adapter<PhotoGridAdapter.Holder>() {
    private var items: List<GalleryImage> = emptyList()

    class Holder(val b: ItemPhotoBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<GalleryImage>) { items = list; notifyDataSetChanged() }

    /** Redraws only the selection marks (the photos stay where they are). */
    fun refreshSelection() = notifyItemRangeChanged(0, items.size, PAYLOAD_SELECTION)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val image = items[position]
        val size = holder.b.root.resources.displayMetrics.widthPixels / 3
        ThumbLoader.load(holder.b.image, image.uri, size)
        bindSelection(holder, image)
        holder.b.root.setOnClickListener { onClick(image) }
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position) else bindSelection(holder, items[position])
    }

    private fun bindSelection(holder: Holder, image: GalleryImage) {
        val on = isSelected(image)
        holder.b.selectedBorder.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
        holder.b.check.visibility = if (on) android.view.View.VISIBLE else android.view.View.GONE
    }

    private companion object { const val PAYLOAD_SELECTION = "selection" }
}

/** The folder list ("Choose folder"). */
class FolderAdapter(private val onClick: (GalleryFolder) -> Unit) : RecyclerView.Adapter<FolderAdapter.Holder>() {
    private var items: List<GalleryFolder> = emptyList()

    class Holder(val b: ItemFolderBinding) : RecyclerView.ViewHolder(b.root)

    fun submit(list: List<GalleryFolder>) { items = list; notifyDataSetChanged() }

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val folder = items[position]
        holder.b.name.text = folder.name
        holder.b.count.text = folder.count.toString()
        val cover = folder.cover
        if (cover != null) ThumbLoader.load(holder.b.cover, cover, 160) else holder.b.cover.setImageDrawable(null)
        holder.b.root.setOnClickListener { onClick(folder) }
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
