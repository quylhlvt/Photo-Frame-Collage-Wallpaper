package com.example.depthwallpaper.core

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.depthwallpaper.R
import com.example.depthwallpaper.core.dialog.RateDialog
import com.google.android.play.core.review.ReviewManagerFactory

/** The result of the rate dialog, passed to [RateHelper.showRateDialog]'s callback. */
enum class RateState { LOW, HIGH, CANCEL }

object RateHelper {

    /**
     * Shows the dialog. The user's answer is remembered (except "Not now"), then [onResult] is
     * called once everything is finished (for the high-rating case: after the review screen).
     */
    fun showRateDialog(activity: Activity, onResult: (RateState) -> Unit) {
        val dialog = RateDialog(activity)
        dialog.onRateLow = {
            AppPrefs.setRated(activity)
            Toast.makeText(activity, R.string.rate_thanks, Toast.LENGTH_SHORT).show()
            onResult(RateState.LOW)
        }
        dialog.onRateHigh = {
            AppPrefs.setRated(activity)
            reviewApp(activity) { onResult(RateState.HIGH) }
        }
        dialog.onCancel = { onResult(RateState.CANCEL) }
        dialog.show()
    }

    /** Google Play in-app review; falls back to the store page when it isn't available. */
    fun reviewApp(activity: Activity, onDone: () -> Unit) {
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result).addOnCompleteListener { onDone() }
            } else {
                openStore(activity)
                onDone()
            }
        }
    }

    private fun openStore(activity: Activity) {
        val pkg = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")))
        } catch (e: Exception) {
            runCatching {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
            }
        }
    }
}
