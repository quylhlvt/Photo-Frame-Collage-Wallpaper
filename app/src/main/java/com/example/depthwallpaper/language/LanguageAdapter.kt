package com.example.depthwallpaper.language

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.LanguageManager.Language
import com.example.depthwallpaper.databinding.ItemLanguageBinding

class LanguageAdapter(
    private val languages: List<Language>,
    private val onPick: (Language) -> Unit,
) : RecyclerView.Adapter<LanguageAdapter.Holder>() {

    private var selectedTag = ""

    class Holder(val binding: ItemLanguageBinding) : RecyclerView.ViewHolder(binding.root)

    fun setSelected(tag: String) {
        selectedTag = tag
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val language = languages[position]
        val selected = language.tag == selectedTag
        val b = holder.binding
        b.flag.text = language.flag
        b.name.text = language.name
        b.name.setTextColor(if (selected) Color.WHITE else 0xFF2B2B3A.toInt())
        b.radio.setImageResource(if (selected) R.drawable.ic_radio_on else R.drawable.ic_radio_off)
        b.itemRoot.setBackgroundResource(if (selected) R.drawable.bg_language_selected else R.drawable.bg_language)
        b.itemRoot.elevation = if (selected) 0f else b.root.resources.displayMetrics.density * 4f
        b.root.setOnClickListener { onPick(language) }
    }

    override fun getItemCount() = languages.size
}
