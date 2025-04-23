package com.example.dwnas

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
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

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_manifest_link)
        initViews()
        initRcViews()
        lifecycleScope.launch(Dispatchers.IO) {
            updateList()
        }

        bIDelAllLinks.setOnClickListener {
            show(this, "Удаление", "Вы хотите удалить все ссылки?") {
                if (it.toString() == "YES") {
                    lifecycleScope.launch(Dispatchers.IO) {
                        dB.deleteAllLinks(this@ManifestActivity)
                        updateList()
                    }
                }
            }
        }

        bIDelAllManifests.setOnClickListener {
            show(this, "Удаление", "Вы хотите удалить все Манифесты?") {
                if (it.toString() == "YES") {
                    lifecycleScope.launch(Dispatchers.IO) {
                        dB.deleteAllManifests(this@ManifestActivity)
                        updateList()
                    }
                }
            }
        }

        bIDelName.setOnClickListener {
            etName.text.clear()
        }

        bSaveLink.setOnClickListener {
            if (etLink.text.toString().contains("rutube.ru/plst")) {
                try{
                    Toast.makeText(
                        this@ManifestActivity,
                        "Нельзя установить плейлист",
                        Toast.LENGTH_SHORT
                    ).show()
                }catch(e:Exception){
                    Log.d("myresult request",e.message.toString())
                }
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
            lifecycleScope.launch(Dispatchers.IO) {
                val list = itemLinkAdapter.currentList
                var index = 0
                var count = 0
                var flag: Boolean
                if (progressBar.visibility == View.VISIBLE) {
                    withContext(Dispatchers.Main) {
                        try {
                            Toast.makeText(
                                this@ManifestActivity,
                                "Нельзя спарсить в данный момент",
                                Toast.LENGTH_SHORT
                            ).show()
                        } catch (e: Exception) {
                            Log.d("myresult request", e.message.toString())
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.VISIBLE
                    }
                    while (index < list.size) {
                        val item = list[index]
                        try {
                            flag = loadItem(item, count)
                            if (flag) {
                                count++
                            } else {
                                count = 0
                                index++
                            }
                            if (count == 5) {
                                index++
                                count = 0
                            }
                            Log.d("myresult request", count.toString())
                        } catch (e: Exception) {
                            Log.d("myresult request", e.message.toString())
                            withContext(Dispatchers.Main) {
                                try {
                                    Toast.makeText(
                                        this@ManifestActivity,
                                        "Нельзя спарсить в данный момент",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } catch (e: Exception) {
                                    Log.d("myresult request", e.message.toString())
                                }

                            }
                        }
                    }
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.INVISIBLE
                    }
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

    override fun onHandle(item: ListItemLink) {
        lifecycleScope.launch(Dispatchers.IO) {
            var flag = true
            var count = 0

            if (progressBar.visibility == View.VISIBLE) {
                withContext(Dispatchers.Main) {
                    try {
                        Toast.makeText(
                            this@ManifestActivity,
                            "Нельзя спарсить в данный момент",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.d("myresult request", e.message.toString())

                    }

                }
            } else {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.VISIBLE
                }
                while (flag) {
                    try {
                        flag = loadItem(item, count)
                        if (flag)
                            count++
                        if (count == 6) {
                            break
                        }

                        Log.d("myresult request", count.toString())

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

    private suspend fun loadItem(item: ListItemLink, count: Int): Boolean {
        var flag = true
        if (!item.link.contains("dzen.ru")) {
            return false
        }

        withContext(Dispatchers.Main) {
            val signal = loadUrlAndWait(item.link, item.name)
            if (signal == "+") {
                flag = false
            } else {
                if (count == 5) {
                    flag = false
                }
            }
        }
        return flag
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

        withContext(Dispatchers.IO) {
            val unescapedHtml = html
                .replace("\\u003C", "<")
                .replace("\\u003E", ">")
                .replace("\\\"", "\"")
                .removeSurrounding("\"")

            val scriptContents = extractScriptContents(unescapedHtml)
            val targetFunction = findFunctionInScripts(scriptContents)

            if (targetFunction != null) {
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

                val res: List<ListItemManifests> = dB.getExistManifest(
                    this@ManifestActivity,
                    manifest.manifest
                )

                if (res.isEmpty()) {
                    dB.addManifest(this@ManifestActivity, manifest) {
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

                override fun onPageFinished(view: WebView?, url: String?) {
                    lifecycleScope.launch {
                        tryGetHtml(name, continuation)
                    }
                    super.onPageFinished(view, url)
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    try {
                        continuation.resume("-")
                    } catch (e: Exception) {
                        Log.d("myresult request", e.message.toString())
                    }
                }
            }
            webView.loadUrl(url)
        }


}