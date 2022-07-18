package com.shashi.newsdozz

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.shashi.newsdozz.bookmarks.BookmarkActivity
import com.shashi.newsdozz.model.NewsData
import com.shashi.newsdozz.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity(), NewsItemClicked, View.OnClickListener {

    // Data binding
    private lateinit var binding: ActivityHomeBinding

    var newsList = ArrayList<NewsData>()
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

    // FIrebase Firestore
    val firestore = Firebase.firestore

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
        binding.ivBookmark.setOnLongClickListener {
            signOut()
            true
        }

        // Setup login
        auth = FirebaseAuth.getInstance()
        firebaseAuthHelper()

        // Swipe to add to bookmark feature
        val itemTouchHelperCallback =
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {

                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    if (isSignedIn()) {

                        var news = newsList.removeAt(viewHolder.adapterPosition)
                        addNewsToBookmark(news)

                    } else {
                        showToast("You must be logged-in to use this feature.")
                    }

                    newsAdapter.notifyDataSetChanged()

                }

            }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvNewsAM)

    }

    // Add data to Bookmark (Cloud firestore)
    private fun addNewsToBookmark(news: NewsData) {

        if (isSignedIn()) {
            var email = auth.currentUser?.email.toString()

            firestore
                .collection(email)
                .add(news)
                .addOnSuccessListener {
                    showToast("Added to Bookmark")
                }
                .addOnFailureListener {
                    showToast("Error adding to Bookmark")
                }
        }

    }

    // Check if user is signed in
    fun isSignedIn(): Boolean {
        val user = auth.currentUser
        if (user != null)
            return true
        return false
    }

    // Google Sign in helper
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
        if (isSignedIn()) {
            // Start Bookmark Activity
            startActivity(Intent(this, BookmarkActivity::class.java))
        } else {
            // Initiate google signin
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

                showToast("Error while signing in! Please try again")

            }

        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {

        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    showToast("Long press bookmark button to logout")
                    binding.ivBookmark.setImageResource(R.drawable.icon_bookmark)

                } else {

                    showToast("Something went wrong while signing in!")
                    binding.ivBookmark.setImageResource(R.drawable.icon_login)

                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun signOut() {

        if (isSignedIn()) {

            // Creating dialog
            var builder = AlertDialog.Builder(this)
            //set title for alert dialog
            builder.setTitle("Confirm Logout")
            builder.setMessage("Are you sure you want to logout?")
            builder.setIcon(R.drawable.icon_logout)

            builder.setPositiveButton("Yes") { dialogInterface, which ->

                // Logout
                Firebase.auth.signOut()
                binding.ivBookmark.setImageResource(R.drawable.icon_login)
                dialogInterface.dismiss()

            }
            builder.setNegativeButton("No") { dialogInterface, which ->
                dialogInterface.dismiss()
            }

            val alertDialog: AlertDialog = builder.create()
            // Set other dialog properties
            alertDialog.setCancelable(false)
            alertDialog.show()

        }
    }

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
                newsList.clear()

                for (i in 0 until articleResponse.length()) {

                    var newsJsonObject = articleResponse.getJSONObject(i)

                    var news = NewsData(
                        newsJsonObject.getString("title"),
                        newsJsonObject.getString("description"),
                        newsJsonObject.getString("image"),
                        newsJsonObject.getString("url")
                    )

                    newsList.add(news)

                }

                newsAdapter.updateNewsList(newsList)
                binding.rvNewsAM.adapter?.notifyDataSetChanged()
                binding.progressBarAM.visibility = View.GONE

            },
            { error ->
                showToast("Something went wrong while fetching news!")
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


