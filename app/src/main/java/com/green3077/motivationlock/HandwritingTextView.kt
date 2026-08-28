package com.green3077.motivationlock

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatTextView

/**
 * 목표 문구를 한 글자씩 드러내며 "지금 손글씨로 써내려가는 중"처럼 보이게 하는 TextView.
 *
 * v1 범위: 글자 단위 타이핑 효과(+ 손글씨 폰트)까지만 구현한다. 실제 붓/펜 획이 곡선을 따라
 * 그어지는 진짜 스트로크 애니메이션(글자마다 벡터 경로 데이터가 필요)은 훨씬 복잡해서 다음
 * 단계 개선 과제로 남겨둔다.
 */
class HandwritingTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var animator: ValueAnimator? = null

    fun playHandwriting(fullText: String, durationMs: Long = DEFAULT_DURATION_MS) {
        animator?.cancel()
        text = ""
        if (fullText.isEmpty()) return

        animator = ValueAnimator.ofInt(0, fullText.length).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { anim ->
                val revealedCount = anim.animatedValue as Int
                text = fullText.substring(0, revealedCount)
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        animator = null
    }

    companion object {
        private const val DEFAULT_DURATION_MS = 3500L
    }
}
