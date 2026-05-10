package com.example.caresync

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class HealthTipState {
    object Loading : HealthTipState()
    data class Success(val tips: List<HealthItem>) : HealthTipState()
    data class Error(val message: String) : HealthTipState()
}

class HealthTipViewModel : ViewModel() {

    private val _healthTipState = MutableLiveData<HealthTipState>()
    val healthTipState: LiveData<HealthTipState> = _healthTipState

    // different health topics to rotate through
    private val topicSets = listOf(
        "Nutrition|Exercise|Sleep",
        "Diabetes|Cardiology|Mental_health",
        "Vaccination|Hypertension|Obesity",
        "Cancer|Asthma|Alzheimer's_disease"
    )
    private var currentTopicIndex = 0

    fun fetchHealthTips(keyword: String = "") {
        _healthTipState.value = HealthTipState.Loading

        // pick topic set — either based on keyword or rotate
        val topics = if (keyword.isNotEmpty()) {
            keyword.replaceFirstChar { it.uppercase() }
        } else {
            topicSets[currentTopicIndex]
        }

        viewModelScope.launch {
            try {
                val response = RetrofitClient.healthApiService
                    .getHealthInfo(titles = topics)

                if (response.isSuccessful) {
                    val pages = response.body()?.query?.pages

                    if (pages != null) {
                        // convert Wikipedia pages to our HealthItem list
                        val tips = pages.values
                            .filter { it.extract != null }
                            .map { page ->
                                HealthItem(
                                    Title = page.title,
                                    MyHFDescription = page.extract
                                        ?.take(150)
                                        ?.plus("...") ?: "No description",
                                    // take(150) = first 150 chars as preview
                                    AccessibleVersion = page.fullurl
                                        ?: "https://en.wikipedia.org/wiki/${page.title}"
                                )
                            }
                        _healthTipState.postValue(HealthTipState.Success(tips))
                    } else {
                        _healthTipState.postValue(
                            HealthTipState.Error("No data found")
                        )
                    }
                } else {
                    _healthTipState.postValue(
                        HealthTipState.Error("Server error: ${response.code()}")
                    )
                }
            } catch (e: Exception) {
                // print exact error to logcat for debugging
                android.util.Log.e("RETROFIT", "Error: ${e.message}", e)
                _healthTipState.postValue(
                    HealthTipState.Error("Error: ${e.message}")
                )
            }
        }
    }

    fun fetchNextTopics() {
        // rotate to next set of topics
        currentTopicIndex = (currentTopicIndex + 1) % topicSets.size
        fetchHealthTips()
    }
}