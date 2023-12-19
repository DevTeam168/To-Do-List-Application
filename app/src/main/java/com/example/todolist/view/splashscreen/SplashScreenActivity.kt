package com.example.todolist.view.splashscreen

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.databinding.ActivitySplashScreenBinding
import com.example.todolist.view.main.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivitySplashScreenBinding
    private var mProgressBarLevel = 0
    private var mHandler = Handler(Looper.getMainLooper())

    private var updateProgressBar = object : Runnable {
        override fun run() {
            mProgressBarLevel += (500 * 100) / 2500
            mBinding.progressBar.progress = mProgressBarLevel

            if (mProgressBarLevel >= 100) {
                startActivity(MainActivity.newIntent(this@SplashScreenActivity))
                finish()
            } else {
                mHandler.postDelayed(this, 500)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        init()
    }

    private fun init() {
        //in order to remove the statusBar
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        // Set the status bar icons to black
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API level 30) and above
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            // For Android Marshmallow (API level 23) to Android 10 (API level 29)
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }

        mHandler.post(updateProgressBar)
    }

}