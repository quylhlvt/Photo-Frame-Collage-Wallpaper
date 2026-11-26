package com.example.depthwallpaper.onboarding.intro

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.depthwallpaper.databinding.ItemIntroBinding

class IntroAdapter(private val pages: List<IntroModel>) : RecyclerView.Adapter<IntroAdapter.Holder>() {

    class Holder(val binding: ItemIntroBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemIntroBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val page = pages[position]
        holder.binding.image.setImageResource(page.image)
        holder.binding.title.setText(page.title)
        holder.binding.content.setText(page.content)
    }

    override fun getItemCount() = pages.size
}
