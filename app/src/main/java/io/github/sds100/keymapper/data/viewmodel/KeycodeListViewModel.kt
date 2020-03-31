package io.github.sds100.keymapper.data.viewmodel

import android.view.KeyEvent
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.util.KeycodeUtils
import java.util.*

/**
 * Created by sds100 on 31/03/2020.
 */

class KeycodeListViewModel : ViewModel() {

    private val mKeycodeLabelMap = MutableLiveData<Map<Int, String>>().apply {
        value = sequence {
            KeycodeUtils.getKeyCodes().forEach {
                yield(it to "$it \t\t ${KeyEvent.keyCodeToString(it)}")
            }
        }.sortedBy { it.first }.toMap()
    }

    val searchQuery: MutableLiveData<String> = MutableLiveData("")

    val filteredKeycodeLabelList = MediatorLiveData<Map<Int, String>>().apply {
        fun filter(query: String) {
            value = mKeycodeLabelMap.value?.filter {
                it.value.toLowerCase(Locale.getDefault()).contains(query)
            } ?: mapOf()
        }

        addSource(searchQuery) { query ->
            filter(query)
        }

        addSource(mKeycodeLabelMap) {
            value = it

            searchQuery.value?.let { query ->
                filter(query)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return KeycodeListViewModel() as T
        }
    }
}