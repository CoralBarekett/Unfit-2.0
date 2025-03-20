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

    // Main product list (e.g. all products or search results)
    private val _products = MutableLiveData<List<ProductInfo>>()
    val products: LiveData<List<ProductInfo>> = _products

    // If you want to store “similar products” separately
    private val _similarProducts = MutableLiveData<List<ProductInfo>>()
    val similarProducts: LiveData<List<ProductInfo>> = _similarProducts

    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Error messages
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Loads all products from the API (Dummy JSON).
     */
    fun loadProducts() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val allProducts = externalApiService.getAllProducts()
                _products.value = allProducts
            } catch (e: Exception) {
                _products.value = emptyList()
                _errorMessage.value = e.message ?: "Failed to load products"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads products similar to a given category.
     * Example usage: loadSimilarProducts("smartphones").
     */
    fun loadSimilarProducts(category: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val products = externalApiService.getSimilarProducts(category)
                _similarProducts.value = products
            } catch (e: Exception) {
                _similarProducts.value = emptyList()
                _errorMessage.value = e.message ?: "Failed to load similar products"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Searches products by a query string.
     */
    fun searchProducts(query: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val results = externalApiService.searchProducts(query)
                // You can either update the main _products or keep a separate LiveData for searches.
                _products.value = results
            } catch (e: Exception) {
                _products.value = emptyList()
                _errorMessage.value = e.message ?: "Search failed"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears any error message after displaying it.
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}