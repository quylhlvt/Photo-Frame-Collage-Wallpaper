package com.example.depthwallpaper.base

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.lifecycleScope
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                setContentView(view)
                initView()
                initAction()
                onBindingReady()
            }
        } else {
            binding = DataBindingUtil.setContentView(this, getLayoutId())
            initView()
            initAction()
        }
    }

    abstract fun getLayoutId(): Int
    abstract fun initView()
    abstract fun initAction()

    /** dp -> px helper shared by screens that build parts of their UI in code. */
    protected fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
