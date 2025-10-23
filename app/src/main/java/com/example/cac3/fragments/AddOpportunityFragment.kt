package com.example.cac3.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * Add Opportunity Fragment - Allow users to contribute new opportunities
 */
class AddOpportunityFragment : Fragment() {

    private lateinit var database: AppDatabase

    private lateinit var titleInput: TextInputEditText
    private lateinit var organizationInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categorySpinner: Spinner
    private lateinit var websiteInput: TextInputEditText
    private lateinit var costInput: TextInputEditText
    private lateinit var tagsInput: TextInputEditText
    private lateinit var submitButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_opportunity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        setupCategorySpinner()
        setupSubmitButton()
    }

    private fun initializeViews(view: View) {
        titleInput = view.findViewById(R.id.titleInput)
        organizationInput = view.findViewById(R.id.organizationInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        categorySpinner = view.findViewById(R.id.categorySpinner)
        websiteInput = view.findViewById(R.id.websiteInput)
        costInput = view.findViewById(R.id.costInput)
        tagsInput = view.findViewById(R.id.tagsInput)
        submitButton = view.findViewById(R.id.submitButton)
    }

    private fun setupCategorySpinner() {
        val categories = OpportunityCategory.values().map { formatCategory(it) }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            submitOpportunity()
        }
    }

    private fun submitOpportunity() {
        val title = titleInput.text.toString().trim()
        val organization = organizationInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val website = websiteInput.text.toString().trim()
        val cost = costInput.text.toString().trim()
        val tags = tagsInput.text.toString().trim()

        // Validation
        if (title.isEmpty()) {
            titleInput.error = "Title is required"
            titleInput.requestFocus()
            return
        }

        if (organization.isEmpty()) {
            organizationInput.error = "Organization is required"
            organizationInput.requestFocus()
            return
        }

        if (description.isEmpty()) {
            descriptionInput.error = "Description is required"
            descriptionInput.requestFocus()
            return
        }

        if (description.length < 20) {
            descriptionInput.error = "Description should be at least 20 characters"
            descriptionInput.requestFocus()
            return
        }

        // Get selected category
        val selectedCategoryPosition = categorySpinner.selectedItemPosition
        val category = OpportunityCategory.values()[selectedCategoryPosition]

        // Create opportunity object
        val opportunity = Opportunity(
            title = title,
            description = description,
            category = category,
            organizationName = organization,
            website = website.ifEmpty { null },
            cost = cost.ifEmpty { "Free" },
            tags = tags.ifEmpty { null },
            type = formatCategory(category),
            priority = 50 // Medium priority for user-submitted content
        )

        // Insert into database
        lifecycleScope.launch {
            try {
                database.opportunityDao().insert(opportunity)

                // Show success message
                Toast.makeText(
                    requireContext(),
                    "Opportunity submitted successfully!",
                    Toast.LENGTH_LONG
                ).show()

                // Clear form
                clearForm()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error submitting opportunity. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun clearForm() {
        titleInput.text?.clear()
        organizationInput.text?.clear()
        descriptionInput.text?.clear()
        websiteInput.text?.clear()
        costInput.text?.clear()
        tagsInput.text?.clear()
        categorySpinner.setSelection(0)
        titleInput.requestFocus()
    }

    private fun formatCategory(category: OpportunityCategory): String {
        return category.name.replace('_', ' ').lowercase()
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
