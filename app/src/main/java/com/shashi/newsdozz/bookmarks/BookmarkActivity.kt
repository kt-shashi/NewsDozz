package com.shashi.newsdozz.bookmarks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.shashi.newsdozz.HomeActivity
import com.shashi.newsdozz.NewsAdapter
import com.shashi.newsdozz.NewsItemClicked
import com.shashi.newsdozz.databinding.ActivityBookmarkBinding
import com.shashi.newsdozz.model.NewsData

class BookmarkActivity : AppCompatActivity(), NewsItemClicked {

    // Data binding
    private lateinit var binding: ActivityBookmarkBinding

    var newsList = ArrayList<NewsData>()
    private var newsAdapter = NewsAdapter(this, this)

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private val TAG = "newsdozz"

    // Firebase Firestore
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Hide title bar
        try {
            this.supportActionBar!!.hide()
        } catch (e: NullPointerException) {

        }

        FirebaseApp.initializeApp(this)
        firestore=Firebase.firestore

        binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeUi()

        loadNews()
    }

    private fun initializeUi() {

        // Check if user is signed in
        auth = FirebaseAuth.getInstance()
        checkSignedIn()

        // Swipe refresh
        binding.swipeRefreshAB.setOnRefreshListener {
            loadNews()
            binding.swipeRefreshAB.isRefreshing = false
        }

        // News recycler view
        binding.rvBookmark.layoutManager = LinearLayoutManager(this)
        binding.rvBookmark.adapter = newsAdapter
    }

    // Fetach news from Firestore
    private fun loadNews() {
        binding.progressBarAB.visibility = View.VISIBLE
        val user = auth.currentUser?.email.toString()

        firestore.collection(user)
            .get()
            .addOnSuccessListener {

                newsList.clear()

                for (document: DocumentSnapshot in it) {
                    if (document.exists()) {
                        var data: NewsData? = document.toObject(NewsData::class.java)
                        newsList.add(data!!)
                    }
                }

                // Show data to recycler view
                newsAdapter.updateNewsList(newsList)
                newsAdapter.notifyDataSetChanged()
                binding.progressBarAB.visibility = View.GONE
            }
            .addOnFailureListener {
                showToast("Error while fetching Bookmarks")
            }

    }

    // Check if user is signed in
    private fun checkSignedIn() {
        val user = auth.currentUser
        if (user == null)
            startActivity(Intent(this, HomeActivity::class.java))
    }

    // Open full news
    override fun onItemClicked(item: NewsData) {

        val url = item.newsUrl
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))

    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


}