package com.shashi.newsdozz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.shashi.newsdozz.databinding.ActivityHomeBinding


class HomeActivity : AppCompatActivity(), NewsItemClicked {

    private lateinit var binding: ActivityHomeBinding
    private var newsAdapter = NewsAdapter(this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide title bar
        try {
            this.supportActionBar!!.hide()
        } catch (e: NullPointerException) {

        }

        // Data binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadNews()
    }

    private fun loadNews() {

        binding.progressBarAM.visibility = View.VISIBLE

        val url = Constants.NEWS_API

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->

                // Get url of image
                var articleResponse = response.getJSONArray("articles")
                var articleNews = ArrayList<NewsData>()

                for (i in 0 until articleResponse.length()) {

                    var newsJsonObject = articleResponse.getJSONObject(i)

                    var news = NewsData(
                        newsJsonObject.getString("title"),
                        newsJsonObject.getString("description"),
                        newsJsonObject.getString("image"),
                        newsJsonObject.getString("url")
                    )

                    articleNews.add(news)

                }

                newsAdapter.updateNewsList(articleNews)
                updateUI()
                binding.progressBarAM.visibility = View.GONE

            },
            { error ->
                // TODO: Handle error
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)

    }

    private fun updateUI() {

        binding.rvNewsAM.layoutManager = LinearLayoutManager(this)
        binding.rvNewsAM.adapter = newsAdapter

    }

    override fun onItemClicked(item: NewsData) {

        val url = item.newsUrl
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))

    }

}

