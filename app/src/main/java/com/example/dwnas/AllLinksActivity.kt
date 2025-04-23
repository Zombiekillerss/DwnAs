package com.example.dwnas

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dwnas.adapters.ItemLinkAdapter
import com.example.dwnas.database.DBRequestMaker
import com.example.dwnas.database.ListItemLink
import com.example.dwnas.database.ListItemManifests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AllLinksActivity: ComponentActivity(), ItemLinkAdapter.Listener {
        private lateinit var itemLinkAdapter: ItemLinkAdapter
        private lateinit var dB: DBRequestMaker
        private lateinit var webView: WebView
        private lateinit var bSaveLink: ImageButton

        private lateinit var bIDeleteLink: ImageButton
        private lateinit var bIDeleteName: ImageButton
        private lateinit var bIDeleteLinks: ImageButton

        private lateinit var etLink: EditText
        private lateinit var etName: EditText

        private lateinit var progressBar: ProgressBar

        private var oneCount: Int = 0
        private var twoCount: Int = 0
        private var threeCount: Int = 0


        @SuppressLint("SetJavaScriptEnabled")
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.get_all_links)
            initViews()
            initRcViews()
            updateList()

            bSaveLink.setOnClickListener {
                if (etLink.text.toString().contains("rutube.ru/plst")){
                    setupWebView()
                    webView.loadUrl(etLink.text.toString())
                } else{
                    val item = ListItemLink(name = etName.text.toString(), link = etLink.text.toString())
                    lifecycleScope.launch(Dispatchers.IO) {
                        withContext(Dispatchers.Main){
                            progressBar.visibility = View.VISIBLE
                        }
                        dB.addLink(this@AllLinksActivity, item)
                        updateList()
                        withContext(Dispatchers.Main){
                            progressBar.visibility = View.INVISIBLE
                        }
                    }
                }
            }

            bIDeleteLink.setOnClickListener {
                etLink.text.clear()
            }
            bIDeleteName.setOnClickListener {
                etName.text.clear()
            }
            bIDeleteLinks.setOnClickListener {
                show(this, "Удаление", "Вы хотите удалить все ссылки?"){
                    Log.d("myresult request", it.toString())
                    if(it.toString() == "YES") {
                        lifecycleScope.launch(Dispatchers.IO){
                            dB.deleteAllLinks(this@AllLinksActivity)
                            updateList()
                        }
                    }
                }

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

    override fun onHandle(item: ListItemLink) {
        return
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

            itemLinkAdapter = ItemLinkAdapter(this, false)

            rcViewListLinks.adapter = itemLinkAdapter
        }

        @SuppressLint("SetJavaScriptEnabled")
        private fun initViews() {
            webView = findViewById(R.id.wv)
            webView.settings.javaScriptEnabled = true
            webView.settings.domStorageEnabled = true
            bSaveLink = findViewById(R.id.bISaveLink)

            bIDeleteLink = findViewById(R.id.bIDelLink)
            bIDeleteName = findViewById(R.id.bIDelName)
            bIDeleteLinks = findViewById(R.id.bIDelAllLinks)

            progressBar = findViewById(R.id.progressBar)
            progressBar.visibility = View.INVISIBLE

            etLink = findViewById(R.id.etLink)
            etName = findViewById(R.id.etName)

            dB = ViewModelProvider(this@AllLinksActivity)[DBRequestMaker::class]
        }

        @SuppressLint("SetJavaScriptEnabled")
        private fun setupWebView() {
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)

                    progressBar.visibility = View.VISIBLE

                    try {
                        val links = mutableListOf<String>()
                        val str = etName.text.toString()
                        var count = 0
                        if(str.contains(' ') && str.substring(0, str.indexOf(' ')).isDigitsOnly()){
                            count = str.substring(0, str.indexOf(' ')).toInt()
                        }
                        startScrollAndParse(links, count)
                    }catch (e:Exception){
                        Toast.makeText(this@AllLinksActivity, "Ошибка ${e.message.toString()}", Toast.LENGTH_SHORT).show()
                    }

                    progressBar.visibility = View.INVISIBLE
                }
            }
        }

        private fun startScrollAndParse(links:  MutableList<String>, count: Int) {
            scrollToBottom()
            Handler(Looper.getMainLooper()).postDelayed({
                extractLinks(links, count)
            }, 3000)
        }

        private fun scrollToBottom() {
            webView.evaluateJavascript("""
            window.scrollTo(0, document.body.scrollHeight);
            true; // Возвращаем true для callback
        """) { }
        }

        private fun extractLinks(links:  MutableList<String>, count: Int) {
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
                    links.clear()
                    for (i in 0 until jsonArray.length()) {
                        links.add(jsonArray.getString(i))
                    }

                    Log.d("myresult request", "Found ${links.size} links")
                    for (link in links) {
                        Log.d("myresult request", link)
                    }

                    if(count <= links.size && count != 0){
                        addAllLinks(links)
                    } else {
                        if (oneCount == links.size) {
                            if (twoCount == links.size) {
                                if (threeCount == links.size) {
                                    addAllLinks(links)
                                } else {
                                    threeCount = links.size
                                    checkIfMoreContent(links, count)
                                }
                            } else {
                                twoCount = links.size
                                checkIfMoreContent(links, count)
                            }
                        } else {
                            checkIfMoreContent(links, count)
                            oneCount = links.size
                            twoCount = 0
                            threeCount = 0
                        }
                    }
                } catch (e: Exception) {
                    Log.e("myresult request", "Failed to parse links", e)
                    Toast.makeText(this, "Ошибка ${e.message.toString()}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun addAllLinks(links: MutableList<String>) {
        Toast.makeText(this, "Найдены все ссылки", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            var startSeg = 0
            var name = etName.text.toString().let { str ->
                if (str.contains(' ') && str.substring(0, str.indexOf(' ')).isDigitsOnly())
                    str.substring(str.indexOf(' ') + 1)
                else
                    str
            }
            if(name.contains(' ')){
                startSeg = name.let{
                    try {
                        it.split(' ').last().toInt() -1
                    }catch(e:Exception){
                        0
                    }
                }
            }
            if(name.split(' ').last().isDigitsOnly())
                name = name.substringBeforeLast(' ')
            
            for (index in 0 until links.size) {
                if(index < startSeg)
                    continue
                val listItem = ListItemLink(name = "$name ${index + 1}", link = "https://rutube.ru${links[index]}")
                dB.addLink(this@AllLinksActivity, listItem)
            }
            updateList()
        }
    }

    private fun checkIfMoreContent(links:  MutableList<String>, count: Int) {
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
                        startScrollAndParse(links, count)
                    } else {
                        Log.d("myresult request", "All content loaded, total links parsed")
                    }
                } catch (e: Exception) {
                    Log.e("myresult request", "Failed to check scroll status", e)
                }
            }
        }
}
