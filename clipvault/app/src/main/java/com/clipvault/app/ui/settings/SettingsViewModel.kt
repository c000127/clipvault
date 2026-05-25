package com.clipvault.app.ui.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.clipvault.app.data.local.AppDatabase
import com.clipvault.app.data.local.entity.ClipItem
import com.clipvault.app.data.local.entity.ItemTag
import com.clipvault.app.data.local.entity.Tag
import com.clipvault.app.data.repository.AiProviderRepository
import com.clipvault.app.data.repository.ClipItemRepository
import com.clipvault.app.data.repository.TagRepository
import com.clipvault.app.ui.theme.ThemeMode
import com.clipvault.app.ui.theme.ThemePreferences
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ExportData(
    val version: Int = 1,
    val exportedAt: String = "",
    val items: List<ClipItem> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val itemTags: List<ItemTag> = emptyList()
)

sealed interface ImportMode {
    data object Overwrite : ImportMode
    data object Merge : ImportMode
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val clipItemRepository: ClipItemRepository,
    private val tagRepository: TagRepository,
    private val aiProviderRepository: AiProviderRepository,
    private val themePreferences: ThemePreferences
) : ViewModel() {

    private val gson = Gson()

    init {
        Log.d("SettingsVM", "init: start")
        Log.d("SettingsVM", "init: themeMode subscribed")
    }

    val themeMode: StateFlow<ThemeMode> = try {
        themePreferences.themeMode
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ThemeMode.FOLLOW_SYSTEM
            )
    } catch (e: Exception) {
        MutableStateFlow(ThemeMode.FOLLOW_SYSTEM)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun exportData(uri: Uri) {
        Log.d("SettingsVM", "exportData: start")
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            try {
                val itemsList = withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(5000) {
                        clipItemRepository.getAllFlow().first()
                    } ?: emptyList()
                }

                val allTags = withContext(Dispatchers.IO) {
                    tagRepository.getAllTagsOnce()
                }

                val allItemTags = mutableListOf<ItemTag>()
                withContext(Dispatchers.IO) {
                    for (item in itemsList) {
                        val tags = clipItemRepository.getTagsForItemOnce(item.id)
                        tags.forEach { tag ->
                            allItemTags.add(ItemTag(itemId = item.id, tagId = tag.id))
                        }
                    }
                }

                Log.d("SettingsVM", "exportData: items=${itemsList.size}, tags=${allTags.size}")
                val exportData = ExportData(
                    version = 1,
                    exportedAt = java.time.Instant.now().toString(),
                    items = itemsList,
                    tags = allTags,
                    itemTags = allItemTags
                )

                val json = withContext(Dispatchers.Default) {
                    gson.toJson(exportData)
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(json.toByteArray())
                    }
                }

                Log.d("SettingsVM", "exportData: done")
                _exportState.value = ExportState.Success("Exported ${itemsList.size} items, ${allTags.size} tags")
            } catch (e: Exception) {
                _exportState.value = ExportState.Error("Export failed: ${e.message}")
            }
        }
    }

    fun importData(uri: Uri, mode: ImportMode) {
        Log.d("SettingsVM", "importData: start mode=$mode")
        viewModelScope.launch {
            _importState.value = ImportState.Loading
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw IllegalStateException("Cannot read file")
                }

                val exportData = withContext(Dispatchers.Default) {
                    gson.fromJson(json, ExportData::class.java)
                }

                Log.d("SettingsVM", "importData: parsed items=${exportData.items.size}")
                val existingItems = withContext(Dispatchers.IO) {
                    clipItemRepository.getAllFlow().first()
                }
                val existingTags = withContext(Dispatchers.IO) {
                    tagRepository.getAllTagsOnce()
                }

                withContext(Dispatchers.IO) {
                    database.withTransaction {
                        when (mode) {
                            is ImportMode.Overwrite -> importOverwrite(exportData, existingItems, existingTags)
                            is ImportMode.Merge -> importMerge(exportData)
                        }
                    }
                }

                Log.d("SettingsVM", "importData: done")
                _importState.value = ImportState.Success(
                    "Imported ${exportData.items.size} items, ${exportData.tags.size} tags"
                )
            } catch (e: Exception) {
                _importState.value = ImportState.Error("Import failed: ${e.message}")
            }
        }
    }

    private suspend fun importOverwrite(data: ExportData, existingItems: List<ClipItem>, existingTags: List<Tag>) {
        // Clear all existing data
        clipItemRepository.deleteByIds(existingItems.map { it.id })
        existingTags.forEach { tagRepository.delete(it) }

        // Insert new data with original IDs preserved
        tagRepository.insertAll(data.tags)
        clipItemRepository.insertAll(data.items)
        data.itemTags.forEach { clipItemRepository.addTagToItem(it.itemId, it.tagId) }
    }

    private suspend fun importMerge(data: ExportData) {
        // Build old ID -> new ID mapping for tags
        val tagIdMap = mutableMapOf<Long, Long>()
        for (tag in data.tags) {
            // Check if tag already exists by name+parentId
            val existing = tagRepository.getAllTagsOnce().find {
                it.name == tag.name && it.parentId == tag.parentId
            }
            if (existing != null) {
                tagIdMap[tag.id] = existing.id
            } else {
                val newId = tagRepository.insert(tag.copy(id = 0))
                tagIdMap[tag.id] = newId
            }
        }

        // Insert items with new IDs
        val itemIdMap = mutableMapOf<Long, Long>()
        for (item in data.items) {
            val newId = clipItemRepository.insert(item.copy(id = 0))
            itemIdMap[item.id] = newId
        }

        // Rebuild item-tag associations
        for (itemTag in data.itemTags) {
            val newItemId = itemIdMap[itemTag.itemId] ?: continue
            val newTagId = tagIdMap[itemTag.tagId] ?: continue
            clipItemRepository.addTagToItem(newItemId, newTagId)
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

sealed interface ExportState {
    data object Idle : ExportState
    data object Loading : ExportState
    data class Success(val message: String) : ExportState
    data class Error(val message: String) : ExportState
}

sealed interface ImportState {
    data object Idle : ImportState
    data object Loading : ImportState
    data class Success(val message: String) : ImportState
    data class Error(val message: String) : ImportState
}
