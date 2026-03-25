package com.czt.bbt.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.kakao.sdk.talk.TalkApiClient
import com.kakao.sdk.template.model.*

object NotificationHelper {

    fun sendKakaoMessage(context: Context, busNo: String, plateNo: String, time: String, station: String, type: String, summary: String = "") {
        val title = "[버스알림] $type 알림"
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
        val prefs = context.getSharedPreferences("bus_alert_prefs", Context.MODE_PRIVATE)
        val userEmail = prefs.getString("google_email", "") ?: ""
        val appPass = prefs.getString("google_app_password", "") ?: ""

        val subject = "[버스알림] $type 알림"
        val body = if (summary.isNotEmpty()) summary else "버스 번호: ${busNo}번\n차량 번호: $plateNo\n시간: $time\n정류장: $station"

        if (userEmail.isNotEmpty() && appPass.isNotEmpty()) {
            // 백그라운드 자동 전송 (JavaMail)
            Thread {
                try {
                    val props = java.util.Properties().apply {
                        put("mail.smtp.host", "smtp.gmail.com")
                        put("mail.smtp.socketFactory.port", "465")
                        put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                        put("mail.smtp.auth", "true")
                        put("mail.smtp.port", "465")
                    }

                    val session = javax.mail.Session.getDefaultInstance(props, object : javax.mail.Authenticator() {
                        override fun getPasswordAuthentication(): javax.mail.PasswordAuthentication {
                            return javax.mail.PasswordAuthentication(userEmail, appPass)
                        }
                    })

                    val message = javax.mail.internet.MimeMessage(session).apply {
                        setFrom(javax.mail.internet.InternetAddress(userEmail))
                        addRecipient(javax.mail.Message.RecipientType.TO, javax.mail.internet.InternetAddress(userEmail))
                        setSubject(subject)
                        setText(body)
                    }

                    javax.mail.Transport.send(message)
                    Log.i("Email", "메일 자동 전송 성공")
                } catch (e: Exception) {
                    Log.e("Email", "메일 자동 전송 실패: ${e.message}")
                    // 실패 시 기존 방식으로 폴백하거나 알림
                }
            }.start()
        } else {
            // 기존 방식: 이메일 앱 호출
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_SUBJECT, subject)
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
}
