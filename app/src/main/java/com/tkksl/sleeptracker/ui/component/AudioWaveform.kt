package com.tkksl.sleeptracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveform(
    waveformData: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color,
    lineWidth: Float = 2f
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
    ) {
        if (waveformData.isEmpty()) return@Canvas
        val pointCount = waveformData.size
        val segmentWidth = size.width / pointCount

        val points = waveformData.mapIndexed { index, volume ->
            val x = index * segmentWidth
            val maxHeight = size.height / 2
            val y = maxHeight - (volume * maxHeight)
            Offset(x, y)
        }

        for (i in 0 until points.size - 1) {
            drawLine(
                start = points[i],
                end = points[i + 1],
                color = lineColor,
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )
        }
    }
}