package com.green3077.motivationlock

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * ImageView.setImageURI(uri)는 직전에 설정했던 것과 "값이 같은"(경로가 같은) Uri를 다시
 * 넘기면 내부적으로 재디코딩을 건너뛴다(안드로이드 프레임워크의 알려진 동작) - 이 앱은 목표
 * 사진을 항상 같은 파일 경로(filesDir/goal_photo.jpg)에 덮어쓰기 때문에, 사진을 새로 저장해도
 * setImageURI로는 화면이 갱신되지 않는 문제가 있었다. 파일을 직접 비트맵으로 디코딩해서
 * setImageBitmap으로 넣으면 매번 새 Bitmap 객체가 생기므로 이 문제를 피할 수 있다.
 */
object ImageUtils {
    fun decodeSampledBitmap(path: String, maxDimension: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOptions.outWidth / (sampleSize * 2) >= maxDimension &&
            boundsOptions.outHeight / (sampleSize * 2) >= maxDimension
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeFile(path, decodeOptions)
    }
}
