package io.github.sds100.keymapper.mappings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.Lifecycle
import com.airbnb.epoxy.EpoxyRecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.divider
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.ui.utils.configuredCheckBox
import io.github.sds100.keymapper.ui.utils.configuredRadioButtonPair
import io.github.sds100.keymapper.ui.utils.configuredRadioButtonTriple
import io.github.sds100.keymapper.ui.utils.configuredSlider
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

/**
 * Created by sds100 on 12/04/2021.
 */
abstract class OptionsBottomSheetFragment<BINDING : ViewDataBinding> : BottomSheetDialogFragment() {

    abstract val helpUrl: String
    abstract val viewModel: OptionsViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: BINDING? = null
    val binding: BINDING
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = bind(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.state.collectLatest { state ->
                getProgressBar(binding).isVisible = state.showProgressBar
                populateList(state.listItems)
            }
        }

        getHelpButton(binding).setOnClickListener {
            UrlUtils.openUrl(requireContext(), helpUrl)
        }

        getDoneButton(binding).setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun populateList(listItems: List<ListItem>) {
        Timber.e("populate list $listItems")
        getRecyclerView(binding).withModels {
            listItems.forEach { model ->
                if (model is RadioButtonPairListItem) {
                    configuredRadioButtonPair(model) { id, isChecked ->
                        viewModel.setRadioButtonValue(id, isChecked)
                    }
                }

                if (model is RadioButtonTripleListItem) {
                    configuredRadioButtonTriple(model) { id, isChecked ->
                        viewModel.setRadioButtonValue(id, isChecked)
                    }
                }

                if (model is CheckBoxListItem) {
                    configuredCheckBox(model) {
                        Timber.e(("on checked change $model $it"))
                        viewModel.setCheckboxValue(model.id, it)
                    }
                }

                if (model is SliderListItem) {
                    configuredSlider(this@OptionsBottomSheetFragment, model) {
                        viewModel.setSliderValue(model.id, it)
                    }
                }

                if (model is DividerListItem) {
                    divider { id(model.id) }
                }
            }
        }
    }

    abstract fun getRecyclerView(binding: BINDING): EpoxyRecyclerView
    abstract fun getProgressBar(binding: BINDING): View
    abstract fun getDoneButton(binding: BINDING): View
    abstract fun getHelpButton(binding: BINDING): View

    abstract fun bind(inflater: LayoutInflater, container: ViewGroup?): BINDING
}