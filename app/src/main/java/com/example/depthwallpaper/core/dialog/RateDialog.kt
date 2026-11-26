package com.example.depthwallpaper.core.dialog

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.depthwallpaper.R
import com.example.depthwallpaper.databinding.DialogRateBinding

/**
 * "Rate us" dialog: a face, a title and a line that change with the stars picked, a Rate button and
 * Not now. Results come back through the callbacks.
 */
class RateDialog(private val activity: Activity) : Dialog(activity), DefaultLifecycleObserver {
    private lateinit var binding: DialogRateBinding

    /** 4 or 5 stars: the caller opens the store review. */
    var onRateHigh: (() -> Unit)? = null

    /** 1 to 3 stars. */
    var onRateLow: (() -> Unit)? = null

    /** Not now / closed. */
    var onCancel: (() -> Unit)? = null

    private var rating = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super<Dialog>.onCreate(savedInstanceState)
        binding = DialogRateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setCancelable(false)
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        showRating()
        binding.stars.setOnRatingBarChangeListener { _, value, _ ->
            rating = value.toInt()
            showRating()
        }
        binding.btnVote.setOnClickListener { onVote() }
        binding.btnNotNow.setOnClickListener { onNotNow() }
    }

    /** The face, title and line for the stars picked so far. */
    private fun showRating() {
        binding.emoji.text = EMOJIS[rating]
        binding.title.setText(TITLES[rating])
        binding.description.setText(DESCRIPTIONS[rating])
    }

    /** Rate button. */
    private fun onVote() {
        if (rating == 0) {
            Toast.makeText(context, R.string.rate_pick_star, Toast.LENGTH_SHORT).show()
            return
        }
        dismiss()
        if (rating <= 3) onRateLow?.invoke() else onRateHigh?.invoke()
    }

    /** Not now button. */
    private fun onNotNow() {
        dismiss()
        onCancel?.invoke()
    }

    override fun show() {
        if (activity.isFinishing || activity.isDestroyed) return
        (activity as? LifecycleOwner)?.lifecycle?.addObserver(this)
        super.show()
    }

    override fun dismiss() {
        (activity as? LifecycleOwner)?.lifecycle?.removeObserver(this)
        if (isShowing) super.dismiss()
    }

    override fun onDestroy(owner: LifecycleOwner) = dismiss()

    private companion object {
        val EMOJIS = listOf("\uD83D\uDE36", "\uD83D\uDE2D", "\uD83D\uDE1E", "\uD83D\uDE10", "\uD83D\uDE42", "\uD83D\uDE0D")
        val TITLES = listOf(
            R.string.rate_title_0, R.string.rate_title_1, R.string.rate_title_2,
            R.string.rate_title_3, R.string.rate_title_4, R.string.rate_title_5,
        )
        val DESCRIPTIONS = listOf(
            R.string.rate_desc_0, R.string.rate_desc_1, R.string.rate_desc_2,
            R.string.rate_desc_3, R.string.rate_desc_4, R.string.rate_desc_5,
        )
    }
}
