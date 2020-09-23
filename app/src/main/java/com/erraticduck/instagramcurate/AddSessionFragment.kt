package com.erraticduck.instagramcurate

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.erraticduck.instagramcurate.domain.Session
import com.erraticduck.instagramcurate.gateway.SessionGateway
import com.erraticduck.instagramcurate.sync.InstagramWorker
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddSessionFragment : BottomSheetDialogFragment() {

    private lateinit var searchQuery: EditText
    private lateinit var searchType: RadioGroup

    private val gateway by lazy { SessionGateway(MainApplication.instance.searchSessionDatabase.sessionDao()) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setCanceledOnTouchOutside(false)
        dialog.dismissWithAnimation = true
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.f_add_session, container, false)
        val searchQueryLayout = view.findViewById<TextInputLayout>(R.id.search_query_layout)
        searchQuery = view.findViewById(R.id.search_query)
        searchQuery.doAfterTextChanged {
            if (!isSearchQueryValid()) {
                searchQueryLayout.error = getString(R.string.error_input)
            } else {
                searchQueryLayout.error = null
            }
        }

        searchType = view.findViewById(R.id.search_type)
        val performMlCheckBox = view.findViewById<CheckBox>(R.id.perform_ml)
        view.findViewById<View>(R.id.btn_add).setOnClickListener {
            if (isValid()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val id = gateway.addSession(
                        Session(
                            searchQuery.text.toString(),
                            when (searchType.checkedRadioButtonId) {
                                R.id.radio_hashtag -> Session.Type.HASHTAG
                                R.id.radio_person -> Session.Type.PERSON
                                else -> throw IllegalStateException("No such view ID")
                            }
                        )
                    )

                    InstagramWorker.enqueue(WorkManager.getInstance(view.context), id, performMlCheckBox.isChecked)
                }
                dismiss()
            }
        }
        return view
    }

    private fun isSearchQueryValid() = searchQuery.text.matches(Regex("[a-z0-9_.]+", RegexOption.IGNORE_CASE))

    private fun isValid() = isSearchQueryValid() && searchType.checkedRadioButtonId != -1

    companion object {
        fun show(fragmentManager: FragmentManager) = AddSessionFragment().show(fragmentManager, "add_session_fragment")
    }
}