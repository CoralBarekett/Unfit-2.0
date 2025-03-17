package com.app.unfit20.ui.auth

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.unfit20.model.User
import com.app.unfit20.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val userRepository = UserRepository()

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> = _loginResult

    private val _signUpResult = MutableLiveData<Boolean>()
    val signUpResult: LiveData<Boolean> = _signUpResult

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _updateProfileResult = MutableLiveData<Boolean>()
    val updateProfileResult: LiveData<Boolean> = _updateProfileResult

    init {
        loadCurrentUser()
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val success = userRepository.login(email, password)
                _loginResult.value = success
                if (success) {
                    loadCurrentUser()
                }
            } catch (e: Exception) {
                _loginResult.value = false
            }
        }
    }

    fun signUp(email: String, password: String, userName: String) {
        viewModelScope.launch {
            try {
                val success = userRepository.signUp(email, password, userName)
                _signUpResult.value = success
                if (success) {
                    loadCurrentUser()
                }
            } catch (e: Exception) {
                _signUpResult.value = false
            }
        }
    }

    fun logout() {
        userRepository.logout()
        _currentUser.value = null
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = userRepository.getCurrentUser()
        }
    }

    fun updateProfile(userName: String, photoUri: Uri?) {
        viewModelScope.launch {
            val result = userRepository.updateProfile(userName, photoUri)
            _updateProfileResult.value = result
            if (result) {
                loadCurrentUser()
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return userRepository.getCurrentUserId() != null
    }
}