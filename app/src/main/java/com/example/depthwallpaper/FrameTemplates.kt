package com.example.depthwallpaper

enum class FrameStyle { POLAROID, TILE }

/**
 * One photo position. x/y are the centre as a fraction of the screen, [w] is the card width as a
 * fraction of the screen width, [aspect] is photo height / photo width.
 */
data class Slot(val x: Float, val y: Float, val rot: Float, val w: Float, val aspect: Float = 1.1f)

data class FrameTemplate(
    val id: String,
    val name: String,
    val style: FrameStyle,
    val slots: List<Slot>,
)

object FrameTemplates {
    private fun grid(cols: Int, rows: Int, w: Float, aspect: Float, y0: Float, dy: Float, rots: FloatArray) =
        buildList {
            val xs = if (cols == 3) floatArrayOf(0.20f, 0.50f, 0.80f) else floatArrayOf(0.30f, 0.70f)
            for (r in 0 until rows) for (c in 0 until cols) {
                add(Slot(xs[c], y0 + dy * r, rots[(r * cols + c) % rots.size], w, aspect))
            }
        }

    val all: List<FrameTemplate> = listOf(
        FrameTemplate(
            "polaroid_grid", "Polaroid grid", FrameStyle.POLAROID,
            grid(3, 3, 0.25f, 1.1f, 0.50f, 0.15f, floatArrayOf(-6f, 3f, -4f, 5f, -3f, 6f, -2f, 4f, -5f)),
        ),
        FrameTemplate(
            "polaroid_duo", "Polaroid duo", FrameStyle.POLAROID,
            listOf(
                Slot(0.42f, 0.46f, -6f, 0.56f, 0.95f),
                Slot(0.60f, 0.72f, 6f, 0.56f, 0.95f),
            ),
        ),
        FrameTemplate(
            "polaroid_scatter", "Polaroid scatter", FrameStyle.POLAROID,
            listOf(
                Slot(0.30f, 0.45f, -8f, 0.36f), Slot(0.70f, 0.50f, 7f, 0.34f),
                Slot(0.27f, 0.64f, 6f, 0.32f), Slot(0.64f, 0.70f, -5f, 0.38f),
                Slot(0.30f, 0.85f, -7f, 0.30f), Slot(0.76f, 0.89f, 8f, 0.28f),
            ),
        ),
        FrameTemplate(
            "tile_grid", "Rounded tiles", FrameStyle.TILE,
            grid(3, 4, 0.29f, 1.0f, 0.40f, 0.15f, floatArrayOf(0f)),
        ),
    )

    fun byId(id: String?): FrameTemplate? = all.firstOrNull { it.id == id }
}
