package com.example.dwnas

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dwnas.adapters.ItemLinkAdapter
import com.example.dwnas.adapters.ItemManifestAdapter
import com.example.dwnas.database.DBRequestMaker
import com.example.dwnas.database.ListItemLink
import com.example.dwnas.database.ListItemManifests
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume


class ManifestActivity : ComponentActivity(), ItemManifestAdapter.Listener,
    ItemLinkAdapter.Listener {

    private lateinit var itemLinkAdapter: ItemLinkAdapter
    private lateinit var itemManifestAdapter: ItemManifestAdapter

    private lateinit var dB: DBRequestMaker

    private lateinit var webView: WebView

    private lateinit var bSaveLink: ImageButton
    private lateinit var bHandleLinks: ImageButton
    private lateinit var bIDelAllLinks: ImageButton
    private lateinit var bIDelAllManifests: ImageButton

    private lateinit var bDeleteLink: ImageButton
    private lateinit var bIDelName: ImageButton

    private lateinit var etLink: EditText
    private lateinit var etName: EditText

    private lateinit var progressBar: ProgressBar

    private var isPageLoaded = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_manifest_link)
        initViews()
        initRcViews()
        lifecycleScope.launch(Dispatchers.IO) {
            updateList()
        }
        Log.d("myresult request", "test")

        bIDelAllLinks.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                dB.deleteAllLinks(this@ManifestActivity)
                updateList()
            }
        }

        bIDelAllManifests.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                dB.deleteAllManifests(this@ManifestActivity)
                updateList()
            }
        }

        bIDelName.setOnClickListener {
            etName.text.clear()
        }

        bSaveLink.setOnClickListener {
            if (etLink.text.toString().contains("rutube.ru/plst")) {
                Toast.makeText(
                    this@ManifestActivity,
                    "Нельзя установить плейлист",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val item =
                    ListItemLink(name = etName.text.toString(), link = etLink.text.toString())
                lifecycleScope.launch(Dispatchers.IO) {
                    dB.addLink(this@ManifestActivity, item)
                    updateList()
                }
            }
        }

        bDeleteLink.setOnClickListener {
            etLink.text.clear()
        }

        bHandleLinks.setOnClickListener {
            val list = itemLinkAdapter.currentList
            lifecycleScope.launch(Dispatchers.IO) {
                var index = 0
                var count = 0
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.VISIBLE
                }

                while (index < list.size) {
                    val item = list[index]
                    try {
                        if (!item.link.contains("dzen.ru")) {
                            index++
                            continue
                        }
                        withContext(Dispatchers.Main) {
                            val lol = loadUrlAndWait(item.link, item.name)

                            Log.d("myresult request", lol)
                            if (lol == "+") {
                                index++
                                count = 0
                            } else {
                                count++
                                if (count == 5) {
                                    index++
                                    count = 0
                                }
                            }
                        }
                        Log.d("myresult request", "end")

                        isPageLoaded = false
                    } catch (e: Exception) {
                        Log.d("myresult request", e.message.toString())
                    }
                }
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.INVISIBLE
                }
            }

        }
    }

    override fun onClickSave(manifest: ListItemManifests) {
        copyToClipboard(this@ManifestActivity, manifest.manifest)
    }

    override fun onClickDelete(manifest: ListItemManifests) {
        lifecycleScope.launch(Dispatchers.IO) {
            dB.deleteManifest(this@ManifestActivity, manifest)
            updateList()
        }
    }

    override fun onClickSave(link: ListItemLink) {
        copyToClipboard(this@ManifestActivity, link.link)
    }

    override fun onClickDelete(link: ListItemLink) {
        lifecycleScope.launch(Dispatchers.IO) {
            dB.deleteLink(this@ManifestActivity, link)
            updateList()
        }
    }

    private suspend fun updateList() {
        itemManifestAdapter.submitList(listOf())
        itemLinkAdapter.submitList(listOf())
        val linkList = mutableListOf<ListItemLink>()
        val manifestList = mutableListOf<ListItemManifests>()
        manifestList.addAll(dB.getManifests(this@ManifestActivity))
        linkList.addAll(dB.getLinks(this@ManifestActivity))
        itemManifestAdapter.submitList(manifestList)
        itemLinkAdapter.submitList(linkList)
    }

    private fun initRcViews() {
        val rcViewListManifests = findViewById<RecyclerView>(R.id.rcManifests)
        val rcViewListLinks = findViewById<RecyclerView>(R.id.rcLinks)

        rcViewListManifests.layoutManager = LinearLayoutManager(this)
        rcViewListLinks.layoutManager = LinearLayoutManager(this)

        itemLinkAdapter = ItemLinkAdapter(this)
        itemManifestAdapter = ItemManifestAdapter(this)

        rcViewListManifests.adapter = itemManifestAdapter
        rcViewListLinks.adapter = itemLinkAdapter
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initViews() {
        webView = findViewById(R.id.wv)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        bSaveLink = findViewById(R.id.bISaveLink)
        bHandleLinks = findViewById(R.id.bIGetMpdLinks)
        bIDelAllLinks = findViewById(R.id.bIDelAllLinks)
        bIDelAllManifests = findViewById(R.id.bIDelAllManifests)

        bDeleteLink = findViewById(R.id.bIDelLink)
        bIDelName = findViewById(R.id.bIDelName)

        etLink = findViewById(R.id.etLink)
        etName = findViewById(R.id.etName)

        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.INVISIBLE
        dB = ViewModelProvider(this@ManifestActivity)[DBRequestMaker::class]
    }

    private fun extractScriptContents(html: String): List<String> {
        val scriptContents = mutableListOf<String>()
        val scriptRegex = "<script[^>]*>(.*?)</script>".toRegex(RegexOption.DOT_MATCHES_ALL)

        scriptRegex.findAll(html).forEach { matchResult ->
            val content = matchResult.groupValues[1]
            if (content.isNotBlank()) {
                scriptContents.add(content)
            }
        }

        return scriptContents
    }

    private fun findFunctionInScripts(scripts: List<String>): List<String>? {
        for (script in scripts) {
            val startIndex = script.indexOf("(function()")
            if (startIndex == -1) continue

            var openBraces = 0
            var currentIndex = startIndex

            while (currentIndex < script.length) {
                when (script[currentIndex]) {
                    '{' -> openBraces++
                    '}' -> {
                        openBraces--
                        if (openBraces == 0) {
                            var str = script.substring(startIndex, currentIndex + 1)
                            if (str.contains("\"streams\"")) {
                                str = str.substring(str.indexOf("\"streams\""))
                                str = str.substring(str.indexOf('[') + 1, str.indexOf(']'))
                                return str.split(',')
                            }
                        }
                    }
                }
                currentIndex++
            }
        }
        return null
    }

    private suspend fun evaluateJavascriptSuspending(script: String): String? {
        return suspendCancellableCoroutine { continuation ->
            webView.postDelayed({
                webView.evaluateJavascript(script) { result ->
                    continuation.resume(result)
                }
            }, 500)

        }
    }

    private suspend fun tryGetHtml(
        name: String,
        continuation: CancellableContinuation<String>
    ) {
        var html: String?
        do {
            delay(10)
            html =
                evaluateJavascriptSuspending("(function() { return document.documentElement.outerHTML; })();")
        } while (html == null || html == "null")
        Log.d("myresult request", html.toString())
        withContext(Dispatchers.IO) {
            val unescapedHtml = html
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\\"", "\"")
                .removeSurrounding("\"")
            val scriptContents = extractScriptContents(unescapedHtml)
            val targetFunction = findFunctionInScripts(scriptContents)

            if (targetFunction != null) {
                Log.d("myresult request", targetFunction.toString())
                var manifestLink = targetFunction[1]
                if (manifestLink.indexOf('"') != -1) {
                    manifestLink =
                        manifestLink.substring(manifestLink.indexOf('"') + 1)
                    manifestLink =
                        manifestLink.substring(0, manifestLink.indexOf('"'))
                }
                val manifest =
                    ListItemManifests(
                        name = name,
                        manifest = manifestLink
                    )
                var res: List<ListItemManifests>
                withContext(Dispatchers.Main) {
                    res = dB.getExistManifest(
                        this@ManifestActivity,
                        manifest.manifest
                    )
                }
                if (res.isEmpty()) {
                    dB.addManifest(this@ManifestActivity, manifest) {
                        Log.d("myresult request", it)
                        lifecycleScope.launch(Dispatchers.IO) {
                            updateList()

                        }
                    }
                }
                try {
                    continuation.resume("+")
                } catch (e: Exception) {
                    Log.d("myresult request", e.message.toString())

                }
            } else {
                try {
                    continuation.resume("-")
                } catch (e: Exception) {
                    Log.d("myresult request", e.message.toString())

                }
                Log.d("myresult request", "Функция не найдена")
            }


        }

    }

    private suspend fun loadUrlAndWait(url: String, name: String) =
        suspendCancellableCoroutine { continuation ->
            webView.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return false
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    if (isPageLoaded) return
                    isPageLoaded = true
                    lifecycleScope.launch {
                        tryGetHtml(name, continuation)
                    }
                    Log.d("myresult request", "proba")
                    super.onPageFinished(view, url)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    isPageLoaded = true
                    try {
                        continuation.resume("-")
                    } catch (e: Exception) {
                        Log.d("myresult request", e.message.toString())

                    }
                }
            }
            webView.loadUrl(url)
            Log.d("myresult request", "testststts")
            continuation.invokeOnCancellation {
                webView.destroy()
            }
        }
}