package com.bj.fishingdiary.presentation.seawatertemperature

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.roundToInt

/**
 * 수온 라인 그래프 커스텀 뷰
 * Temperature Line Graph Custom View
 *
 * iOS의 ZeddLineGraph에 대응
 * Corresponds to ZeddLineGraph in iOS
 *
 * 역할:
 * Role:
 * - Canvas를 사용하여 수온 그래프 그리기
 * - Draw temperature graph using Canvas
 * - 배경 그리드 라인 및 온도 레이블 표시
 * - Display background grid lines and temperature labels
 * - 최고/최저 온도 표시
 * - Display maximum/minimum temperature
 *
 * @param context Android Context
 * @param attrs XML 속성
 */
class TemperatureLineGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ==================== 데이터 ====================

    /**
     * 온도 값 리스트
     * Temperature values list
     */
    private var values: List<Float> = emptyList()

    /**
     * 그래프 선 색상
     * Graph line color
     */
    private var lineColor: Int = Color.BLUE

    // ==================== Paint 객체 ====================

    /**
     * 그래프 선 Paint
     * Graph line paint
     */
    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * 배경 그리드 선 Paint
     * Background grid line paint
     */
    private val gridLinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.LTGRAY
        isAntiAlias = true
    }

    /**
     * 텍스트 Paint
     * Text paint
     */
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
    }

    /**
     * 최고/최저 온도 레이블 Paint
     * Max/Min temperature label paint
     */
    private val labelPaint = Paint().apply {
        color = Color.DKGRAY
        textSize = 28f
        isAntiAlias = true
    }

    // ==================== Public Methods ====================

    /**
     * 그래프 데이터 설정
     * Set graph data
     *
     * @param values 온도 값 리스트
     * @param color 그래프 선 색상
     */
    fun setData(values: List<Float>, color: Int = Color.BLUE) {
        this.values = values
        this.lineColor = color
        this.linePaint.color = color
        invalidate() // 다시 그리기
    }

    // ==================== Drawing ====================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 유효한 온도 값 필터링 (0보다 큰 값)
        // Filter valid temperature values (greater than 0)
        val validValues = values.filter { it > 0 }

        if (validValues.isEmpty()) {
            drawEmptyMessage(canvas)
            return
        }

        // 그래프 그리기
        // Draw graph
        drawBackgroundGrid(canvas, validValues)
        drawLineGraph(canvas, validValues)
        drawTemperatureLabels(canvas, validValues)
    }

    /**
     * 배경 그리드 그리기
     * Draw background grid
     */
    private fun drawBackgroundGrid(canvas: Canvas, validValues: List<Float>) {
        val min = validValues.minOrNull()?.toInt()?.minus(1) ?: return
        val max = validValues.maxOrNull()?.toInt()?.plus(1) ?: return
        val lineCount = max - min + 1
        val spacing = height.toFloat() / (lineCount - 1)

        for (i in 0 until lineCount) {
            val y = i * spacing
            val temp = max - i

            // 가로선 그리기
            // Draw horizontal line
            canvas.drawLine(40f, y, width.toFloat(), y, gridLinePaint)

            // 온도 레이블 그리기
            // Draw temperature label
            canvas.drawText(
                temp.toString(),
                5f,
                y + 10f,
                textPaint
            )
        }
    }

    /**
     * 라인 그래프 그리기
     * Draw line graph
     */
    private fun drawLineGraph(canvas: Canvas, validValues: List<Float>) {
        if (values.isEmpty()) return

        val min = validValues.minOrNull()?.toInt()?.minus(1) ?: return
        val max = validValues.maxOrNull()?.toInt()?.plus(1) ?: return
        val lineCount = max - min + 1
        val graphWeight = height.toFloat() / (lineCount - 1)
        val xOffset = width.toFloat() / values.size

        val path = Path()
        var currentX = 0f
        var validPoint = false

        for ((index, value) in values.withIndex()) {
            currentX = index * xOffset

            when {
                value >= 0 -> {
                    // 유효한 데이터 포인트
                    // Valid data point
                    val y = height - ((value - min) * graphWeight)
                    val newPosition = Pair(currentX, y)

                    if (!validPoint) {
                        path.moveTo(newPosition.first, newPosition.second)
                    } else {
                        path.lineTo(newPosition.first, newPosition.second)
                    }
                    validPoint = true
                }
                value == -90f -> {
                    // 중간에 시간 누락된 데이터
                    // Missing time data in the middle
                    if (validPoint && path.isEmpty.not()) {
                        // 같은 y 좌표로 수평선 그리기
                        // Draw horizontal line with same y coordinate
                        path.lineTo(currentX, path.currentPoint.y)
                    }
                }
                else -> {
                    // 오늘 날짜 더미 데이터
                    // Today's dummy data
                    validPoint = false
                }
            }
        }

        canvas.drawPath(path, linePaint)
    }

    /**
     * 최고/최저 온도 레이블 그리기
     * Draw max/min temperature labels
     */
    private fun drawTemperatureLabels(canvas: Canvas, validValues: List<Float>) {
        val maxTemp = validValues.maxOrNull() ?: return
        val minTemp = validValues.minOrNull() ?: return

        // 최고 온도 레이블
        // Max temperature label
        val maxText = String.format("주간최고: %.1f", maxTemp)
        val maxTextWidth = labelPaint.measureText(maxText)
        canvas.drawText(
            maxText,
            width - maxTextWidth - 10f,
            30f,
            labelPaint
        )

        // 최저 온도 레이블
        // Min temperature label
        val minText = String.format("주간최저: %.1f", minTemp)
        val minTextWidth = labelPaint.measureText(minText)
        canvas.drawText(
            minText,
            width - minTextWidth - 10f,
            height - 10f,
            labelPaint
        )
    }

    /**
     * 데이터 없음 메시지 그리기
     * Draw empty message
     */
    private fun drawEmptyMessage(canvas: Canvas) {
        val message = "수온정보가 없습니다."
        val textWidth = textPaint.measureText(message)
        canvas.drawText(
            message,
            (width - textWidth) / 2,
            height / 2f,
            textPaint
        )
    }
}

/**
 * Path의 현재 포인트 가져오기 확장 프로퍼티
 * Extension property to get current point of Path
 */
private val Path.currentPoint: android.graphics.PointF
    get() {
        val point = floatArrayOf(0f, 0f)
        val pathMeasure = android.graphics.PathMeasure(this, false)
        pathMeasure.getPosTan(pathMeasure.length, point, null)
        return android.graphics.PointF(point[0], point[1])
    }
