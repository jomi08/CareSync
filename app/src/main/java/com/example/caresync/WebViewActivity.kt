package com.example.caresync

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.caresync.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // get URL and title passed from PatientsFragment
        val url   = intent.getStringExtra("url") ?: "https://health.gov"
        val title = intent.getStringExtra("title") ?: "Health Article"

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = title

        setupWebView(url)
    }

    private fun setupWebView(url: String) {
        binding.webView.apply {

            // WebViewClient = handle page navigation inside our WebView
            // without this, tapping links opens the external browser
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    // hide progress bar when page fully loaded
                    binding.progressBar.visibility = View.GONE
                }
            }

            // WebChromeClient = handles browser UI things
            // like updating the progress bar
            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    binding.progressBar.progress = newProgress
                    // hide progress bar when 100%
                    if (newProgress == 100) {
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }

            settings.apply {
                javaScriptEnabled = true
                // ↑ allow JavaScript so the webpage works properly
                domStorageEnabled = true
                // ↑ allow local storage for web pages that need it
                loadWithOverviewMode = true
                // ↑ zoom out to fit wide pages on screen
                useWideViewPort = true
                // ↑ use the viewport meta tag from the webpage
                builtInZoomControls = true
                // ↑ allow pinch to zoom
                displayZoomControls = false
                // ↑ hide the ugly +/- zoom buttons
            }

            // actually load the URL
            loadUrl(url)
        }
    }

    // handle back button — go back in WebView history
    // instead of closing the activity
    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
            // ↑ if WebView has history, go back one page
        } else {
            super.onBackPressed()
            // ↑ no history → close activity normally
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}