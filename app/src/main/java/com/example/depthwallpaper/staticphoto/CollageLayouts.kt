package com.example.depthwallpaper.staticphoto

import android.graphics.RectF
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * A static collage layout. [cells] are rectangles in unit space (0..1) covering the collage area.
 * [fullBleed] layouts use the whole screen with no margins/gaps (a plain full-screen photo).
 */
data class CollageLayout(val id: String, val count: Int, val cells: List<RectF>, val fullBleed: Boolean = false)

/** Background + cell styling. [gap], [corner] and [border] are fractions of the screen width. */
data class CollageTheme(
    val id: String, val name: String, val top: Int, val bottom: Int,
    val gap: Float, val corner: Float, val border: Float,
)

/** A ready-made entry on the DIY screen. */
data class CollagePreset(val count: Int, val key: String, val themeId: String)

object Collages {
    const val MAX_PHOTOS = 10

    val themes = listOf(
        CollageTheme("dark", "Dark", 0xFF2B2B2B.toInt(), 0xFF1A1A1A.toInt(), 0.012f, 0.02f, 0f),
        CollageTheme("light", "Light", 0xFFFFFFFF.toInt(), 0xFFEFEFEF.toInt(), 0.012f, 0.02f, 0f),
        CollageTheme("pink", "Pink", 0xFFFFD6E5.toInt(), 0xFFFFA9C9.toInt(), 0.016f, 0.012f, 0.012f),
        CollageTheme("sunset", "Sunset", 0xFFFF9A8B.toInt(), 0xFF6A3093.toInt(), 0.014f, 0.03f, 0f),
        CollageTheme("ocean", "Ocean", 0xFF89F7FE.toInt(), 0xFF66A6FF.toInt(), 0.014f, 0.03f, 0f),
        CollageTheme("mint", "Mint", 0xFFD4FC79.toInt(), 0xFF96E6A1.toInt(), 0.014f, 0.025f, 0f),
        CollageTheme("lavender", "Lavender", 0xFFE0C3FC.toInt(), 0xFF8EC5FC.toInt(), 0.016f, 0.012f, 0.01f),
        CollageTheme("mono", "Mono", 0xFF000000.toInt(), 0xFF000000.toInt(), 0.006f, 0f, 0f),
    )

    fun theme(id: String?): CollageTheme? = themes.firstOrNull { it.id == id }

    val presets = listOf(
        CollagePreset(1, "full", "dark"),
        CollagePreset(2, "stack", "pink"),
        CollagePreset(2, "cols", "ocean"),
        CollagePreset(3, "herol", "dark"),
        CollagePreset(4, "grid", "sunset"),
        CollagePreset(5, "hero_top", "mint"),
        CollagePreset(6, "grid", "lavender"),
        CollagePreset(9, "grid", "mono"),
    )

    private val cache = HashMap<Int, List<CollageLayout>>()

    /** All layouts suggested for [n] photos (1..10). */
    fun layoutsFor(n: Int): List<CollageLayout> = cache.getOrPut(n.coerceIn(1, MAX_PHOTOS)) { build(n.coerceIn(1, MAX_PHOTOS)) }

    fun find(n: Int, key: String): CollageLayout = layoutsFor(n).firstOrNull { it.id == "$n-$key" } ?: layoutsFor(n)[0]

    fun layout(id: String?): CollageLayout? {
        val n = id?.substringBefore('-')?.toIntOrNull() ?: return null
        return layoutsFor(n).firstOrNull { it.id == id }
    }

    // ---- builders ----
    private fun balanced(n: Int, parts: Int): List<Int> {
        val base = n / parts
        val extra = n % parts
        return List(parts) { if (it < extra) base + 1 else base }
    }

    /** Horizontal bands; band r holds counts[r] equal cells. Optional band height [weights]. */
    private fun rowsOf(counts: List<Int>, weights: List<Float>? = null): List<RectF> {
        val total = weights?.sum() ?: counts.size.toFloat()
        var y = 0f
        val out = ArrayList<RectF>()
        counts.forEachIndexed { r, k ->
            val h = (weights?.get(r) ?: 1f) / total
            for (i in 0 until k) out.add(RectF(i / k.toFloat(), y, (i + 1) / k.toFloat(), y + h))
            y += h
        }
        return out
    }

    /** Vertical bands; column c holds counts[c] stacked cells. */
    private fun colsOf(counts: List<Int>): List<RectF> =
        rowsOf(counts).map { RectF(it.top, it.left, it.bottom, it.right) }

    private fun build(n: Int): List<CollageLayout> {
        val list = ArrayList<CollageLayout>()
        fun add(key: String, cells: List<RectF>, bleed: Boolean = false) {
            if (list.none { it.cells == cells && it.fullBleed == bleed }) list.add(CollageLayout("$n-$key", n, cells, bleed))
        }
        if (n == 1) {
            add("full", listOf(RectF(0f, 0f, 1f, 1f)), bleed = true)
            add("single", listOf(RectF(0f, 0f, 1f, 1f)))
            return list
        }
        if (n <= 6) add("stack", rowsOf(List(n) { 1 }))
        if (n <= 5) add("cols", colsOf(List(n) { 1 }))
        val c = ceil(sqrt(n * 0.5)).toInt().coerceAtLeast(1)
        val r = ceil(n / c.toFloat()).toInt()
        add("grid", rowsOf(balanced(n, r)))
        add("gridrev", rowsOf(balanced(n, r).reversed()))
        if (n >= 4) add("colsbal", colsOf(balanced(n, if (n >= 7) 3 else 2)))
        if (n >= 3) {
            val side = (0 until n - 1).map { RectF(0.58f, it / (n - 1f), 1f, (it + 1) / (n - 1f)) }
            add("herol", listOf(RectF(0f, 0f, 0.58f, 1f)) + side)
            val rest = rowsOf(balanced(n - 1, ceil((n - 1) / 3f).toInt()))
                .map { RectF(it.left, 0.4f + it.top * 0.6f, it.right, 0.4f + it.bottom * 0.6f) }
            add("hero_top", listOf(RectF(0f, 0f, 1f, 0.4f)) + rest)
        }
        if (n == 2) add("uneven", rowsOf(listOf(1, 1), listOf(0.4f, 0.6f)))
        return list
    }
}
