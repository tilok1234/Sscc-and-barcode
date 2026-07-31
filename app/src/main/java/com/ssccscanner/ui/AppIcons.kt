package com.ssccscanner.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-authored outline icons matching the handoff's style: 24×24 viewBox,
 * ~1.7px stroke, round caps, no fill. Icon() tint recolors them.
 */
object AppIcons {

    val Camera: ImageVector by lazy {
        strokeIcon("Camera") {
            // body
            moveTo(3.5f, 8.0f)
            curveTo(3.5f, 7.17f, 4.17f, 6.5f, 5.0f, 6.5f)
            lineTo(7.5f, 6.5f)
            lineTo(9.0f, 4.5f)
            lineTo(15.0f, 4.5f)
            lineTo(16.5f, 6.5f)
            lineTo(19.0f, 6.5f)
            curveTo(19.83f, 6.5f, 20.5f, 7.17f, 20.5f, 8.0f)
            lineTo(20.5f, 17.5f)
            curveTo(20.5f, 18.33f, 19.83f, 19.0f, 19.0f, 19.0f)
            lineTo(5.0f, 19.0f)
            curveTo(4.17f, 19.0f, 3.5f, 18.33f, 3.5f, 17.5f)
            close()
            // lens
            moveTo(15.2f, 12.5f)
            curveTo(15.2f, 14.27f, 13.77f, 15.7f, 12.0f, 15.7f)
            curveTo(10.23f, 15.7f, 8.8f, 14.27f, 8.8f, 12.5f)
            curveTo(8.8f, 10.73f, 10.23f, 9.3f, 12.0f, 9.3f)
            curveTo(13.77f, 9.3f, 15.2f, 10.73f, 15.2f, 12.5f)
            close()
        }
    }

    val Document: ImageVector by lazy {
        strokeIcon("Document") {
            moveTo(7.0f, 3.5f)
            lineTo(14.0f, 3.5f)
            lineTo(19.0f, 8.5f)
            lineTo(19.0f, 20.5f)
            lineTo(7.0f, 20.5f)
            close()
            moveTo(14.0f, 3.5f)
            lineTo(14.0f, 8.5f)
            lineTo(19.0f, 8.5f)
            moveTo(10.0f, 13.0f)
            lineTo(16.0f, 13.0f)
            moveTo(10.0f, 16.5f)
            lineTo(16.0f, 16.5f)
        }
    }

    val Folder: ImageVector by lazy {
        strokeIcon("Folder") {
            moveTo(3.5f, 6.5f)
            curveTo(3.5f, 5.95f, 3.95f, 5.5f, 4.5f, 5.5f)
            lineTo(9.0f, 5.5f)
            lineTo(11.0f, 7.5f)
            lineTo(19.5f, 7.5f)
            curveTo(20.05f, 7.5f, 20.5f, 7.95f, 20.5f, 8.5f)
            lineTo(20.5f, 17.5f)
            curveTo(20.5f, 18.05f, 20.05f, 18.5f, 19.5f, 18.5f)
            lineTo(4.5f, 18.5f)
            curveTo(3.95f, 18.5f, 3.5f, 18.05f, 3.5f, 17.5f)
            close()
        }
    }

    val Gallery: ImageVector by lazy {
        strokeIcon("Gallery") {
            moveTo(4.0f, 5.5f)
            lineTo(20.0f, 5.5f)
            lineTo(20.0f, 18.5f)
            lineTo(4.0f, 18.5f)
            close()
            // sun
            moveTo(9.4f, 9.6f)
            curveTo(9.4f, 10.26f, 8.86f, 10.8f, 8.2f, 10.8f)
            curveTo(7.54f, 10.8f, 7.0f, 10.26f, 7.0f, 9.6f)
            curveTo(7.0f, 8.94f, 7.54f, 8.4f, 8.2f, 8.4f)
            curveTo(8.86f, 8.4f, 9.4f, 8.94f, 9.4f, 9.6f)
            close()
            // mountains
            moveTo(4.5f, 16.5f)
            lineTo(9.5f, 11.5f)
            lineTo(13.0f, 15.0f)
            lineTo(15.5f, 12.5f)
            lineTo(19.5f, 16.5f)
        }
    }

    val ChevronDown: ImageVector by lazy {
        strokeIcon("ChevronDown") {
            moveTo(6.0f, 9.5f)
            lineTo(12.0f, 15.5f)
            lineTo(18.0f, 9.5f)
        }
    }

    val Pencil: ImageVector by lazy {
        strokeIcon("Pencil") {
            moveTo(4.0f, 20.0f)
            lineTo(4.6f, 16.4f)
            lineTo(16.2f, 4.8f)
            curveTo(16.98f, 4.02f, 18.25f, 4.02f, 19.03f, 4.8f)
            curveTo(19.81f, 5.58f, 19.81f, 6.85f, 19.03f, 7.63f)
            lineTo(7.43f, 19.23f)
            close()
            moveTo(14.5f, 6.5f)
            lineTo(17.3f, 9.3f)
        }
    }

    val Check: ImageVector by lazy {
        strokeIcon("Check") {
            moveTo(5.0f, 12.5f)
            lineTo(10.0f, 17.5f)
            lineTo(19.0f, 6.5f)
        }
    }

