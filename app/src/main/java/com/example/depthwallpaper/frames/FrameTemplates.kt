package com.example.depthwallpaper.frames


enum class FrameStyle {
    POLAROID, TILE,
    /** White polaroid with a handwritten caption under the photo. */
    CAPTION,
    /** Dark-grey polaroid with a date written in the bottom border. */
    DARK,
    /** Capsule-shaped photo with a gold border. */
    PILL,
    /** Photo with no border at all. */
    PLAIN,
    /** Round photo. */
    CIRCLE,
    /** Slanted parallelogram photo. */
    SKEW,
    /** Thin dark-grey border on all sides. */
    THIN_DARK,
    /** Thin white border on all sides. */
    THIN_WHITE,
}

/** What fills the wallpaper behind the photos, on top of the background colour. */
enum class BgStyle { SOLID, PAPER, GRADIENT }

enum class DecorKind { CLOUD, FLOWER, HEART, SPARKLE, VINYL, LIPS, PANEL }

/**
 * One photo position. x/y are the centre as a fraction of the screen, [w] is the card width as a
 * fraction of the screen width, [aspect] is photo height / photo width.
 */
data class Slot(
    val x: Float, val y: Float, val rot: Float, val w: Float, val aspect: Float = 1.1f,
    val caption: String? = null,
)

/** Handwritten text floating on the wallpaper. [size] is a fraction of the screen width. */
data class Label(val text: String, val x: Float, val y: Float, val size: Float, val color: Int, val rot: Float = 0f)

/**
 * A drawn decoration. x/y are the centre and [w] the width as fractions of the screen; [h] is only
 * used by PANEL (a rounded rectangle). [color] 0 means the kind's own colours.
 */
data class Decor(
    val kind: DecorKind, val x: Float, val y: Float, val w: Float,
    val rot: Float = 0f, val flip: Boolean = false, val h: Float = 0f, val color: Int = 0,
)

data class FrameTemplate(
    val id: String,
    val name: String,
    val style: FrameStyle,
    val slots: List<Slot>,
    val defaultBg: Int = FrameRenderer.BG_COLOR,
    val labels: List<Label> = emptyList(),
    val decor: List<Decor> = emptyList(),
    val bgStyle: BgStyle = BgStyle.SOLID,
    /** Text repeated over the whole background (e.g. "I love you"). */
    val bgPattern: String? = null,
    val bgPatternColor: Int = 0,
    val captionColor: Int = 0,
)

object FrameTemplates {
    private const val CREAM = 0xFFFFFFF5.toInt()
    private const val PINK_TEXT = 0xFFF58CDE.toInt()
    private const val DARK = 0xFF444444.toInt()

    private fun heartSlots(): List<Slot> {
        val p = arrayOf(
            floatArrayOf(0.27f, 0.43f, -6f), floatArrayOf(0.355f, 0.37f, -12f), floatArrayOf(0.44f, 0.40f, 8f),
            floatArrayOf(0.60f, 0.385f, -10f), floatArrayOf(0.73f, 0.355f, -14f), floatArrayOf(0.765f, 0.43f, 5f),
            floatArrayOf(0.50f, 0.465f, 0f), floatArrayOf(0.30f, 0.52f, -8f), floatArrayOf(0.68f, 0.50f, 6f),
            floatArrayOf(0.40f, 0.56f, -4f), floatArrayOf(0.57f, 0.585f, 3f), floatArrayOf(0.49f, 0.65f, 3f),
        )
        return p.map { Slot(it[0], it[1], it[2], 0.19f, 0.85f) }
    }

    private fun lips() = listOf(
        Decor(DecorKind.LIPS, 0.07f, 0.02f, 0.22f), Decor(DecorKind.LIPS, 0.52f, 0.03f, 0.30f, -4f),
        Decor(DecorKind.LIPS, 0.95f, 0.01f, 0.22f, 8f), Decor(DecorKind.LIPS, 0.50f, 0.15f, 0.36f, 3f),
        Decor(DecorKind.LIPS, 0.95f, 0.14f, 0.20f, -10f), Decor(DecorKind.LIPS, 0.06f, 0.14f, 0.18f, 14f),
        Decor(DecorKind.LIPS, 0.12f, 0.27f, 0.20f, -6f), Decor(DecorKind.LIPS, 0.52f, 0.27f, 0.30f, 0f),
        Decor(DecorKind.LIPS, 0.96f, 0.27f, 0.22f, 6f),
        Decor(DecorKind.LIPS, 0.05f, 0.45f, 0.18f, 12f), Decor(DecorKind.LIPS, 0.96f, 0.43f, 0.18f, -12f),
        Decor(DecorKind.LIPS, 0.08f, 0.66f, 0.20f, 4f), Decor(DecorKind.LIPS, 0.95f, 0.64f, 0.20f, -4f),
        Decor(DecorKind.LIPS, 0.12f, 0.84f, 0.26f, -8f), Decor(DecorKind.LIPS, 0.52f, 0.84f, 0.30f, 3f),
        Decor(DecorKind.LIPS, 0.92f, 0.82f, 0.22f, 6f), Decor(DecorKind.LIPS, 0.15f, 0.96f, 0.30f, 0f),
        Decor(DecorKind.LIPS, 0.55f, 0.97f, 0.30f, -6f), Decor(DecorKind.LIPS, 0.95f, 0.95f, 0.24f, 8f),
        // Darker small ones at the card's corners.
        Decor(DecorKind.LIPS, 0.20f, 0.575f, 0.16f, -8f, color = 0xFF8E1B1B.toInt()),
        Decor(DecorKind.LIPS, 0.83f, 0.575f, 0.16f, 8f, color = 0xFF8E1B1B.toInt()),
    )

