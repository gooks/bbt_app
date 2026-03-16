package com.czt.bbt.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.*

object NotificationHelper {

    fun sendKakaoMessage(context: Context, busNo: String, plateNo: String, time: String, station: String, type: String, summary: String = "") {
        // type: "승차", "하차", "도착 안내" 등
        val title = "[버스알림] $type 안내"
        val description = if (summary.isNotEmpty()) summary else "버스 번호: ${busNo}번\n차량 번호: $plateNo\n시간: $time\n정류장: $station"

        val defaultFeed = FeedTemplate(
            content = Content(
                title = title,
                description = description,
                imageUrl = "http://k.kakaocdn.net/dn/Q2iNx/btqgeRgV54P/VLj1NbrLccVpafpBkMzyVi/kakaolink40_original.png", // 기본 아이콘 예시
                link = Link(webUrl = "https://www.gbis.go.kr", mobileWebUrl = "https://www.gbis.go.kr")
            )
        )

        if (ShareClient.instance.isKakaoTalkSharingAvailable(context)) {
            ShareClient.instance.shareDefault(context, defaultFeed) { sharingResult, error ->
                if (error != null) {
                    Log.e("KakaoShare", "실패: $error")
                } else if (sharingResult != null) {
                    context.startActivity(sharingResult.intent)
                }
            }
        }
    }

    fun sendEmail(context: Context, busNo: String, plateNo: String, time: String, station: String, type: String, summary: String = "") {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_SUBJECT, "[버스알림] $type 알림")
            val body = if (summary.isNotEmpty()) summary else "버스 번호: ${busNo}번\n차량 번호: $plateNo\n시간: $time\n정류장: $station"
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("Email", "이메일 앱을 찾을 수 없습니다.")
        }
    }
}
