package com.example.dwnas

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
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
import kotlinx.coroutines.launch


class ManifestActivity : ComponentActivity(), ItemManifestAdapter.Listener, ItemLinkAdapter.Listener {
    private lateinit var itemLinkAdapter: ItemLinkAdapter
    private lateinit var itemManifestAdapter: ItemManifestAdapter

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_manifest_link)
        val webView = findViewById<WebView>(R.id.wv)
        val bSaveLink = findViewById<Button>(R.id.bSaveLink)
        val bDeleteLink = findViewById<Button>(R.id.bDelLink)
        val db = ViewModelProvider(this@ManifestActivity)[DBRequestMaker::class]
        val bHandleLinks = findViewById<Button>(R.id.bGetMpdLinks)
        val et = findViewById<EditText>(R.id.etLink)
        //val etName = findViewById<EditText>(R.id.etLink)
        initRcViews()
        webView.getSettings().javaScriptEnabled = true
        Log.d("myresult request", "test")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
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

                        // Ищем функцию среди содержимого script-тегов
                        val targetFunction = findFunctionInScripts(scriptContents, "(function()")


                        if (targetFunction != null) {
                            Log.d("myresult request", targetFunction.toString())
                            copyToClipboard(this@ManifestActivity, targetFunction[1])
                            runOnUiThread{

                            }
                            // Используйте найденную функцию
                        } else {
                            Log.d("myresult request", "Функция не найдена")
                        }
                    }
                }
            }
        }

        bSaveLink.setOnClickListener {
            val item = ListItemLink(name="name", link=et.text.toString())
            lifecycleScope.launch {
                db.insertLink(this@ManifestActivity, item)
            }
        }

        bDeleteLink.setOnClickListener{
            et.text.clear()
        }
        bHandleLinks.setOnClickListener{
            webView.loadUrl(et.text.toString())
        }
    }



    fun extractScriptContents(html: String): List<String> {
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

    // Функция для поиска конкретной функции в содержимом script-тегов
    fun findFunctionInScripts(scripts: List<String>, functionStart: String): List<String>? {
        for (script in scripts) {
            val startIndex = script.indexOf(functionStart)
            if (startIndex == -1) continue

            var openBraces = 0
            var currentIndex = startIndex

            while (currentIndex < script.length) {
                when (script[currentIndex]) {
                    '{' -> openBraces++
                    '}' -> {
                        openBraces--
                        if (openBraces == 0) {
                            // Нашли закрывающую скобку функции
                            var str = script.substring(startIndex, currentIndex + 1)
                            if(str.contains("\"streams\"")) {
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

    override fun onClickSave(device: ListItemManifests) {
        copyToClipboard(this@ManifestActivity, device.manifest)
    }

    override fun onClickDelete(device: ListItemManifests) {
        TODO("Not yet implemented")
    }

    override fun onClickSave(device: ListItemLink) {
        copyToClipboard(this@ManifestActivity, device.link)
    }

    override fun onClickDelete(device: ListItemLink) {
        TODO("Not yet implemented")
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

}