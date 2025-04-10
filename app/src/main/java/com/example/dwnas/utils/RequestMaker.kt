package com.example.dwnas.utils

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.InputStreamReader

class RequestMaker(): ViewModel() {
    val handler = Handler(Looper.getMainLooper())

    fun getMpd(urlStr: String, onResult: (String) -> Unit){
        val url = urlStr
            .toHttpUrl()
            .newBuilder()
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
//            .header("User-Agent", "Mozilla/5.0")
//            .header("Accept", "text/html")
            .get()
            .build()

        val client = OkHttpClient()
            .newBuilder()
            .callTimeout(5, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .dns(Dns.SYSTEM)
            .build()
        viewModelScope.launch {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    handler.post {
                        onResult("{\"error\": \"Не удалось подключиться к серверу\"}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            handler.post {
                                onResult("{\"error\": \"Нет данных для этой даты\"}")
                            }
                        }
                        val data = response.body
                        if (data != null) {
                            val allTextData = data.string()
                            handler.post {
                                onResult(allTextData)
                            }
                        } else {
                            handler.post {
                                onResult("[{\"error\": \"Ошибка получения: Нет данных\"}]")
                            }
                        }
                    } catch (e: Exception) {
                        handler.post {
                            onResult("{\"error\": \"${e.message}\"}")
                        }
                    }
                }
            })
        }
    }

    fun getHtml(onResult: (String) -> Unit){
        val url = "https://dzen.ru/video/watch/65055e3b0dc3cb7ba46408d3"
            .toHttpUrl()
            .newBuilder()
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "text/html")
            .get()
            .build()

        val client = OkHttpClient()
            .newBuilder()
            .callTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .dns(Dns.SYSTEM)
            .build()
        viewModelScope.launch {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    handler.post {
                        onResult("{\"error\": \"Не удалось подключиться к серверу\"}")
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (!response.isSuccessful) {
                            handler.post {
                                onResult("{\"error\": \"Нет данных для этой даты\"}")
                            }
                        }
                        val data = response.body
                        if (data != null) {
                            data.byteStream().use{ inputStream ->
                                val reader = InputStreamReader(inputStream, "UTF-8")
                                val buffer = CharArray(500)

                                // Пропускаем первые 500 символов
                                reader.read(buffer, 0, 500)

                                // Читаем оставшийся текст
                                val remainingText = reader.readText()
                                handler.post {
                                    Log.d("data1231", remainingText)
                                    onResult(remainingText)
                                }
                            }
                        } else {
                            handler.post {
                                onResult("{\"error\": \"Нет данных\"}")
                            }
                        }

//                        val html = response.body?.string() ?: ""
//                        val doc = Jsoup.parse(html)
//                        val scriptTags = doc.select("script")
//
//                        val scriptsContent = scriptTags.map { it.html() }
//                        handler.post {
//                            Log.d("data1231", scriptsContent.toString())
//                            onResult(scriptsContent.toString())
//                        }
//                        val data = response.body
//                        if (data != null) {
//                            val allTextData = data.string()
//                            handler.post {
//                                Log.d("data1231", allTextData)
//                                onResult(allTextData)
//                            }
//                        } else {
//                            handler.post {
//                                onResult("{\"error\": \"Нет данных\"}")
//                            }
//                        }
                    } catch (e: Exception) {
                        handler.post {
                            onResult("{\"error\": \"${e.message}\"}")
                        }
                    }
                }
            })
        }
    }
}