package com.app.unfit20.repository

import com.app.unfit20.model.ProductInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service to fetch data from an external API
 */
class ExternalApiService {

    // Base URL for the Dummy JSON API which provides product data
    private val baseUrl = "https://dummyjson.com/products"

    /**
     * Fetches ALL products
     */
    suspend fun getAllProducts(): List<ProductInfo> = withContext(Dispatchers.IO) {
        val url = URL(baseUrl)  // e.g. https://dummyjson.com/products
        val connection = url.openConnection() as HttpURLConnection

        return@withContext try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = connection.inputStream.bufferedReader()
                val response = reader.readText()
                reader.close()

                // Parse JSON
                val jsonObject = JSONObject(response)
                val productsArray = jsonObject.getJSONArray("products")

                val products = mutableListOf<ProductInfo>()
                for (i in 0 until productsArray.length()) {
                    val productObj = productsArray.getJSONObject(i)
                    val product = ProductInfo(
                        id = productObj.getString("id"),
                        title = productObj.getString("title"),
                        price = productObj.getDouble("price"),
                        description = productObj.getString("description"),
                        category = productObj.getString("category"),
                        imageUrl = productObj.getString("thumbnail"),
                        rating = productObj.getDouble("rating")
                    )
                    products.add(product)
                }
                products
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList<ProductInfo>()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Fetches similar product information based on category
     * @param category The product category to search for
     * @return List of similar products
     */
    suspend fun getSimilarProducts(category: String): List<ProductInfo> = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/category/$category")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                val reader = connection.inputStream.bufferedReader()
                val response = reader.readText()
                reader.close()

                // Parse JSON
                val jsonObject = JSONObject(response)
                val productsArray = jsonObject.getJSONArray("products")

                val products = mutableListOf<ProductInfo>()

                // Parse each product
                for (i in 0 until productsArray.length()) {
                    val productObj = productsArray.getJSONObject(i)
                    val product = ProductInfo(
                        id = productObj.getString("id"),
                        title = productObj.getString("title"),
                        price = productObj.getDouble("price"),
                        description = productObj.getString("description"),
                        category = productObj.getString("category"),
                        imageUrl = productObj.getString("thumbnail"),
                        rating = productObj.getDouble("rating")
                    )
                    products.add(product)
                }

                return@withContext products
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Searches for products
     * @param query The search query
     * @return List of products matching the query
     */
    suspend fun searchProducts(query: String): List<ProductInfo> = withContext(Dispatchers.IO) {
        val url = URL("$baseUrl/search?q=$query")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                val reader = connection.inputStream.bufferedReader()
                val response = reader.readText()
                reader.close()

                // Parse JSON
                val jsonObject = JSONObject(response)
                val productsArray = jsonObject.getJSONArray("products")

                val products = mutableListOf<ProductInfo>()

                // Parse each product
                for (i in 0 until productsArray.length()) {
                    val productObj = productsArray.getJSONObject(i)
                    val product = ProductInfo(
                        id = productObj.getString("id"),
                        title = productObj.getString("title"),
                        price = productObj.getDouble("price"),
                        description = productObj.getString("description"),
                        category = productObj.getString("category"),
                        imageUrl = productObj.getString("thumbnail"),
                        rating = productObj.getDouble("rating")
                    )
                    products.add(product)
                }

                return@withContext products
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}