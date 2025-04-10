package com.example.dwnas

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONException


class ManifestActivity : ComponentActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.get_manifest_link)
        val webView = findViewById<WebView>(R.id.wv)
        val button = findViewById<Button>(R.id.bGetDataBuff)
        val et = findViewById<EditText>(R.id.etLink)
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
                                val text = findViewById<TextView>(R.id.tv)
                                text.text = targetFunction[1]
                            }
                            // Используйте найденную функцию
                        } else {
                            Log.d("myresult request", "Функция не найдена")
                        }
                    }
                }
            }
        }

        button.setOnClickListener {
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

}