package eu.kelosocial.android

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
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
    private var pendingCaptureUri: Uri? = null

    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        fileChooserCallback?.onReceiveValue(
            if (success && pendingCaptureUri != null) arrayOf(pendingCaptureUri!!) else null
        )
        fileChooserCallback = null
        pendingCaptureUri = null
    }

    private val captureVideo = registerForActivityResult(
        ActivityResultContracts.CaptureVideo()
    ) { success ->
        fileChooserCallback?.onReceiveValue(
            if (success && pendingCaptureUri != null) arrayOf(pendingCaptureUri!!) else null
        )
        fileChooserCallback = null
        pendingCaptureUri = null
    }

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

                    val acceptTypes = fileChooserParams?.acceptTypes
                        ?.filter { it.isNotBlank() }
                        ?: emptyList()

                    val wantsVideo = acceptTypes.any { it.startsWith("video/") }
                    val wantsImage = acceptTypes.any { it.startsWith("image/") } || !wantsVideo
                    val captureRequested = fileChooserParams?.isCaptureEnabled == true

                    if (captureRequested && (wantsImage || wantsVideo)) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                            this@MainActivity.fileChooserCallback = null
                            Toast.makeText(
                                this@MainActivity,
                                "L'accès à l'appareil photo est nécessaire.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return false
                        }

                        pendingCaptureUri = createCaptureUri(wantsVideo)
                        if (pendingCaptureUri == null) {
                            this@MainActivity.fileChooserCallback?.onReceiveValue(null)
                            this@MainActivity.fileChooserCallback = null
                            return false
                        }

                        if (wantsVideo) {
                            captureVideo.launch(pendingCaptureUri)
                        } else {
                            takePicture.launch(pendingCaptureUri)
                        }
                        return true
                    }

                    val mimeType = when {
                        wantsVideo -> "video/*"
                        wantsImage -> "image/*"
                        else -> "*/*"
                    }

                    return try {
                        filePicker.launch(mimeType)
                        true
                    } catch (_: Exception) {
                        this@MainActivity.fileChooserCallback = null
                        Toast.makeText(
                            this@MainActivity,
                            "Impossible d'ouvrir les fichiers.",
                            Toast.LENGTH_SHORT
                        ).show()
                        false
                    }
                }
            }

            loadUrl("https://kelosocial.eu")
        }

        setContentView(webView)
    }

    private fun createCaptureUri(video: Boolean): Uri? {
        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "kelo_" + System.currentTimeMillis() + if (video) ".mp4" else ".jpg"
            )
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                if (video) "video/mp4" else "image/jpeg"
            )
        }

        val collection = if (video) {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        return contentResolver.insert(collection, values)
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
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
