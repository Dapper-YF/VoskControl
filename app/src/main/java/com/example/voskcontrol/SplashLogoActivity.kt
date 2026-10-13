package com.example.voskcontrol

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashLogoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash_logo)

        val logoVideoView: VideoView = findViewById(R.id.logoVideoView)
        // 确保你的logo视频文件名为 logo_video.mp4 放在 res/raw/ 目录下
        val videoPath = "android.resource://" + packageName + "/" + R.raw.logo_video
        logoVideoView.setVideoURI(Uri.parse(videoPath))
        logoVideoView.setOnCompletionListener {
            startActivity(Intent(this, SplashAnimationActivity::class.java))
            finish()
        }
        logoVideoView.start()
    }
}