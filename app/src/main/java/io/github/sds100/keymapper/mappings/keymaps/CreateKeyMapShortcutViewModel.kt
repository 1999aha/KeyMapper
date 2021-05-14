package io.github.sds100.keymapper.mappings.keymaps

import android.content.Intent
import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 08/09/20.
 */
class CreateKeyMapShortcutViewModel(
    private val configKeyMapUseCase: ConfigKeyMapUseCase,
    private val listUseCase: ListKeyMapsUseCase,
    private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), PopupViewModel by PopupViewModelImpl(),
    ResourceProvider by resourceProvider {

    private val listItemCreator = KeyMapListItemCreator(listUseCase, resourceProvider)

    private val _state = MutableStateFlow<State<List<KeyMapListItem>>>(State.Loading)
    val state = _state.asStateFlow()

    private val _returnIntentResult = MutableSharedFlow<Intent>()
    val returnUidResult = _returnIntentResult.asSharedFlow()

    init {
        val rebuildUiState = MutableSharedFlow<State<List<KeyMap>>>(replay = 1)

        combine(
            rebuildUiState,
            listUseCase.showDeviceDescriptors
        ) { keyMapListState, showDeviceDescriptors ->
            val selectionUiState =
                KeyMapListItem.SelectionUiState(isSelected = false, isSelectable = false)

            _state.value = keyMapListState.mapData { keyMapList ->
                keyMapList.map { keyMap ->
                    val keyMapListUiState = listItemCreator.create(keyMap, showDeviceDescriptors)

                    KeyMapListItem(keyMapListUiState, selectionUiState)
                }
            }
        }.launchIn(viewModelScope)

        viewModelScope.launch {
            listUseCase.keyMapList.collectLatest {
                rebuildUiState.emit(it)
            }
        }

        viewModelScope.launch {
            listUseCase.invalidateActionErrors.drop(1).collectLatest {
                /*
                Don't get the key maps from the repository because there can be a race condition
                when restoring key maps. This happens because when the activity is resumed the
                key maps in the repository are being updated and this flow is collected
                at the same time.
                 */
                rebuildUiState.emit(rebuildUiState.first())
            }
        }
    }

    fun onKeyMapCardClick(uid: String) {
        viewModelScope.launch {
            val state = listUseCase.keyMapList.first { it is State.Data }

            if (state !is State.Data) return@launch

            configKeyMapUseCase.loadKeyMap(uid)
            configKeyMapUseCase.setTriggerFromOtherAppsEnabled(true)

            val keyMapState = configKeyMapUseCase.mapping.first()

            if (keyMapState !is State.Data) return@launch

            val keyMap = keyMapState.data

            val intent = if (keyMap.actionList.size == 1) {
                createShortcutUseCase.createIntentForSingleAction(keyMap.uid, keyMap.actionList[0])
            } else {

                val key = "create_launcher_shortcut"
                val response = showPopup(
                    key,
                    PopupUi.Text(
                        getString(R.string.hint_shortcut_name),
                        allowEmpty = false
                    )
                ) ?: return@launch

                createShortcutUseCase.createIntentForMultipleActions(
                    keyMapUid = keyMap.uid,
                    shortcutLabel = response.text
                )
            }

            configKeyMapUseCase.save()

            _returnIntentResult.emit(intent)
        }
    }

    fun onTriggerErrorChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    fun onActionChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    fun onConstraintsChipClick(chipModel: ChipUi) {
        if (chipModel is ChipUi.Error) {
            showSnackBarAndFixError(chipModel.error)
        }
    }

    private fun showSnackBarAndFixError(error: Error) {
        viewModelScope.launch {
            val actionText = if (error.isFixable) {
                getString(R.string.snackbar_fix)
            } else {
                null
            }

            val snackBar = PopupUi.SnackBar(
                message = error.getFullMessage(this@CreateKeyMapShortcutViewModel),
                actionText = actionText
            )

            showPopup("fix_error", snackBar) ?: return@launch

            if (error.isFixable) {
                listUseCase.fixError(error)
            }
        }
    }

    class Factory(
        private val configKeyMapUseCase: ConfigKeyMapUseCase,
        private val listUseCase: ListKeyMapsUseCase,
        private val createShortcutUseCase: CreateKeyMapShortcutUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            CreateKeyMapShortcutViewModel(
                configKeyMapUseCase, listUseCase, createShortcutUseCase, resourceProvider
            ) as T
    }
}