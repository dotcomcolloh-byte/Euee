package com.telefam.company.app.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object TelefamIcons {

    val Mail: ImageVector by lazy {
        lucideIcon("Mail") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 4f)
                horizontalLineTo(20f)
                curveTo(21.1f, 4f, 22f, 4.9f, 22f, 6f)
                verticalLineTo(18f)
                curveTo(22f, 19.1f, 21.1f, 20f, 20f, 20f)
                horizontalLineTo(4f)
                curveTo(2.9f, 20f, 2f, 19.1f, 2f, 18f)
                verticalLineTo(6f)
                curveTo(2f, 4.9f, 2.9f, 4f, 4f, 4f)
                close()
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(22f, 6f)
                lineTo(12f, 13f)
                lineTo(2f, 6f)
            }
        }
    }

    val ArrowLeft: ImageVector by lazy {
        lucideIcon("ArrowLeft") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(19f, 12f)
                horizontalLineTo(5f)
                moveTo(12f, 19f)
                lineTo(5f, 12f)
                lineTo(12f, 5f)
            }
        }
    }

    val Camera: ImageVector by lazy {
        lucideIcon("Camera") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14.5f, 4f)
                horizontalLineTo(9.5f)
                lineTo(7f, 7f)
                horizontalLineTo(4f)
                curveTo(2.9f, 7f, 2f, 7.9f, 2f, 9f)
                verticalLineTo(18f)
                curveTo(2f, 19.1f, 2.9f, 20f, 4f, 20f)
                horizontalLineTo(20f)
                curveTo(21.1f, 20f, 22f, 19.1f, 22f, 18f)
                verticalLineTo(9f)
                curveTo(22f, 7.9f, 21.1f, 7f, 20f, 7f)
                horizontalLineTo(17f)
                close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                moveTo(12f, 10f)
                arcTo(3.5f, 3.5f, 0f, true, true, 11.99f, 10f)
            }
        }
    }

    val Plus: ImageVector by lazy {
        lucideIcon("Plus") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.4f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 5f)
                verticalLineTo(19f)
                moveTo(5f, 12f)
                horizontalLineTo(19f)
            }
        }
    }

    val User: ImageVector by lazy {
        lucideIcon("User") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 21f)
                verticalLineTo(19f)
                curveTo(20f, 16.79f, 18.21f, 15f, 16f, 15f)
                horizontalLineTo(8f)
                curveTo(5.79f, 15f, 4f, 16.79f, 4f, 19f)
                verticalLineTo(21f)
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                moveTo(12f, 3f)
                arcTo(4f, 4f, 0f, true, true, 11.99f, 3f)
            }
        }
    }

    val Users: ImageVector by lazy {
        lucideIcon("Users") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 21f)
                verticalLineTo(19f)
                curveTo(16f, 17.34f, 14.66f, 16f, 13f, 16f)
                horizontalLineTo(5f)
                curveTo(3.34f, 16f, 2f, 17.34f, 2f, 19f)
                verticalLineTo(21f)
                moveTo(9f, 4f)
                arcTo(3.5f, 3.5f, 0f, true, true, 8.99f, 4f)
                moveTo(22f, 21f)
                verticalLineTo(19f)
                curveTo(22f, 17.6f, 21.04f, 16.4f, 19.7f, 16.08f)
                moveTo(16f, 4.13f)
                curveTo(17.75f, 4.58f, 19f, 6.17f, 19f, 8f)
                curveTo(19f, 9.83f, 17.75f, 11.42f, 16f, 11.87f)
            }
        }
    }

    val AtSign: ImageVector by lazy {
        lucideIcon("AtSign") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, true, true, 11.99f, 8f)
                moveTo(16f, 8f)
                verticalLineTo(13f)
                curveTo(16f, 14.66f, 17.34f, 16f, 19f, 16f)
                curveTo(20.66f, 16f, 22f, 14.5f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(14.5f, 22f, 16.8f, 21.1f, 18.6f, 19.6f)
            }
        }
    }

    val Phone: ImageVector by lazy {
        lucideIcon("Phone") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(22f, 16.92f)
                verticalLineTo(19.92f)
                curveTo(22f, 21.02f, 21.08f, 21.92f, 19.98f, 21.84f)
                curveTo(10.86f, 21.1f, 3.6f, 13.84f, 2.86f, 4.72f)
                curveTo(2.78f, 3.62f, 3.68f, 2.7f, 4.78f, 2.7f)
                horizontalLineTo(7.78f)
                curveTo(8.7f, 2.7f, 9.48f, 3.36f, 9.64f, 4.26f)
                lineTo(10.22f, 7.5f)
                curveTo(10.35f, 8.22f, 10.02f, 8.95f, 9.39f, 9.33f)
                lineTo(7.9f, 10.22f)
                curveTo(9.35f, 12.92f, 11.78f, 15.35f, 14.48f, 16.8f)
                lineTo(15.37f, 15.31f)
                curveTo(15.75f, 14.68f, 16.48f, 14.35f, 17.2f, 14.48f)
                lineTo(20.44f, 15.06f)
                curveTo(21.34f, 15.22f, 22f, 16f, 22f, 16.92f)
                close()
            }
        }
    }

    val Calendar: ImageVector by lazy {
        lucideIcon("Calendar") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 4f)
                horizontalLineTo(19f)
                curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
                verticalLineTo(20f)
                curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                verticalLineTo(6f)
                curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
                close()
                moveTo(16f, 2f)
                verticalLineTo(6f)
                moveTo(8f, 2f)
                verticalLineTo(6f)
                moveTo(3f, 10f)
                horizontalLineTo(21f)
            }
        }
    }

    val MapPin: ImageVector by lazy {
        lucideIcon("MapPin") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(20f, 10f)
                curveTo(20f, 16f, 12f, 22f, 12f, 22f)
                curveTo(12f, 22f, 4f, 16f, 4f, 10f)
                curveTo(4f, 5.58f, 7.58f, 2f, 12f, 2f)
                curveTo(16.42f, 2f, 20f, 5.58f, 20f, 10f)
                close()
            }
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
                moveTo(12f, 7f)
                arcTo(3f, 3f, 0f, true, true, 11.99f, 7f)
            }
        }
    }

    val MessageSquare: ImageVector by lazy {
        lucideIcon("MessageSquare") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21f, 15f)
                curveTo(21f, 16.1f, 20.1f, 17f, 19f, 17f)
                horizontalLineTo(7f)
                lineTo(3f, 21f)
                verticalLineTo(5f)
                curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
                close()
            }
        }
    }

    val ChevronDown: ImageVector by lazy {
        lucideIcon("ChevronDown") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }
    }

    val Search: ImageVector by lazy {
        lucideIcon("Search") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(11f, 3f)
                arcTo(8f, 8f, 0f, true, true, 10.99f, 3f)
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }
    }

    val Bell: ImageVector by lazy {
        lucideIcon("Bell") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(18f, 8f)
                curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
                curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                horizontalLineTo(21f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                moveTo(13.73f, 21f)
                curveTo(13.37f, 21.62f, 12.71f, 22f, 12f, 22f)
                curveTo(11.29f, 22f, 10.63f, 21.62f, 10.27f, 21f)
            }
        }
    }

    val MoreVertical: ImageVector by lazy {
        lucideIcon("MoreVertical") {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 4f)
                arcTo(1.5f, 1.5f, 0f, true, true, 11.99f, 4f)
                moveTo(12f, 10.5f)
                arcTo(1.5f, 1.5f, 0f, true, true, 11.99f, 10.5f)
                moveTo(12f, 17f)
                arcTo(1.5f, 1.5f, 0f, true, true, 11.99f, 17f)
            }
        }
    }

    val Sun: ImageVector by lazy {
        lucideIcon("Sun") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 8f)
                arcTo(4f, 4f, 0f, true, true, 11.99f, 8f)
                moveTo(12f, 2f)
                verticalLineTo(4f)
                moveTo(12f, 20f)
                verticalLineTo(22f)
                moveTo(4.93f, 4.93f)
                lineTo(6.34f, 6.34f)
                moveTo(17.66f, 17.66f)
                lineTo(19.07f, 19.07f)
                moveTo(2f, 12f)
                horizontalLineTo(4f)
                moveTo(20f, 12f)
                horizontalLineTo(22f)
                moveTo(6.34f, 17.66f)
                lineTo(4.93f, 19.07f)
                moveTo(19.07f, 4.93f)
                lineTo(17.66f, 6.34f)
            }
        }
    }

    val Moon: ImageVector by lazy {
        lucideIcon("Moon") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21f, 12.79f)
                arcTo(9f, 9f, 0f, true, true, 11.21f, 3f)
                arcTo(7f, 7f, 0f, false, false, 21f, 12.79f)
                close()
            }
        }
    }

    val Monitor: ImageVector by lazy {
        lucideIcon("Monitor") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 3f)
                horizontalLineTo(20f)
                curveTo(21.1f, 3f, 22f, 3.9f, 22f, 5f)
                verticalLineTo(15f)
                curveTo(22f, 16.1f, 21.1f, 17f, 20f, 17f)
                horizontalLineTo(4f)
                curveTo(2.9f, 17f, 2f, 16.1f, 2f, 15f)
                verticalLineTo(5f)
                curveTo(2f, 3.9f, 2.9f, 3f, 4f, 3f)
                close()
                moveTo(8f, 21f)
                horizontalLineTo(16f)
                moveTo(12f, 17f)
                verticalLineTo(21f)
            }
        }
    }

    val Messages: ImageVector by lazy {
        lucideIcon("Messages") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14f, 9f)
                curveTo(14f, 10.1f, 13.1f, 11f, 12f, 11f)
                horizontalLineTo(6f)
                lineTo(2f, 15f)
                verticalLineTo(4f)
                curveTo(2f, 2.9f, 2.9f, 2f, 4f, 2f)
                horizontalLineTo(12f)
                curveTo(13.1f, 2f, 14f, 2.9f, 14f, 4f)
                close()
                moveTo(18f, 9f)
                horizontalLineTo(20f)
                curveTo(21.1f, 9f, 22f, 9.9f, 22f, 11f)
                verticalLineTo(22f)
                lineTo(18f, 18f)
                horizontalLineTo(11f)
                curveTo(9.9f, 18f, 9f, 17.1f, 9f, 16f)
                verticalLineTo(15f)
            }
        }
    }

    val Layers: ImageVector by lazy {
        lucideIcon("Layers") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2f)
                lineTo(2f, 7f)
                lineTo(12f, 12f)
                lineTo(22f, 7f)
                close()
                moveTo(2f, 12f)
                lineTo(12f, 17f)
                lineTo(22f, 12f)
                moveTo(2f, 17f)
                lineTo(12f, 22f)
                lineTo(22f, 17f)
            }
        }
    }

    val MessagePlus: ImageVector by lazy {
        lucideIcon("MessagePlus") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(21f, 15f)
                curveTo(21f, 16.1f, 20.1f, 17f, 19f, 17f)
                horizontalLineTo(7f)
                lineTo(3f, 21f)
                verticalLineTo(5f)
                curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f)
                horizontalLineTo(19f)
                curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
                close()
                moveTo(12f, 7f)
                verticalLineTo(13f)
                moveTo(9f, 10f)
                horizontalLineTo(15f)
            }
        }
    }

    val CheckCircle: ImageVector by lazy {
        lucideIcon("CheckCircle") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, true, 11.99f, 2f)
                moveTo(9f, 12f)
                lineTo(11f, 14f)
                lineTo(15f, 10f)
            }
        }
    }

    val Globe: ImageVector by lazy {
        lucideIcon("Globe") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 2f)
                arcTo(10f, 10f, 0f, true, true, 11.99f, 2f)
                moveTo(2f, 12f)
                horizontalLineTo(22f)
                moveTo(12f, 2f)
                curveTo(14.5f, 4.7f, 15.8f, 8.2f, 15.8f, 12f)
                curveTo(15.8f, 15.8f, 14.5f, 19.3f, 12f, 22f)
                curveTo(9.5f, 19.3f, 8.2f, 15.8f, 8.2f, 12f)
                curveTo(8.2f, 8.2f, 9.5f, 4.7f, 12f, 2f)
            }
        }
    }

    val UserPlus: ImageVector by lazy {
        lucideIcon("UserPlus") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 21f)
                verticalLineTo(19f)
                curveTo(16f, 16.79f, 14.21f, 15f, 12f, 15f)
                horizontalLineTo(5f)
                curveTo(2.79f, 15f, 1f, 16.79f, 1f, 19f)
                verticalLineTo(21f)
                moveTo(8.5f, 3f)
                arcTo(4f, 4f, 0f, true, true, 8.49f, 3f)
                moveTo(20f, 8f)
                verticalLineTo(14f)
                moveTo(17f, 11f)
                horizontalLineTo(23f)
            }
        }
    }

    val UserCheck: ImageVector by lazy {
        lucideIcon("UserCheck") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(16f, 21f)
                verticalLineTo(19f)
                curveTo(16f, 16.79f, 14.21f, 15f, 12f, 15f)
                horizontalLineTo(5f)
                curveTo(2.79f, 15f, 1f, 16.79f, 1f, 19f)
                verticalLineTo(21f)
                moveTo(8.5f, 3f)
                arcTo(4f, 4f, 0f, true, true, 8.49f, 3f)
                moveTo(17f, 11f)
                lineTo(19f, 13f)
                lineTo(23f, 9f)
            }
        }
    }

    val Crown: ImageVector by lazy {
        lucideIcon("Crown") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(2f, 19f)
                horizontalLineTo(22f)
                moveTo(3f, 15f)
                lineTo(2f, 6f)
                lineTo(7.5f, 10.5f)
                lineTo(12f, 4f)
                lineTo(16.5f, 10.5f)
                lineTo(22f, 6f)
                lineTo(21f, 15f)
                close()
            }
        }
    }

    val Settings: ImageVector by lazy {
        lucideIcon("Settings") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 9f)
                arcTo(3f, 3f, 0f, true, true, 11.99f, 9f)
                moveTo(19.4f, 15f)
                arcTo(1.65f, 1.65f, 0f, false, false, 19.73f, 16.82f)
                lineTo(19.79f, 16.88f)
                arcTo(2f, 2f, 0f, true, true, 16.96f, 19.71f)
                lineTo(16.9f, 19.65f)
                arcTo(1.65f, 1.65f, 0f, false, false, 15.08f, 19.32f)
                arcTo(1.65f, 1.65f, 0f, false, false, 14.08f, 20.83f)
                verticalLineTo(21f)
                arcTo(2f, 2f, 0f, true, true, 10.08f, 21f)
                verticalLineTo(20.91f)
                arcTo(1.65f, 1.65f, 0f, false, false, 9f, 19.4f)
                arcTo(1.65f, 1.65f, 0f, false, false, 7.18f, 19.73f)
                lineTo(7.12f, 19.79f)
                arcTo(2f, 2f, 0f, true, true, 4.29f, 16.96f)
                lineTo(4.35f, 16.9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 4.68f, 15.08f)
                arcTo(1.65f, 1.65f, 0f, false, false, 3.17f, 14.08f)
                horizontalLineTo(3f)
                arcTo(2f, 2f, 0f, true, true, 3f, 10.08f)
                horizontalLineTo(3.09f)
                arcTo(1.65f, 1.65f, 0f, false, false, 4.6f, 9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 4.27f, 7.18f)
                lineTo(4.21f, 7.12f)
                arcTo(2f, 2f, 0f, true, true, 7.04f, 4.29f)
                lineTo(7.1f, 4.35f)
                arcTo(1.65f, 1.65f, 0f, false, false, 8.92f, 4.68f)
                horizontalLineTo(9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 10f, 3.17f)
                verticalLineTo(3f)
                arcTo(2f, 2f, 0f, true, true, 14f, 3f)
                verticalLineTo(3.09f)
                arcTo(1.65f, 1.65f, 0f, false, false, 15f, 4.6f)
                arcTo(1.65f, 1.65f, 0f, false, false, 16.82f, 4.27f)
                lineTo(16.88f, 4.21f)
                arcTo(2f, 2f, 0f, true, true, 19.71f, 7.04f)
                lineTo(19.65f, 7.1f)
                arcTo(1.65f, 1.65f, 0f, false, false, 19.32f, 8.92f)
                verticalLineTo(9f)
                arcTo(1.65f, 1.65f, 0f, false, false, 20.83f, 10f)
                horizontalLineTo(21f)
                arcTo(2f, 2f, 0f, true, true, 21f, 14f)
                horizontalLineTo(20.91f)
                arcTo(1.65f, 1.65f, 0f, false, false, 19.4f, 15f)
                close()
            }
        }
    }

    val Menu: ImageVector by lazy {
        lucideIcon("Menu") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                moveTo(4f, 12f)
                horizontalLineTo(20f)
                moveTo(4f, 18f)
                horizontalLineTo(20f)
            }
        }
    }

    val Pencil: ImageVector by lazy {
        lucideIcon("Pencil") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(17f, 3f)
                arcTo(2.83f, 2.83f, 0f, true, true, 21f, 7f)
                lineTo(7.5f, 20.5f)
                lineTo(2f, 22f)
                lineTo(3.5f, 16.5f)
                close()
            }
        }
    }

    val Share2: ImageVector by lazy {
        lucideIcon("Share2") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(18f, 3f)
                arcTo(3f, 3f, 0f, true, true, 17.99f, 3f)
                moveTo(6f, 9f)
                arcTo(3f, 3f, 0f, true, true, 5.99f, 9f)
                moveTo(18f, 15f)
                arcTo(3f, 3f, 0f, true, true, 17.99f, 15f)
                moveTo(8.59f, 13.51f)
                lineTo(15.42f, 17.49f)
                moveTo(15.41f, 6.51f)
                lineTo(8.59f, 10.49f)
            }
        }
    }

    val LayoutGrid: ImageVector by lazy {
        lucideIcon("LayoutGrid") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 3f)
                horizontalLineTo(10f)
                verticalLineTo(10f)
                horizontalLineTo(3f)
                close()
                moveTo(14f, 3f)
                horizontalLineTo(21f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
                close()
                moveTo(14f, 14f)
                horizontalLineTo(21f)
                verticalLineTo(21f)
                horizontalLineTo(14f)
                close()
                moveTo(3f, 14f)
                horizontalLineTo(10f)
                verticalLineTo(21f)
                horizontalLineTo(3f)
                close()
            }
        }
    }

    val Bookmark: ImageVector by lazy {
        lucideIcon("Bookmark") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(19f, 21f)
                lineTo(12f, 16f)
                lineTo(5f, 21f)
                verticalLineTo(5f)
                curveTo(5f, 3.9f, 5.9f, 3f, 7f, 3f)
                horizontalLineTo(17f)
                curveTo(18.1f, 3f, 19f, 3.9f, 19f, 5f)
                close()
            }
        }
    }

    val Repeat2: ImageVector by lazy {
        lucideIcon("Repeat2") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(2f, 9f)
                lineTo(5f, 6f)
                lineTo(8f, 9f)
                moveTo(5f, 6f)
                verticalLineTo(15f)
                curveTo(5f, 16.66f, 6.34f, 18f, 8f, 18f)
                horizontalLineTo(14f)
                moveTo(22f, 15f)
                lineTo(19f, 18f)
                lineTo(16f, 15f)
                moveTo(19f, 18f)
                verticalLineTo(9f)
                curveTo(19f, 7.34f, 17.66f, 6f, 16f, 6f)
                horizontalLineTo(10f)
            }
        }
    }

    val Lock: ImageVector by lazy {
        lucideIcon("Lock") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(5f, 11f)
                horizontalLineTo(19f)
                curveTo(20.1f, 11f, 21f, 11.9f, 21f, 13f)
                verticalLineTo(20f)
                curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
                verticalLineTo(13f)
                curveTo(3f, 11.9f, 3.9f, 11f, 5f, 11f)
                close()
                moveTo(7f, 11f)
                verticalLineTo(7f)
                arcTo(5f, 5f, 0f, true, true, 17f, 7f)
                verticalLineTo(11f)
            }
        }
    }

    val SlidersHorizontal: ImageVector by lazy {
        lucideIcon("SlidersHorizontal") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round) {
                moveTo(21f, 4f)
                horizontalLineTo(14f)
                moveTo(10f, 4f)
                horizontalLineTo(3f)
                moveTo(21f, 12f)
                horizontalLineTo(12f)
                moveTo(8f, 12f)
                horizontalLineTo(3f)
                moveTo(21f, 20f)
                horizontalLineTo(16f)
                moveTo(12f, 20f)
                horizontalLineTo(3f)
                moveTo(14f, 2f)
                verticalLineTo(6f)
                moveTo(8f, 10f)
                verticalLineTo(14f)
                moveTo(16f, 18f)
                verticalLineTo(22f)
            }
        }
    }

    val Trash2: ImageVector by lazy {
        lucideIcon("Trash2") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(3f, 6f)
                horizontalLineTo(21f)
                moveTo(19f, 6f)
                verticalLineTo(20f)
                curveTo(19f, 21.1f, 18.1f, 22f, 17f, 22f)
                horizontalLineTo(7f)
                curveTo(5.9f, 22f, 5f, 21.1f, 5f, 20f)
                verticalLineTo(6f)
                moveTo(8f, 6f)
                verticalLineTo(4f)
                curveTo(8f, 2.9f, 8.9f, 2f, 10f, 2f)
                horizontalLineTo(14f)
                curveTo(15.1f, 2f, 16f, 2.9f, 16f, 4f)
                verticalLineTo(6f)
                moveTo(10f, 11f)
                verticalLineTo(17f)
                moveTo(14f, 11f)
                verticalLineTo(17f)
            }
        }
    }

    val X: ImageVector by lazy {
        lucideIcon("X") {
            path(stroke = SolidColor(Color.Black), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }
    }

    private fun lucideIcon(
        name: String,
        builder: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(builder).build()
}

@Composable
fun GoogleLogoVector(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.19f
        val inset = stroke * 0.6f
        val arcSize = Size(w - inset * 2, h - inset * 2)
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFFEA4335),
            startAngle = -145f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 145f,
            sweepAngle = 70f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 105f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -5f,
            sweepAngle = 50f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(w * 0.50f, h * 0.50f),
            end = Offset(w * 0.92f, h * 0.50f),
            strokeWidth = stroke
        )
    }
}

