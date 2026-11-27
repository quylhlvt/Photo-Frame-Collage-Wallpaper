package com.example.depthwallpaper.main.project

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.show
import com.example.depthwallpaper.databinding.ItemProjectBinding

class ProjectAdapter(
    private val onMenu: (Project, View) -> Unit,
    private val onToggle: (Project) -> Unit,
    private val onLongPress: (Project) -> Unit,
) : RecyclerView.Adapter<ProjectAdapter.Holder>() {

    private var items: List<Project> = emptyList()
    private var selecting = false
    private var selected: Set<String> = emptySet()

    class Holder(val binding: ItemProjectBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(list: List<Project>, selecting: Boolean, selected: Set<String>) {
        items = list
        this.selecting = selecting
        this.selected = selected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val p = items[position]
        val b = holder.binding
        b.thumb.setImageBitmap(BitmapFactory.decodeFile(p.thumbPath))
        b.btnMore.show(!selecting)
        b.btnCheck.show(selecting)
        b.btnCheck.setImageResource(if (p.id in selected) R.drawable.ic_check_on else R.drawable.ic_check_off)
        b.root.setOnClickListener { if (selecting) onToggle(p) else onMenu(p, b.btnMore) }
        b.root.setOnLongClickListener { if (!selecting) onLongPress(p); true }
        b.btnMore.setOnClickListener { onMenu(p, b.btnMore) }
    }

    override fun getItemCount() = items.size
}
