package com.app.unfit20.ui.post

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.app.unfit20.databinding.DialogAddCommentBinding
import com.app.unfit20.ui.ViewModelFactory

class AddCommentDialogFragment : DialogFragment() {

    private var _binding: DialogAddCommentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PostViewModel by viewModels { ViewModelFactory() }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogAddCommentBinding.inflate(LayoutInflater.from(context))

        val postId = requireArguments().getString(ARG_POST_ID) ?: return super.onCreateDialog(savedInstanceState)

        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Add a comment")
            .setView(binding.root)
            .setPositiveButton("Send") { _, _ ->
                val commentText = binding.etComment.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    viewModel.addComment(postId, commentText)
                }
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_POST_ID = "postId"

        fun newInstance(postId: String): AddCommentDialogFragment {
            val fragment = AddCommentDialogFragment()
            val args = Bundle().apply {
                putString(ARG_POST_ID, postId)
            }
            fragment.arguments = args
            return fragment
        }
    }
}