    val Close: ImageVector by lazy {
        strokeIcon("Close") {
            moveTo(6.5f, 6.5f)
            lineTo(17.5f, 17.5f)
            moveTo(17.5f, 6.5f)
            lineTo(6.5f, 17.5f)
        }
    }

    val Back: ImageVector by lazy {
        strokeIcon("Back") {
            moveTo(14.5f, 5.5f)
            lineTo(8.0f, 12.0f)
            lineTo(14.5f, 18.5f)
        }
    }

    val Share: ImageVector by lazy {
        strokeIcon("Share") {
            moveTo(12.0f, 3.5f)
            lineTo(12.0f, 14.0f)
            moveTo(8.0f, 7.0f)
            lineTo(12.0f, 3.5f)
            lineTo(16.0f, 7.0f)
            moveTo(5.5f, 11.5f)
            lineTo(5.5f, 20.0f)
            lineTo(18.5f, 20.0f)
            lineTo(18.5f, 11.5f)
        }
    }

    val Alert: ImageVector by lazy {
        strokeIcon("Alert") {
            // triangle
            moveTo(12.0f, 4.0f)
            lineTo(21.0f, 20.0f)
            lineTo(3.0f, 20.0f)
            close()
            // exclamation
            moveTo(12.0f, 10.5f)
            lineTo(12.0f, 14.5f)
            moveTo(12.0f, 17.0f)
            lineTo(12.0f, 17.1f)
        }
    }

    val Sliders: ImageVector by lazy {
        strokeIcon("Sliders") {
            moveTo(4.0f, 7.0f); lineTo(20.0f, 7.0f)
            moveTo(14.5f, 5.2f); lineTo(14.5f, 8.8f)
            moveTo(4.0f, 12.0f); lineTo(20.0f, 12.0f)
            moveTo(8.5f, 10.2f); lineTo(8.5f, 13.8f)
            moveTo(4.0f, 17.0f); lineTo(20.0f, 17.0f)
            moveTo(16.5f, 15.2f); lineTo(16.5f, 18.8f)
        }
    }

    val Plus: ImageVector by lazy {
        strokeIcon("Plus") {
            moveTo(12.0f, 5.5f)
            lineTo(12.0f, 18.5f)
            moveTo(5.5f, 12.0f)
            lineTo(18.5f, 12.0f)
        }
    }

    val Copy: ImageVector by lazy {
        strokeIcon("Copy") {
            moveTo(9.0f, 9.0f)
            lineTo(20.0f, 9.0f)
            lineTo(20.0f, 20.0f)
            lineTo(9.0f, 20.0f)
            close()
            moveTo(5.0f, 15.0f)
            lineTo(4.0f, 15.0f)
            lineTo(4.0f, 4.0f)
            lineTo(15.0f, 4.0f)
            lineTo(15.0f, 5.0f)
        }
    }

    val Grid: ImageVector by lazy {
        strokeIcon("Grid") {
            moveTo(4.5f, 4.5f); lineTo(10.5f, 4.5f); lineTo(10.5f, 10.5f); lineTo(4.5f, 10.5f); close()
            moveTo(13.5f, 4.5f); lineTo(19.5f, 4.5f); lineTo(19.5f, 10.5f); lineTo(13.5f, 10.5f); close()
            moveTo(4.5f, 13.5f); lineTo(10.5f, 13.5f); lineTo(10.5f, 19.5f); lineTo(4.5f, 19.5f); close()
            moveTo(13.5f, 13.5f); lineTo(19.5f, 13.5f); lineTo(19.5f, 19.5f); lineTo(13.5f, 19.5f); close()
        }
    }

    val Calendar: ImageVector by lazy {
        strokeIcon("Calendar") {
            // frame
            moveTo(4.0f, 6.5f)
            lineTo(20.0f, 6.5f)
            lineTo(20.0f, 20.0f)
            lineTo(4.0f, 20.0f)
            close()
            // binding pins
            moveTo(8.0f, 4.0f); lineTo(8.0f, 8.5f)
            moveTo(16.0f, 4.0f); lineTo(16.0f, 8.5f)
            // header rule
            moveTo(4.0f, 11.0f); lineTo(20.0f, 11.0f)
        }
    }

    val Tag: ImageVector by lazy {
        strokeIcon("Tag") {
            // tag body
            moveTo(4.0f, 4.0f)
            lineTo(11.5f, 4.0f)
            lineTo(20.0f, 12.5f)
            lineTo(12.5f, 20.0f)
            lineTo(4.0f, 11.5f)
            close()
            // eyelet
            moveTo(8.3f, 8.0f)
            curveTo(8.3f, 8.17f, 8.17f, 8.3f, 8.0f, 8.3f)
            curveTo(7.83f, 8.3f, 7.7f, 8.17f, 7.7f, 8.0f)
            curveTo(7.7f, 7.83f, 7.83f, 7.7f, 8.0f, 7.7f)
            curveTo(8.17f, 7.7f, 8.3f, 7.83f, 8.3f, 8.0f)
            close()
        }
    }

    private fun strokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                block()
            }
        }.build()
}
