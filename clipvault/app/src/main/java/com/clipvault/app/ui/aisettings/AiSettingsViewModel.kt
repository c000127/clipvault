package com.clipvault.app.ui.aisettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clipvault.app.data.local.entity.AiProvider
import com.clipvault.app.data.local.entity.DEFAULT_SYSTEM_PROMPT
import com.clipvault.app.data.remote.AiResult
import com.clipvault.app.data.remote.AiService
import com.clipvault.app.data.repository.AiProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderFormState(
    val id: Long = 0,
    val name: String = "",
    val baseUrl: String = "",
    val apiKey: String = "",
    val modelName: String = "",
    val supportsVision: Boolean = false,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
)

sealed interface TestState {
    data object Idle : TestState
    data object Loading : TestState
    data class Success(val message: String) : TestState
    data class Error(val message: String) : TestState
}

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val aiProviderRepository: AiProviderRepository,
    private val aiService: AiService
) : ViewModel() {

    private val _providers = MutableStateFlow<List<AiProvider>>(emptyList())
    val providers: StateFlow<List<AiProvider>> = _providers.asStateFlow()

    private val _showForm = MutableStateFlow(false)
    val showForm: StateFlow<Boolean> = _showForm.asStateFlow()

    private val _formState = MutableStateFlow(ProviderFormState())
    val formState: StateFlow<ProviderFormState> = _formState.asStateFlow()

    private val _testState = MutableStateFlow<TestState>(TestState.Idle)
    val testState: StateFlow<TestState> = _testState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        loadProviders()
    }

    private fun loadProviders() {
        viewModelScope.launch {
            aiProviderRepository.getAllProviders().collect {
                _providers.value = it
            }
        }
    }

    fun showAddForm() {
        _formState.value = ProviderFormState()
        _showForm.value = true
    }

    fun showEditForm(provider: AiProvider) {
        _formState.value = ProviderFormState(
            id = provider.id,
            name = provider.name,
            baseUrl = provider.baseUrl,
            apiKey = "", // Don't show stored key
            modelName = provider.modelName,
            supportsVision = provider.supportsVision,
            maxTokens = provider.maxTokens,
            temperature = provider.temperature,
            systemPrompt = provider.systemPrompt
        )
        _showForm.value = true
    }

    fun hideForm() {
        _showForm.value = false
        _testState.value = TestState.Idle
    }

    fun updateFormField(field: String, value: Any) {
        _formState.value = when (field) {
            "name" -> _formState.value.copy(name = value as String)
            "baseUrl" -> _formState.value.copy(baseUrl = value as String)
            "apiKey" -> _formState.value.copy(apiKey = value as String)
            "modelName" -> _formState.value.copy(modelName = value as String)
            "supportsVision" -> _formState.value.copy(supportsVision = value as Boolean)
            "maxTokens" -> _formState.value.copy(maxTokens = value as Int)
            "temperature" -> _formState.value.copy(temperature = value as Float)
            "systemPrompt" -> _formState.value.copy(systemPrompt = value as String)
            else -> _formState.value
        }
    }

    fun saveProvider() {
        val form = _formState.value
        if (form.name.isBlank() || form.baseUrl.isBlank() || form.modelName.isBlank()) {
            _message.value = "Please fill in all required fields"
            return
        }

        viewModelScope.launch {
            val provider = AiProvider(
                id = if (form.id > 0) form.id else 0,
                name = form.name,
                baseUrl = form.baseUrl.trimEnd('/'),
                apiKey = "", // Store empty in Room
                modelName = form.modelName,
                supportsVision = form.supportsVision,
                maxTokens = form.maxTokens,
                temperature = form.temperature,
                systemPrompt = form.systemPrompt
            )

            if (form.id > 0) {
                aiProviderRepository.update(provider)
                // Update API key if provided
                if (form.apiKey.isNotBlank()) {
                    aiProviderRepository.saveApiKey(form.id, form.apiKey)
                }
                _message.value = "Provider updated"
            } else {
                val newId = aiProviderRepository.insert(provider)
                if (form.apiKey.isNotBlank()) {
                    aiProviderRepository.saveApiKey(newId, form.apiKey)
                }
                _message.value = "Provider created"
            }

            hideForm()
        }
    }

    fun deleteProvider(provider: AiProvider) {
        viewModelScope.launch {
            aiProviderRepository.delete(provider)
            aiProviderRepository.deleteApiKey(provider.id)
            _message.value = "Provider deleted"
        }
    }

    fun setActiveProvider(providerId: Long) {
        viewModelScope.launch {
            aiProviderRepository.setActiveProvider(providerId)
            _message.value = "Provider activated"
        }
    }

    fun testConnection() {
        val form = _formState.value
        if (form.apiKey.isBlank()) {
            _testState.value = TestState.Error("Please enter an API Key")
            return
        }

        viewModelScope.launch {
            _testState.value = TestState.Loading
            val provider = AiProvider(
                name = form.name,
                baseUrl = form.baseUrl.trimEnd('/'),
                modelName = form.modelName,
                supportsVision = form.supportsVision,
                maxTokens = form.maxTokens,
                temperature = form.temperature,
                systemPrompt = form.systemPrompt
            )

            val result = aiService.testConnection(provider, form.apiKey)
            _testState.value = when (result) {
                is AiResult.Success -> TestState.Success("Connection successful!")
                is AiResult.Error -> TestState.Error(result.message)
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
