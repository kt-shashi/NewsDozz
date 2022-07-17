package com.shashi.newsdozz

import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.shashi.newsdozz.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity(), NewsItemClicked, View.OnClickListener {

    // Data binding
    private lateinit var binding: ActivityHomeBinding

    private var newsAdapter = NewsAdapter(this, this)
    private var newsUrl: String = Constants.NEWS_API
    private var buttonCategory = ArrayList<Button>()
    private lateinit var newsCategory: String

    // Shared prefs
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var lang: String

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var gso: GoogleSignInOptions
    private val TAG = "newsdozz"
    private val RC_SIGN_IN = 100

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

        // Swipe refresh
        binding.swipeRefreshAM.setOnRefreshListener {
            loadNews()
            binding.swipeRefreshAM.isRefreshing = false
        }

        // News recycler view
        binding.rvNewsAM.layoutManager = LinearLayoutManager(this)
        binding.rvNewsAM.adapter = newsAdapter

        // News category buttons
        buttonCategory.add(binding.btnBreakingNews)
        buttonCategory.add(binding.btnBusiness)
        buttonCategory.add(binding.btnTechnology)
        buttonCategory.add(binding.btnSports)
        buttonCategory.add(binding.btnScience)
        buttonCategory.add(binding.btnHealth)
        for (i in buttonCategory)
            i.setOnClickListener(this)

        // By default Breaking news is selected
        newsCategory = "breaking-news"
        binding.btnBreakingNews.setBackgroundResource(R.drawable.category_button_selected_design)
        binding.btnBreakingNews.setTextColor(Color.parseColor("#000000"))

        sharedPreferences = getSharedPreferences(Constants.LANGUAGE_PREF, MODE_PRIVATE)
        getLanguage()
        newsUrl =
            "https://gnews.io/api/v4/top-headlines?lang=$lang&country=in&topic=$newsCategory&token=ef098601144aaa99d14f8cd6d85eb7d8"

        // Listener for Language Change
        binding.ivLanguage.setOnClickListener {
            changeLanguage()
        }

        // Listener for Signing in/Bookmark
        binding.ivBookmark.setOnClickListener {
            signIn()
        }
//        binding.ivBookmark.setOnLongClickListener {
//            signout()
//            true
//        }

        auth = FirebaseAuth.getInstance()
        firebaseAuthHelper()
    }

    private fun firebaseAuthHelper() {

        val user = auth.currentUser
        gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (user != null) {
            binding.ivBookmark.setImageResource(R.drawable.icon_bookmark)
        } else {
            binding.ivBookmark.setImageResource(R.drawable.icon_login)
        }
    }

    // Start sign-in
    private fun signIn() {
        val user = auth.currentUser
        if (user != null) {
            // TODO: Open Bookmarks  
        } else {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {

                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)

            } catch (e: Exception) {

            }

        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    val user = auth.currentUser

                    Log.d(TAG, "firebaseAuthWithGoogle: Sign in successful")
                    Log.d(TAG, "firebaseAuthWithGoogle: ${user?.email} : ${user?.displayName}")

                    binding.ivBookmark.setImageResource(R.drawable.icon_bookmark)

                } else {

                    Log.w(TAG, "signInWithCredential:failure", task.exception)

                    binding.ivBookmark.setImageResource(R.drawable.icon_login)

                }
            }
    }

//    private fun signout() {
//        val user = auth.currentUser
//
//        if (user != null) {
//
//            Firebase.auth.signOut()
//            binding.ivBookmark.setImageResource(R.drawable.icon_login)
//
//        } else {
//            Toast.makeText(this, "No login found", Toast.LENGTH_SHORT).show()
//            binding.ivBookmark.setImageResource(R.drawable.icon_bookmark)
//        }
//    }

    // Get language from Shared pref
    private fun getLanguage() {
        lang = sharedPreferences.getString(Constants.LANGUAGE, "en").toString()
    }

    // Save language into Shared pref
    private fun setLanguage(lang: String) {
        with(sharedPreferences.edit()) {
            putString(Constants.LANGUAGE, lang)
            apply()
        }

        onClick(binding.btnBreakingNews)
    }

    // Change language
    private fun changeLanguage() {
        getLanguage()

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_language)

        val radioGroup = dialog.findViewById<RadioGroup>(R.id.radioGroupLanguage)

        when (lang) {
            "en" -> {
                radioGroup.check(R.id.rbEnglish)
            }
            "hi" -> {
                radioGroup.check(R.id.rbHindi)
            }
            "mr" -> {
                radioGroup.check(R.id.rbMarathi)
            }
            "ta" -> {
                radioGroup.check(R.id.rbTamil)
            }
            "te" -> {
                radioGroup.check(R.id.rbTelugu)
            }
        }

        radioGroup.setOnCheckedChangeListener { radioGroup, id ->

            when (id) {
                R.id.rbEnglish -> {
                    lang = "en"
                }
                R.id.rbHindi -> {
                    lang = "hi"
                }
                R.id.rbMarathi -> {
                    lang = "mr"
                }
                R.id.rbTamil -> {
                    lang = "ta"
                }
                R.id.rbTelugu -> {
                    lang = "te"
                }
            }

        }

        var btnSave = dialog.findViewById<Button>(R.id.btnSaveLanguage)
        var btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        btnSave.setOnClickListener {
            setLanguage(lang)
            dialog.dismiss()
        }
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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

        newsCategory = buttonCategory[position].text.toString()

        newsUrl =
            "https://gnews.io/api/v4/top-headlines?lang=$lang&country=in&topic=$newsCategory&token=ef098601144aaa99d14f8cd6d85eb7d8"

        for (i in 0 until buttonCategory.size) {
            if (i == position)
                continue

            buttonCategory[i].setBackgroundResource(R.drawable.category_button_design)
            buttonCategory[i].setTextColor(Color.parseColor("#DAE0E2"))
            buttonCategory[i].isClickable = true
        }

        loadNews()
    }

    // Load news using Volley
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

    // Handle news item clicks
    override fun onItemClicked(item: NewsData) {

        val url = item.newsUrl
        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(url))

    }

}


