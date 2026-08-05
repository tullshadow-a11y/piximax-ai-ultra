package com.novacore.piximaxaiultra

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.novacore.piximaxaiultra.ui.theme.PixiMaxTheme
import com.startapp.sdk.adsbase.StartAppAd
import com.startapp.sdk.adsbase.StartAppSDK

class MainActivity : ComponentActivity() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val fileChooserLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (filePathCallback != null) {
                val dataIntent = result.data
                var results: Array<Uri>? = null
                if (result.resultCode == RESULT_OK && dataIntent != null) {
                    val dataString = dataIntent.dataString
                    val clipData = dataIntent.clipData
                    if (clipData != null) {
                        results = Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                    } else if (dataString != null) {
                        results = arrayOf(Uri.parse(dataString))
                    }
                }
                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Start.io Native SDK with App ID 207695262
        StartAppSDK.init(this, "207695262", false)
        StartAppSDK.enableReturnAds(false)

        enableEdgeToEdge()
        setContent {
            PixiMaxTheme {
                var webViewInstance by remember { mutableStateOf<WebView?>(null) }

                BackHandler(enabled = webViewInstance?.canGoBack() == true) {
                    webViewInstance?.goBack()
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(Color(0xFF07090E))
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        allowFileAccess = true
                                        allowContentAccess = true
                                        mediaPlaybackRequiresUserGesture = false
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                    }
                                    // Add Javascript Interfaces for Start.io Ad triggers
                                    addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")
                                    addJavascriptInterface(WebAppInterface(this@MainActivity), "AndroidNativeAd")

                                    webViewClient = WebViewClient()
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onShowFileChooser(
                                            webView: WebView?,
                                            filePathCallbackParam: ValueCallback<Array<Uri>>?,
                                            fileChooserParams: FileChooserParams?
                                        ): Boolean {
                                            filePathCallback?.onReceiveValue(null)
                                            filePathCallback = filePathCallbackParam

                                            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                                addCategory(Intent.CATEGORY_OPENABLE)
                                                type = "image/*"
                                            }
                                            try {
                                                fileChooserLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                filePathCallback = null
                                                return false
                                            }
                                            return true
                                        }
                                    }
                                    loadUrl("file:///android_asset/index.html")
                                    webViewInstance = this
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    inner class WebAppInterface(private val activity: MainActivity) {
        @JavascriptInterface
        fun showAd() {
            activity.runOnUiThread {
                StartAppAd.showAd(activity)
            }
        }

        @JavascriptInterface
        fun showInterstitial() {
            activity.runOnUiThread {
                StartAppAd.showAd(activity)
            }
        }
    }
}
