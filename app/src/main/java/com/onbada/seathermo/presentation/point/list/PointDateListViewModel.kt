package com.onbada.seathermo.presentation.point.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onbada.seathermo.domain.entity.PointDate
import com.onbada.seathermo.domain.usecase.PointDateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 포인트 날짜 목록 ViewModel
 * Point Date List ViewModel
 *
 * iOS의 PointMainViewModel(추정)에 대응
 */
class PointDateListViewModel(
    private val pointDateUseCase: PointDateUseCase
) : ViewModel() {

    // 날짜 목록 상태
    private val _pointDateList = MutableStateFlow<List<PointDate>>(emptyList())
    val pointDateList: StateFlow<List<PointDate>> = _pointDateList.asStateFlow()

    // 로딩 상태
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 에러 메시지
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        fetchDateList()
    }

    fun fetchDateList() {
        if (_isLoading.value) return
        _isLoading.value = true
        _errorMessage.value = null

        pointDateUseCase.executeFetchDateList { result ->
            result.onSuccess { list ->
                _pointDateList.value = list
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Unknown error"
            }
            _isLoading.value = false
        }
    }
}

/**
 * ViewModel Factory
 */
class PointDateListViewModelFactory(
    private val pointDateUseCase: PointDateUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PointDateListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PointDateListViewModel(pointDateUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
