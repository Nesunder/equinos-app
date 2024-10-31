package com.equinos.tips

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.equinos.R
import com.equinos.databinding.ActivityTipsBinding

class TipsActivity : AppCompatActivity() {
    private lateinit var adapter: TipsAdapter
    private lateinit var tipsBinding: ActivityTipsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tipsBinding = ActivityTipsBinding.inflate(layoutInflater)
        setContentView(tipsBinding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = TipsAdapter(this@TipsActivity, TipsList.tipsList)
        tipsBinding.recyclerViewTips.layoutManager =
            LinearLayoutManager(this@TipsActivity, LinearLayoutManager.VERTICAL, false)
        tipsBinding.recyclerViewTips.adapter = adapter
    }
}