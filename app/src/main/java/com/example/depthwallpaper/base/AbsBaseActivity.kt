package com.example.depthwallpaper.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
import com.example.depthwallpaper.core.hideNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Base class for every screen. Inflates the data-binding layout from [getLayoutId], exposes it as
 * [binding], then calls [initView] and [initAction].
 */
abstract class AbsBaseActivity<V : ViewDataBinding> : AppCompatActivity() {
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

    /** true: status bar icons are drawn dark (light screens). */
    protected open val lightStatusBar: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideNavigation(isBlack = lightStatusBar)
        if (inflateAsync) {
            setContentView(FrameLayout(this).apply {
                if (placeholderBackground != 0) setBackgroundResource(placeholderBackground)
            })
            lifecycleScope.launch {
                val view = withContext(Dispatchers.Default) {
                    LayoutInflater.from(this@AbsBaseActivity).inflate(getLayoutId(), null)
                }
                binding = DataBindingUtil.bind<V>(view)
                    ?: throw IllegalStateException("Layout ${getLayoutId()} is not a data binding layout")
                binding.lifecycleOwner = this@AbsBaseActivity
                setContentView(view)
                applySystemBarPadding()
                initView()
                initAction()
                onBindingReady()
            }
        } else {
            binding = DataBindingUtil.setContentView(this, getLayoutId())
            // LiveData used in layout expressions (@{vm.x}) update the views while the screen is alive.
            binding.lifecycleOwner = this
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

    /**
     * The window draws behind the status bar and cutout (see hideNavigation), so the screen's root
     * view gets their size added to its own padding. Layouts therefore keep their own padding.
     */
    private fun applySystemBarPadding() {
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        val l = root.paddingLeft
        val t = root.paddingTop
        val r = root.paddingRight
        val b = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.setPadding(l + bars.left, t + bars.top, r + bars.right, b + bars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    abstract fun getLayoutId(): Int
    abstract fun initView()
    abstract fun initAction()

    /** dp -> px helper shared by screens that build parts of their UI in code. */
    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
