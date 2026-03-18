package com.czt.bbt.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.*

object NotificationHelper {

    fun sendKakaoMessage(context: Context, busNo: String, plateNo: String, time: String, station: String, type: String, summary: String = "") {
        val title = "[버스알림] $type 안내"
        val description = if (summary.isNotEmpty()) summary else "버스 번호: ${busNo}번\n차량 번호: $plateNo\n시간: $time\n정류장: $station"

        val defaultFeed = FeedTemplate(
            content = Content(
                title = title,
                description = description,
                imageUrl = "http://k.kakaocdn.net/dn/Q2iNx/btqgeRgV54P/VLj1NbrLccVpafpBkMzyVi/kakaolink40_original.png",
                link = Link(webUrl = "https://www.gbis.go.kr", mobileWebUrl = "https://www.gbis.go.kr")
            )
        )

        // TalkApiClient.instance를 사용하여 "나에게 보내기" 호출
        try {
            TalkApiClient.instance.sendDefaultMemo(defaultFeed) { error: Throwable? ->
                if (error != null) {
                    val errorMsg = if (error.message?.contains("tokens don't exist") == true) 
                        "로그인이 필요합니다. 앱 설정에서 카카오 로그인을 해주세요." else error.message
                    Log.e("KakaoTalk", "나에게 보내기 실패: $errorMsg")
                } else {
                    Log.i("KakaoTalk", "나에게 보내기 성공")
                }
            }
        } catch (e: Exception) {
            Log.e("KakaoTalk", "TalkApiClient 호출 불가: ${e.message}")
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
