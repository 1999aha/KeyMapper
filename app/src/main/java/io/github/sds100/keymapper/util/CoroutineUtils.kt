package io.github.sds100.keymapper.util

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by sds100 on 18/11/20.
 */

val Fragment.viewLifecycleScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycle.coroutineScope

fun LifecycleOwner.launchRepeatOnLifecycle(
    state: Lifecycle.State,
    block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        this@launchRepeatOnLifecycle.repeatOnLifecycle(state, block)
    }
}

fun <T> Flow<T>.firstBlocking(): T = runBlocking { first() }