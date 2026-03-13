package com.onbada.seathermo.presentation.point.list

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.onbada.seathermo.R
import com.onbada.seathermo.data.repository.DefaultPointDataListRepository
import com.onbada.seathermo.data.storage.FileDataStorageImpl
import com.onbada.seathermo.domain.entity.PointData
import com.onbada.seathermo.domain.usecase.PointDataUseCase
import com.onbada.seathermo.managers.FDFileManager
import com.onbada.seathermo.presentation.point.PointMapActivity
import kotlinx.coroutines.launch

class PointDataListActivity : AppCompatActivity() {

    private lateinit var viewModel: PointDataListViewModel
    private lateinit var adapter: PointDataAdapter
    private lateinit var progressBar: ProgressBar

    // Intent Data
    private var pointDatePath: String? = null
    private var pointDateName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_data_list)

        processIntent()
        initializeViews()
        initializeViewModel()
        observeViewModel()
        
        // Load Data
        if (pointDatePath != null && pointDateName != null) {
            viewModel.fetchDataList(pointDatePath!!, pointDateName!!)
        }
    }

    private fun processIntent() {
        pointDatePath = intent.getStringExtra("POINT_DATE_PATH")
        pointDateName = intent.getStringExtra("POINT_DATE_NAME")
    }

    private fun initializeViews() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        adapter = PointDataAdapter { pointData ->
            navigateToMap(pointData)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun initializeViewModel() {
        val fileManager = FDFileManager.getInstance(applicationContext)
        val storage = FileDataStorageImpl(fileManager)
        val repository = DefaultPointDataListRepository(storage)
        val useCase = PointDataUseCase(repository)

        val factory = PointDataListViewModelFactory(useCase)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory).get(PointDataListViewModel::class.java)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.pointDataList.collect { list ->
                adapter.submitList(list)
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                if (message != null) {
                    Toast.makeText(this@PointDataListActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToMap(pointData: PointData) {
        val intent = Intent(this, PointMapActivity::class.java)
        // PointData 객체에 포함된 경로와 이름을 전달
        // 실제 PointData는 Parcelable이 아니므로 주요 필드만 전달하거나 경로를 전달
        intent.putExtra("POINT_DATA_PATH", pointData.dataPath?.absolutePath)
        intent.putExtra("POINT_DATA_NAME", pointData.dataName)
        startActivity(intent)
    }
}
