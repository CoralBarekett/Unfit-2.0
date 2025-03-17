package com.app.unfit20.ui.marketplace

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.unfit20.model.ProductInfo
import com.app.unfit20.repository.ExternalApiService
import kotlinx.coroutines.launch

class MarketplaceViewModel : ViewModel() {

    private val externalApiService = ExternalApiService()

    private val _similarProducts = MutableLiveData<List<ProductInfo>>()
    val similarProducts: LiveData<List<ProductInfo>> = _similarProducts

    private val _searchResults = MutableLiveData<List<ProductInfo>>()
    val searchResults: LiveData<List<ProductInfo>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadSimilarProducts(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val products = externalApiService.getSimilarProducts(category)
                _similarProducts.value = products
            } catch (e: Exception) {
                // Handle error
                _similarProducts.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchProducts(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val products = externalApiService.searchProducts(query)
                _searchResults.value = products
            } catch (e: Exception) {
                // Handle error
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}