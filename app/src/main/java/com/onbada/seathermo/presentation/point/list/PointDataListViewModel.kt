package com.onbada.seathermo.presentation.point.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onbada.seathermo.domain.entity.PointData
import com.onbada.seathermo.domain.entity.PointDate
import com.onbada.seathermo.domain.usecase.FetchDataListRequestValue
import com.onbada.seathermo.domain.usecase.PointDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class PointDataListViewModel(
    private val pointDataUseCase: PointDataUseCase
) : ViewModel() {

    private val _pointDataList = MutableStateFlow<List<PointData>>(emptyList())
    val pointDataList: StateFlow<List<PointData>> = _pointDataList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchDataList(datePath: String, dateName: String) {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        // PointDate 객체를 재구성 (Repository가 path를 기반으로 동작하므로)
        val pointDate = PointDate(dateName, File(datePath))
        val requestValue = FetchDataListRequestValue(pointDate)

        pointDataUseCase.executeFetchDataList(requestValue) { result ->
            result.onSuccess { list ->
                _pointDataList.value = list
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Unknown error"
            }
            _isLoading.value = false
        }
    }
}

class PointDataListViewModelFactory(
    private val pointDataUseCase: PointDataUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PointDataListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PointDataListViewModel(pointDataUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
