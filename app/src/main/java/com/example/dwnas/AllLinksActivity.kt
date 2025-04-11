package com.example.dwnas

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

class AllLinksActivity: ComponentActivity(), ItemManifestAdapter.Listener,
    ItemLinkAdapter.Listener {
        private lateinit var itemLinkAdapter: ItemLinkAdapter
        private lateinit var dB: DBRequestMaker
        private lateinit var webView: WebView
        private lateinit var bSaveLink: Button
        private lateinit var bDeleteLink: Button
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
                if (etLink.text.toString().contains("rutube.ru/plst")){
                    //val list = mutableListOf<String>()
                    setupWebView()
                    webView.loadUrl(etLink.text.toString())
                } else{
                    val item = ListItemLink(name = etName.text.toString(), link = etLink.text.toString())
                    lifecycleScope.launch(Dispatchers.Default) {
                        dB.addLink(this@AllLinksActivity, item) {
                            updateList()
                        }
                    }
                }
            }

            bDeleteLink.setOnClickListener {
                etLink.text.clear()
            }
        }

        override fun onClickSave(manifest: ListItemManifests) {
            copyToClipboard(this@AllLinksActivity, manifest.manifest)
        }

        override fun onClickDelete(manifest: ListItemManifests) {
            lifecycleScope.launch {
                dB.deleteManifest(this@AllLinksActivity, manifest)
                updateList()
            }
        }

        override fun onClickSave(link: ListItemLink) {
            copyToClipboard(this@AllLinksActivity, link.link)
        }

        override fun onClickDelete(link: ListItemLink) {
            lifecycleScope.launch(Dispatchers.IO) {
                dB.deleteLink(this@AllLinksActivity, link)
                updateList()
            }
        }

        private fun updateList() {
            itemLinkAdapter.submitList(listOf())
            lifecycleScope.launch {
                val linkList = mutableListOf<ListItemLink>()
                val manifestList = mutableListOf<ListItemManifests>()
                manifestList.addAll(dB.getManifests(this@AllLinksActivity))
                linkList.addAll(dB.getLinks(this@AllLinksActivity))
                itemLinkAdapter.submitList(linkList)
            }
        }

        private fun initRcViews() {
            val rcViewListLinks = findViewById<RecyclerView>(R.id.rcLinks)

            rcViewListLinks.layoutManager = LinearLayoutManager(this)

            itemLinkAdapter = ItemLinkAdapter(this)

            rcViewListLinks.adapter = itemLinkAdapter
        }

        @SuppressLint("SetJavaScriptEnabled")
        private fun initViews() {
            webView = findViewById(R.id.wv)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            bSaveLink = findViewById(R.id.bSaveLink)
            bDeleteLink = findViewById(R.id.bDelLink)
            etLink = findViewById(R.id.etLink)
            etName = findViewById(R.id.etName)
            dB = ViewModelProvider(this@AllLinksActivity)[DBRequestMaker::class]
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

        @SuppressLint("SetJavaScriptEnabled")
        private fun setupWebView() {
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    startScrollAndParse()
                }
            }
        }

        private fun startScrollAndParse() {
            scrollToBottom()
            Handler(Looper.getMainLooper()).postDelayed({
                extractLinks()
            }, 3000)
        }

        private fun scrollToBottom() {
            webView.evaluateJavascript("""
            window.scrollTo(0, document.body.scrollHeight);
            true; // Возвращаем true для callback
        """) { }
        }

        private fun extractLinks() {
            webView.evaluateJavascript("""
            (function() {
                var videoCards = document.querySelectorAll('div.wdp-playlist-video-card-module__card article.wdp-playlist-video-card-module__content div.wdp-playlist-video-card-module__info a[href]');
                var links = [];
                for (var i = 0; i < videoCards.length; i++) {
                    links.push(videoCards[i].getAttribute('href'));
                }
                
                return links;
            })();
        """) { result ->
                try {
                    val jsonArray = JSONArray(result)
                    val links = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        links.add(jsonArray.getString(i))
                    }

                    // Теперь у вас есть все ссылки
                    Log.d("myresult request", "Found ${links.size} links")
                    for (link in links) {
                        Log.d("myresult request", link)
                    }

                    // Если нужно проверить, есть ли еще контент для подгрузки
                    checkIfMoreContent()
                } catch (e: Exception) {
                    Log.e("myresult request", "Failed to parse links", e)
                }
            }
        }

        private fun checkIfMoreContent() {
            webView.evaluateJavascript("""
            (function() {
                // Проверяем, есть ли еще контент для подгрузки
                // Это зависит от структуры вашей страницы
                // Например, можно проверить наличие кнопки "Load more" или сравнить scrollHeight с текущей позицией
                return {
                    canScrollMore: window.innerHeight + window.scrollY < document.body.offsetHeight,
                    currentScrollY: window.scrollY,
                    scrollHeight: document.body.scrollHeight
                };
            })();
        """) { result ->
                try {
                    val json = JSONObject(result)
                    val canScrollMore = json.getBoolean("canScrollMore")

                    if (canScrollMore) {
                        // Если есть еще контент, повторяем процесс
                        startScrollAndParse()
                    } else {
                        Log.d("myresult request", "All content loaded, total links parsed")
                    }
                } catch (e: Exception) {
                    Log.e("myresult request", "Failed to check scroll status", e)
                }
            }
        }
}