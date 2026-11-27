package com.example.depthwallpaper.base

import android.content.Context
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import androidx.lifecycle.lifecycleScope
import com.example.depthwallpaper.core.dp
import com.example.depthwallpaper.core.hideNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base class for every screen. Inflates the view-binding class given as [bindingInflater] (for
 * example `ActivityHomeBinding::inflate`), exposes it as [binding], then calls [initView] and
 * [initAction].
 */
abstract class AbsBaseActivity<V : ViewBinding>(
    private val bindingInflater: (LayoutInflater) -> V,
) : AppCompatActivity() {
    lateinit var binding: V

    /**
     * Opt in for screens with a very large layout: the layout is inflated on a background thread so
     * the screen appears at once (showing [placeholderBackground]) instead of freezing the transition.
     * [initView]/[initAction]/[onBindingReady] then run as soon as the view is ready.
     */
    protected open val inflateAsync: Boolean = false

    @DrawableRes
    protected open val placeholderBackground: Int = 0

    /** Called once [binding] is ready; only needed when [inflateAsync] is true. */
    protected open fun onBindingReady() {}

    override fun onResume() {
        super.onResume()
        // Back from the system wallpaper screen: make sure the lock screen follows our wallpaper.
        com.example.depthwallpaper.core.WallpaperUi.syncLockScreen(this)
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.depthwallpaper.core.LanguageManager.wrap(newBase))
    }

    /**
     * Back button/gesture. Return true when the screen handled it (for example by closing the whole
     * app); false lets the screen simply close.
     */
    protected open fun handleBackPressed(): Boolean = false

    /** true: status bar icons are drawn dark (light screens). */
    protected open val lightStatusBar: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideNavigation(isBlack = lightStatusBar)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!handleBackPressed()) finish()
            }
        })
        if (inflateAsync) {
            setContentView(FrameLayout(this).apply {
                if (placeholderBackground != 0) setBackgroundResource(placeholderBackground)
            })
            lifecycleScope.launch {
                binding = withContext(Dispatchers.Default) { bindingInflater(layoutInflater) }
                setContentView(binding.root)
                applySystemBarPadding()
                initView()
                initAction()
                onBindingReady()
            }
        } else {
            binding = bindingInflater(layoutInflater)
            setContentView(binding.root)
            applySystemBarPadding()
            initView()
            initAction()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // The navigation bar can reappear after a dialog or the system picker: hide it again.
        if (hasFocus) hideNavigation(isBlack = lightStatusBar)
    }

    /** true: the content's top edge (e.g. a full-bleed image) goes behind the status bar instead of below it. */
    protected open val drawBehindStatusBar: Boolean = false

    /** Which view gets the system bar padding; null (default) means the screen's root view. */
    protected open fun insetsTarget(): android.view.View? = null

    /**
     * The window draws behind the status bar and cutout (see hideNavigation), so the screen's root
     * view gets their size added to its own padding. Layouts therefore keep their own padding.
     */
    private fun applySystemBarPadding() {
        val root = insetsTarget() ?: findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        val l = root.paddingLeft
        val t = root.paddingTop
        val r = root.paddingRight
        val b = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(l + bars.left, if (drawBehindStatusBar) t else t + bars.top, r + bars.right, b + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    abstract fun initView()
    abstract fun initAction()

    /** dp -> px helper shared by screens that build parts of their UI in code (scaled up on tablets). */
    protected fun dp(v: Int) = this.dp(v.toFloat()).toInt()
}
