package eu.kelosocial.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    private var fileChooserCallback: android.webkit.ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        fileChooserCallback?.onReceiveValue(uris.toTypedArray())
        fileChooserCallback = null
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        createNotificationChannel()
        requestNotificationPermission()

        val webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = "$userAgentString KeloSocialAndroid/1.0"
            }

            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url.host ?: return false
                    return !(host == "kelosocial.eu" || host.endsWith(".kelosocial.eu"))
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: android.webkit.ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                    this@MainActivity.fileChooserCallback = filePathCallback

                    val acceptTypes = fileChooserParams?.acceptTypes?.filter { it.isNotBlank() } ?: emptyList()
                    val mimeType = when {
                        acceptTypes.any { it.startsWith("video/") } -> "video/*"
                        acceptTypes.any { it.startsWith("image/") } -> "image/*"
                        else -> "*/*"
                    }

                    return try {
                        filePicker.launch(mimeType)
                        true
                    } catch (_: Exception) {
                        this@MainActivity.fileChooserCallback = null
                        Toast.makeText(this@MainActivity, "Impossible d'ouvrir les fichiers.", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            }

            loadUrl("https://kelosocial.eu")
        }

        setContentView(webView)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "kelo_social",
                "Kelo Social",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications de Kelo Social"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