@Composable
fun CountryFlagBadge(iso2: String, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 24.dp, height = 16.dp)) {
        val w = size.width
        val h = size.height
        when (iso2.uppercase()) {
            "KE" -> {
                drawRect(Color(0xFF111111), Offset(0f, 0f), Size(w, h * 0.28f))
                drawRect(Color.White, Offset(0f, h * 0.28f), Size(w, h * 0.08f))
                drawRect(Color(0xFFBB161D), Offset(0f, h * 0.36f), Size(w, h * 0.28f))
                drawRect(Color.White, Offset(0f, h * 0.64f), Size(w, h * 0.08f))
                drawRect(Color(0xFF106B2E), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
                drawOval(Color(0xFFBB161D), Offset(w * 0.41f, h * 0.25f), Size(w * 0.18f, h * 0.50f))
            }
            "US", "CA" -> {
                drawRect(Color(0xFFB22234), Offset.Zero, Size(w, h))
                for (i in 1..5 step 2) {
                    drawRect(Color.White, Offset(0f, h * i / 7f), Size(w, h / 7f))
                }
                drawRect(Color(0xFF3C3B6E), Offset.Zero, Size(w * 0.45f, h * 0.55f))
            }
            "GB" -> {
                drawRect(Color(0xFF012169), Offset.Zero, Size(w, h))
                drawRect(Color.White, Offset(w * 0.40f, 0f), Size(w * 0.20f, h))
                drawRect(Color.White, Offset(0f, h * 0.36f), Size(w, h * 0.28f))
                drawRect(Color(0xFFC8102E), Offset(w * 0.44f, 0f), Size(w * 0.12f, h))
                drawRect(Color(0xFFC8102E), Offset(0f, h * 0.42f), Size(w, h * 0.16f))
            }
            "NG" -> {
                drawRect(Color(0xFF008751), Offset.Zero, Size(w / 3f, h))
                drawRect(Color.White, Offset(w / 3f, 0f), Size(w / 3f, h))
                drawRect(Color(0xFF008751), Offset(2f * w / 3f, 0f), Size(w / 3f, h))
            }
            else -> {
                val p = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
                }
                drawPath(p, Color(0xFFE5E7EB))
                drawRect(Color(0xFFC80E18), Offset(0f, 0f), Size(w, h * 0.33f))
                drawRect(Color(0xFF1F2937), Offset(0f, h * 0.67f), Size(w, h * 0.33f))
            }
        }
    }
}
