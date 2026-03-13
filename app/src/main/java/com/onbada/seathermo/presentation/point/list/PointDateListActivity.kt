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
import com.onbada.seathermo.data.repository.DefaultPointDateListRepository
import com.onbada.seathermo.data.storage.FileDataStorageImpl
import com.onbada.seathermo.domain.entity.PointDate
import com.onbada.seathermo.domain.usecase.PointDateUseCase
import com.onbada.seathermo.managers.FDFileManager
import kotlinx.coroutines.launch

/**
 * 포인트 날짜 목록 화면
 * Point Date List Activity
 *
 * 사용자가 날짜(폴더)를 선택하는 화면
 */
class PointDateListActivity : AppCompatActivity() {

    private lateinit var viewModel: PointDateListViewModel
    private lateinit var adapter: PointDateAdapter
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_point_date_list)

        initializeViews()
        initializeViewModel()
        observeViewModel()
    }

    private fun initializeViews() {
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)

        adapter = PointDateAdapter { pointDate ->
            // 날짜 선택 시 상세 목록(순번 선택) 화면으로 이동
            navigateToDataList(pointDate)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun initializeViewModel() {
        // DI Container가 없으므로 수동 주입
        val fileManager = FDFileManager.getInstance(applicationContext)
        val storage = FileDataStorageImpl(fileManager)
        val repository = DefaultPointDateListRepository(storage)
        val useCase = PointDateUseCase(repository)
        
        val factory = PointDateListViewModelFactory(useCase)
        viewModel = androidx.lifecycle.ViewModelProvider(this, factory).get(PointDateListViewModel::class.java)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.pointDateList.collect { list ->
                adapter.submitList(list)
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { message ->
                if (message != null) {
                    Toast.makeText(this@PointDateListActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun navigateToDataList(pointDate: PointDate) {
        // TODO: PointDataListActivity 구현 후 연결
        // val intent = Intent(this, PointDataListActivity::class.java)
        // intent.putExtra("POINT_DATE_PATH", pointDate.datePath?.absolutePath)
        // startActivity(intent)
        
        Toast.makeText(this, "Selected: ${pointDate.date}", Toast.LENGTH_SHORT).show()
        
        // 임시: 순번 선택 화면 구현 전까지 메시지만 표시
        // Step_2에서 DataListActivity 만들면서 주석 해제 예정
        val intent = Intent(this, PointDataListActivity::class.java)
        intent.putExtra("POINT_DATE_PATH", pointDate.datePath?.absolutePath)
        intent.putExtra("POINT_DATE_NAME", pointDate.date)
        startActivity(intent)
    }
}
