package com.example.depthwallpaper.core

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.databinding.InverseBindingAdapter
import androidx.databinding.InverseBindingListener
import androidx.viewpager2.widget.ViewPager2

/** Custom attributes usable in layouts: `app:visible="@{flag}"`, `app:bitmap="@{vm.image}"`, ... */
object BindingAdapters {

    /** Shows the view when [show] is true, hides it (GONE) otherwise. */
    @JvmStatic
    @BindingAdapter("visible")
    fun setVisible(view: View, show: Boolean?) {
        view.visibility = if (show == true) View.VISIBLE else View.GONE
    }

    @JvmStatic
    @BindingAdapter("bitmap")
    fun setBitmap(view: ImageView, bitmap: Bitmap?) {
        if (bitmap != null) view.setImageBitmap(bitmap)
    }

    // Two-way binding of a ViewPager2's current page: app:page="@={vm.page}".
    @JvmStatic
    @BindingAdapter("page")
    fun setPage(pager: ViewPager2, page: Int?) {
        if (page != null && pager.currentItem != page) pager.setCurrentItem(page, true)
    }

    @JvmStatic
    @InverseBindingAdapter(attribute = "page")
    fun getPage(pager: ViewPager2): Int = pager.currentItem

    @JvmStatic
    @BindingAdapter("pageAttrChanged")
    fun setPageListener(pager: ViewPager2, listener: InverseBindingListener?) {
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) { listener?.onChange() }
        })
    }
}
