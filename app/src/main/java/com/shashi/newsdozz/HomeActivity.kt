package com.shashi.newsdozz

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.shashi.newsdozz.databinding.ActivityHomeBinding


class HomeActivity : AppCompatActivity(), NewsItemClicked, View.OnClickListener {

    private lateinit var binding: ActivityHomeBinding
    private var newsAdapter = NewsAdapter(this, this)

    private var newsUrl: String = Constants.NEWS_API
    private var buttonCategory = ArrayList<Button>()

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

        initializeUiComponents()

        // Load News by default for the first time app opens
        loadNews()
    }

    // Initialize buttons
    private fun initializeUiComponents() {

        binding.swipeRefreshAM.setOnRefreshListener {
            loadNews()
            binding.swipeRefreshAM.isRefreshing = false
        }

        binding.rvNewsAM.layoutManager = LinearLayoutManager(this)
        binding.rvNewsAM.adapter = newsAdapter

        buttonCategory.add(binding.btnBreakingNews)
        buttonCategory.add(binding.btnBusiness)
        buttonCategory.add(binding.btnTechnology)
        buttonCategory.add(binding.btnSports)
        buttonCategory.add(binding.btnScience)
        buttonCategory.add(binding.btnHealth)

        for (i in buttonCategory)
            i.setOnClickListener(this)

        // By default Breaking news is selected
        binding.btnBreakingNews.setBackgroundResource(R.drawable.category_button_selected_design)
        binding.btnBreakingNews.setTextColor(Color.parseColor("#000000"))
    }

    // Handle Category button click
    override fun onClick(view: View?) {

        var position = 0

        when (view?.id) {
            R.id.btnBreakingNews -> position = 0
            R.id.btnBusiness -> position = 1
            R.id.btnTechnology -> position = 2
            R.id.btnSports -> position = 3
            R.id.btnScience -> position = 4
            R.id.btnHealth -> position = 5
        }

        updateCategoryUI(position)

    }

    // Update Category UI components
    private fun updateCategoryUI(position: Int) {
        buttonCategory[position].setBackgroundResource(R.drawable.category_button_selected_design)
        buttonCategory[position].setTextColor(Color.parseColor("#000000"))
        buttonCategory[position].isClickable = false

        val newsCategory = buttonCategory[position].text

        newsUrl =
            "https://gnews.io/api/v4/top-headlines?lang=en&country=in&topic=$newsCategory&token=ef098601144aaa99d14f8cd6d85eb7d8"

        for (i in 0 until buttonCategory.size) {
            if (i == position)
                continue

            buttonCategory[i].setBackgroundResource(R.drawable.category_button_design)
            buttonCategory[i].setTextColor(Color.parseColor("#DAE0E2"))
            buttonCategory[i].isClickable = true
        }

        loadNews()
    }

    private fun loadNews() {

        binding.progressBarAM.visibility = View.VISIBLE

        val url = newsUrl

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
                binding.rvNewsAM.adapter?.notifyDataSetChanged()
                binding.progressBarAM.visibility = View.GONE

            },
            { error ->
                Toast.makeText(this, "Oops.. Something went wrong!", Toast.LENGTH_SHORT).show()
            }
        )

        VolleySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)

    }

    override fun onItemClicked(item: NewsData) {

        val url = item.newsUrl
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))

    }


}


