package io.github.sds100.keymapper.shizuku

import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import rikka.shizuku.Shizuku

/**
 * Created by sds100 on 20/07/2021.
 */
class ShizukuAdapterImpl(
    private val coroutineScope: CoroutineScope,
    private val packageManagerAdapter: PackageManagerAdapter
) : ShizukuAdapter {
    override val isStarted = MutableStateFlow(Shizuku.getBinder() != null)

    override val isInstalled: StateFlow<Boolean> =
        packageManagerAdapter.installedPackages
            .filter { it is State.Data }
            .map { state ->
                require(state is State.Data)

                state.data.any { it.packageName == ShizukuUtils.SHIZUKU_PACKAGE }
            }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(),
                packageManagerAdapter.isAppInstalled(ShizukuUtils.SHIZUKU_PACKAGE)
            )

    init {
        Shizuku.addBinderReceivedListener {
            isStarted.value = Shizuku.getBinder() != null
        }

        Shizuku.addBinderDeadListener {
            isStarted.value = Shizuku.getBinder() != null
        }
    }
}