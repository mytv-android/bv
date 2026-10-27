package com.github.mytv.bv.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dev.aaa1115910.biliapi.entity.user.DynamicItem
import dev.aaa1115910.biliapi.repositories.UserRepository
import com.github.mytv.bv.BVApp
import com.github.mytv.bv.util.Prefs
import com.github.mytv.bv.util.fException
import com.github.mytv.bv.util.fInfo
import com.github.mytv.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class DynamicDetailViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    var dynamicId by mutableStateOf("")
    var dynamicItem by mutableStateOf<DynamicItem?>(null)

    suspend fun loadDynamic() {
        logger.fInfo { "Loading dynamic detail: $dynamicId" }
        runCatching {
            dynamicItem = userRepository.getDynamicDetail(
                dynamicId = dynamicId,
                preferApiType = Prefs.apiType
            )
        }.onFailure {
            logger.fException(it) { "Failed to load dynamic" }
            withContext(Dispatchers.Main) {
                "Failed to load dynamic: ${it.message}".toast(BVApp.context)
            }
        }
    }
}