package com.green3077.motivationlock

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.setPadding
import java.io.File
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * "보물지도" 스타일 콜라주 보드: 사진 여러 장 + 짧은 문구(포스트잇)를 코르크판 위에 살짝 회전된
 * 상태로 흩뿌려서 보여준다. 정교한 드래그 배치 편집기 없이, 아이템 목록(내용)이 같으면 항상 같은
 * 배치가 나오도록 시드를 고정한 난수로 칸을 나눠 배치한다 - 화면을 켤 때마다 레이아웃이 들썩이면
 * 산만하니, 내용이 안 바뀌는 한 배치도 고정해준다.
 */
class TreasureBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    fun showBoard(photos: List<File>, notes: List<String>) {
        doOnLayout {
            layoutItems(photos, notes)
        }
    }

    private fun layoutItems(photos: List<File>, notes: List<String>) {
        removeAllViews()
        val boardWidth = width
        val boardHeight = height
        if (boardWidth <= 0 || boardHeight <= 0) return

        data class Item(val isPhoto: Boolean, val photo: File?, val note: String?)
        val items = photos.map { Item(true, it, null) } + notes.map { Item(false, null, it) }
        if (items.isEmpty()) return

        val seed = items.joinToString("|") { it.photo?.name ?: it.note.orEmpty() }.hashCode()
        val random = Random(seed)

        val columns = ceil(sqrt(items.size.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(items.size.toDouble() / columns).toInt().coerceAtLeast(1)
        val cellWidth = boardWidth / columns
        val cellHeight = boardHeight / rows

        items.forEachIndexed { index, item ->
            val col = index % columns
            val row = index / columns
            val cellLeft = col * cellWidth
            val cellTop = row * cellHeight

            val view = if (item.isPhoto) createPhotoView(item.photo!!, cellWidth, cellHeight) else createNoteView(item.note!!)
            val itemWidth = (cellWidth * 0.8f).toInt()
            val itemHeight = (cellHeight * 0.8f).toInt()

            val jitterX = random.nextInt((cellWidth * 0.15f).toInt().coerceAtLeast(1))
            val jitterY = random.nextInt((cellHeight * 0.15f).toInt().coerceAtLeast(1))
            val rotation = random.nextInt(ROTATION_RANGE_DEGREES * 2) - ROTATION_RANGE_DEGREES

            val params = LayoutParams(itemWidth, itemHeight).apply {
                leftMargin = cellLeft + jitterX
                topMargin = cellTop + jitterY
                gravity = Gravity.TOP or Gravity.START
            }
            view.layoutParams = params
            view.rotation = rotation.toFloat()
            view.elevation = ELEVATION_DP * resources.displayMetrics.density
            addView(view)
        }
    }

    private fun createPhotoView(file: File, cellWidth: Int, cellHeight: Int): ImageView {
        val maxDimension = maxOf(cellWidth, cellHeight)
        val bitmap = ImageUtils.decodeSampledBitmap(file.absolutePath, maxDimension)
        return ImageView(context).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundResource(R.drawable.bg_polaroid)
            setPadding((6 * resources.displayMetrics.density).toInt())
        }
    }

    private fun createNoteView(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setBackgroundResource(R.drawable.bg_sticky_note)
            setTextColor(0xFF4A3C1A.toInt())
            textSize = 15f
            gravity = Gravity.CENTER
        }
    }

    companion object {
        private const val ROTATION_RANGE_DEGREES = 10
        private const val ELEVATION_DP = 4f
    }
}
