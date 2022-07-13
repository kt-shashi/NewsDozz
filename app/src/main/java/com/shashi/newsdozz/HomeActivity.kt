package com.shashi.newsdozz

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.shashi.newsdozz.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Data binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}