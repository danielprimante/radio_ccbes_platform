package com.radio.ccbes.ui.screens.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.radio.ccbes.data.cache.AppDatabase
import com.radio.ccbes.data.model.Post
import com.radio.ccbes.data.repository.PostRepository
import com.radio.ccbes.data.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository: PostRepository
    private val userRepository = UserRepository()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _adminPosts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _adminPosts.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        postRepository = PostRepository(database.postDao(), userRepository)
        loadNews()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadNews()
            _isRefreshing.value = false
        }
    }

    private fun loadNews() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Obtenemos todos los posts (simplificado para este caso)
                // En una app real, Firestore debería tener un índice para filtrar por role del autor
                // Pero como no podemos cambiar el esquema de Firestore fácilmente sin consola,
                // vamos a filtrar los posts que ya tenemos en el feed o traer los últimos y filtrar.
                
                // Opción más eficiente con el repositorio actual: 
                // Traer los últimos 50 posts y filtrar por aquellos cuyos autores son admin.
                
                // NOTA: Para que esto sea 100% correcto el filtrado debería ser en el backend.
                // Aquí simularemos el anclaje de noticias oficiales.
                
                val allPostsFlow = postRepository.getPostsByCategory(com.radio.ccbes.data.model.PostCategory.ALL)
                allPostsFlow.collect { allPosts ->
                    // Filtrar por administradores (usando el handle @ccbes como ejemplo de admin principal 
                    // o verificando el rol si el objeto Post lo tuviera, pero Post no tiene rol)
                    // Tendremos que verificar el rol del usuario de cada post.
                    
                    val adminUserIds = mutableSetOf<String>()
                    val authorIds = allPosts.map { it.userId }.distinct()
                    
                    val users = userRepository.getUsersByIds(authorIds)
                    users.forEach { user ->
                        if (user.role == "admin") {
                            adminUserIds.add(user.id)
                        }
                    }
                    
                    val filtered = allPosts.filter { adminUserIds.contains(it.userId) }
                    _adminPosts.value = filtered
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }
}
