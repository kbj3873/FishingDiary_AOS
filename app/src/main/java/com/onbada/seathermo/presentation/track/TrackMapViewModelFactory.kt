package com.onbada.seathermo.presentation.track

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.onbada.seathermo.domain.usecase.TrackMapUseCase

/**
 * TrackMapViewModel Factory
 */
class TrackMapViewModelFactory(
    private val context: android.content.Context,
    private val trackMapUseCase: TrackMapUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackMapViewModel(context, trackMapUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
