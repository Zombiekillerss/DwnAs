package com.example.dwnas

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume


class ManifestActivity : ComponentActivity(), ItemManifestAdapter.Listener,
    ItemLinkAdapter.Listener {
    private lateinit var itemLinkAdapter: ItemLinkAdapter
    private lateinit var itemManifestAdapter: ItemManifestAdapter
    private lateinit var dB: DBRequestMaker
    private lateinit var webView: WebView
    private lateinit var bSaveLink: Button
    private lateinit var bDeleteLink: Button
    private lateinit var bHandleLinks: Button
    private lateinit var etLink: EditText
    private lateinit var etName: EditText

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_manifest_link)
        initViews()
        initRcViews()
        updateList()
        Log.d("myresult request", "test")

        bSaveLink.setOnClickListener {
            val item = ListItemLink(name = etName.text.toString(), link = etLink.text.toString())
            lifecycleScope.launch(Dispatchers.Default) {
                dB.addLink(this@ManifestActivity, item) {
                    updateList()
                }
            }
        }

        bDeleteLink.setOnClickListener {
            etLink.text.clear()
        }

        bHandleLinks.setOnClickListener {
            val list = itemLinkAdapter.currentList
            lifecycleScope.launch {
                for (item in list) {
                    loadUrlAndWait(item.link)

                }
            }

        }
    }

    override fun onClickSave(manifest: ListItemManifests) {
        copyToClipboard(this@ManifestActivity, manifest.manifest)
    }

    override fun onClickDelete(manifest: ListItemManifests) {
        lifecycleScope.launch {
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

    private fun updateList() {
        itemManifestAdapter.submitList(listOf())
        itemLinkAdapter.submitList(listOf())
        lifecycleScope.launch {
            val linkList = mutableListOf<ListItemLink>()
            val manifestList = mutableListOf<ListItemManifests>()
            manifestList.addAll(dB.getManifests(this@ManifestActivity))
            linkList.addAll(dB.getLinks(this@ManifestActivity))

            itemManifestAdapter.submitList(manifestList)
            itemLinkAdapter.submitList(linkList)
        }
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
        webView.getSettings().javaScriptEnabled = true
        bSaveLink = findViewById(R.id.bSaveLink)
        bDeleteLink = findViewById(R.id.bDelLink)
        bHandleLinks = findViewById(R.id.bGetMpdLinks)
        etLink = findViewById(R.id.etLink)
        etName = findViewById(R.id.etName)
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

    private suspend fun loadUrlAndWait(url: String): String = suspendCancellableCoroutine { continuation ->
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript(
                    "(function() { return document.documentElement.outerHTML; })();"
                ) { html ->
                    lifecycleScope.launch {
                        val unescapedHtml = html
                            .replace("\\u003C", "<")
                            .replace("\\u003E", ">")
                            .replace("\\\"", "\"")
                            .removeSurrounding("\"")
                        val scriptContents = extractScriptContents(unescapedHtml)

                        val targetFunction = findFunctionInScripts(scriptContents)


                        if (targetFunction != null) {
                            Log.d("myresult request", targetFunction.toString())
                            runOnUiThread {

                            }
                        } else {
                            Log.d("myresult request", "Функция не найдена")
                        }
                    }
                }
                continuation.resume("+")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                continuation.resume("-")
            }
        }

        webView.loadUrl(url)

        continuation.invokeOnCancellation {
            webView.webViewClient = WebViewClient()
        }
    }
}