    val all: List<FrameTemplate> = listOf(
        FrameTemplate(
            "garden", "Flower garden", FrameStyle.POLAROID,
            listOf(Slot(0.42f, 0.47f, 15f, 0.68f, 0.63f), Slot(0.58f, 0.77f, -14f, 0.60f, 0.63f)),
            defaultBg = 0xFFF1F1F1.toInt(), bgStyle = BgStyle.PAPER,
            decor = listOf(
                Decor(DecorKind.FLOWER, 0.17f, 0.335f, 0.20f), Decor(DecorKind.FLOWER, 0.79f, 0.60f, 0.18f, 20f),
                Decor(DecorKind.FLOWER, 0.12f, 0.93f, 0.28f, -10f), Decor(DecorKind.FLOWER, 0.24f, 0.955f, 0.17f, 15f),
                Decor(DecorKind.HEART, 0.78f, 0.28f, 0.10f, 12f, color = 0xFFD94B3F.toInt()),
            ),
        ),
        FrameTemplate(
            "booth", "Photo booth", FrameStyle.PLAIN,
            listOf(
                Slot(0.265f, 0.205f, 0f, 0.355f, 1.64f), Slot(0.265f, 0.505f, 0f, 0.355f, 1.64f), Slot(0.265f, 0.81f, 0f, 0.355f, 1.64f),
                Slot(0.665f, 0.17f, 0f, 0.355f, 1.36f), Slot(0.665f, 0.45f, 0f, 0.355f, 1.62f), Slot(0.665f, 0.76f, 0f, 0.355f, 1.62f),
            ),
            defaultBg = 0xFFE9DED8.toInt(), bgStyle = BgStyle.PAPER,
        ),
        FrameTemplate(
            "heart", "Heart collage", FrameStyle.THIN_DARK, heartSlots(),
            defaultBg = 0xFFEDE4DD.toInt(), bgStyle = BgStyle.PAPER,
            labels = listOf(Label("The Beauty Of Love", 0.50f, 0.86f, 0.075f, 0xFF222222.toInt())),
        ),
        FrameTemplate(
            "kisses", "All my love", FrameStyle.CAPTION,
            listOf(Slot(0.50f, 0.56f, 0f, 0.62f, 1.28f, "All my love for you")),
            defaultBg = 0xFFFFFFFF.toInt(), captionColor = 0xFFD32F2F.toInt(), decor = lips(),
        ),
        FrameTemplate(
            "music", "Music", FrameStyle.POLAROID,
            listOf(Slot(0.33f, 0.60f, -12f, 0.42f, 1.05f), Slot(0.66f, 0.76f, -8f, 0.42f, 1.0f)),
            defaultBg = 0xFFE8EEFB.toInt(),
            decor = listOf(
                Decor(DecorKind.VINYL, 0.52f, 0.595f, 0.46f),
                Decor(DecorKind.SPARKLE, 0.11f, 0.485f, 0.09f, color = 0xFF8EC5DD.toInt()),
                Decor(DecorKind.SPARKLE, 0.28f, 0.865f, 0.11f, color = 0xFF8EC5DD.toInt()),
                Decor(DecorKind.SPARKLE, 0.93f, 0.60f, 0.05f, color = 0xFF8EC5DD.toInt()),
            ),
        ),
        FrameTemplate(
            "iloveyou", "I love you", FrameStyle.POLAROID,
            listOf(
                Slot(0.29f, 0.47f, 0f, 0.34f, 1.05f), Slot(0.71f, 0.47f, 0f, 0.34f, 1.05f),
                Slot(0.29f, 0.765f, 0f, 0.34f, 1.05f), Slot(0.71f, 0.765f, 0f, 0.34f, 1.05f),
            ),
            defaultBg = 0xFFFFFBE8.toInt(), bgPattern = "I love you", bgPatternColor = 0xFFF28B8B.toInt(),
        ),
        FrameTemplate(
            "rows", "Hearts rows", FrameStyle.PLAIN,
            listOf(
                Slot(0.265f, 0.404f, 0f, 0.255f, 0.95f), Slot(0.535f, 0.404f, 0f, 0.255f, 0.95f),
                Slot(0.485f, 0.593f, 0f, 0.255f, 0.95f), Slot(0.755f, 0.593f, 0f, 0.255f, 0.95f),
                Slot(0.265f, 0.785f, 0f, 0.255f, 0.95f), Slot(0.535f, 0.785f, 0f, 0.255f, 0.95f),
            ),
            defaultBg = 0xFFFAD5E0.toInt(),
            decor = listOf(
                Decor(DecorKind.PANEL, 0.40f, 0.404f, 0.57f, h = 0.135f, color = DARK),
                Decor(DecorKind.PANEL, 0.62f, 0.593f, 0.57f, h = 0.135f, color = DARK),
                Decor(DecorKind.PANEL, 0.40f, 0.785f, 0.57f, h = 0.135f, color = DARK),
                Decor(DecorKind.HEART, 0.10f, 0.97f, 0.12f, -10f, color = 0xFFE96B6B.toInt()),
                Decor(DecorKind.HEART, 0.26f, 0.95f, 0.15f, 8f, color = 0xFFF2A0B6.toInt()),
                Decor(DecorKind.HEART, 0.50f, 0.975f, 0.10f, 0f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.HEART, 0.63f, 0.915f, 0.18f, -12f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.HEART, 0.82f, 0.90f, 0.30f, 10f, color = 0xFFE05A55.toInt()),
                Decor(DecorKind.HEART, 0.93f, 0.965f, 0.13f, 0f, color = 0xFFE96B6B.toInt()),
                Decor(DecorKind.HEART, 0.90f, 0.74f, 0.09f, 6f, color = 0xFFF2B8A8.toInt()),
                Decor(DecorKind.HEART, 0.89f, 0.45f, 0.10f, 6f, color = 0xFFF7C2CE.toInt()),
            ),
        ),
        FrameTemplate(
            "strips", "Slanted strips", FrameStyle.SKEW,
            listOf(
                Slot(0.50f, 0.455f, 0f, 0.66f, 0.50f), Slot(0.50f, 0.627f, 0f, 0.66f, 0.50f), Slot(0.50f, 0.793f, 0f, 0.66f, 0.50f),
            ),
            defaultBg = 0xFFCFE3F8.toInt(), bgStyle = BgStyle.GRADIENT,
            decor = listOf(
                Decor(DecorKind.SPARKLE, 0.10f, 0.095f, 0.20f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.SPARKLE, 0.78f, 0.13f, 0.16f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.SPARKLE, 0.72f, 0.24f, 0.11f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.SPARKLE, 0.05f, 0.40f, 0.07f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.SPARKLE, 0.04f, 0.86f, 0.08f, color = 0xFFFFFFFF.toInt()),
            ),
        ),
        FrameTemplate(
            "soft", "Soft frame", FrameStyle.THIN_WHITE,
            listOf(Slot(0.50f, 0.555f, 0f, 0.52f, 1.45f)),
            defaultBg = 0xFFF5DDD9.toInt(),
            decor = listOf(
                Decor(DecorKind.SPARKLE, 0.28f, 0.865f, 0.06f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.SPARKLE, 0.50f, 0.90f, 0.05f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.SPARKLE, 0.72f, 0.865f, 0.06f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.HEART, 0.39f, 0.84f, 0.05f, color = 0xFFFFFFFF.toInt()),
                Decor(DecorKind.HEART, 0.62f, 0.84f, 0.05f, color = 0xFFFFFFFF.toInt()),
            ),
        ),
        FrameTemplate(
            "circles", "Circles", FrameStyle.CIRCLE,
            listOf(Slot(0.37f, 0.48f, 0f, 0.51f, 1.0f), Slot(0.63f, 0.74f, 0f, 0.51f, 1.0f)),
            defaultBg = 0xFFF6C5EC.toInt(),
        ),
        FrameTemplate(
            "caption_single", "Best memory", FrameStyle.CAPTION,
            listOf(Slot(0.50f, 0.56f, 0f, 0.80f, 1.40f, "Best memory")),
        ),
        FrameTemplate(
            "pill_clouds", "Pink clouds", FrameStyle.PILL,
            listOf(Slot(0.50f, 0.58f, 0f, 0.64f, 1.95f)),
            defaultBg = CREAM,
            decor = listOf(
                Decor(DecorKind.CLOUD, 0.17f, 0.285f, 0.34f), Decor(DecorKind.CLOUD, 0.83f, 0.275f, 0.30f, flip = true),
            ),
            labels = listOf(Label("The Beauty", 0.50f, 0.89f, 0.075f, PINK_TEXT)),
        ),
        FrameTemplate(
            "dark_duo", "Love memory", FrameStyle.DARK,
            listOf(
                Slot(0.34f, 0.42f, -12f, 0.50f, 1.05f, "23.03.25"),
                Slot(0.62f, 0.69f, 12f, 0.50f, 1.05f, "23.03.25"),
            ),
            defaultBg = CREAM,
            labels = listOf(
                Label("Love Memory", 0.74f, 0.42f, 0.085f, PINK_TEXT, -4f),
                Label("Our Love", 0.17f, 0.64f, 0.085f, PINK_TEXT, 3f),
            ),
        ),
    )

    fun byId(id: String?): FrameTemplate? = all.firstOrNull { it.id == id }
}
