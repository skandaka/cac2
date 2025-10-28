package com.example.cac3.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
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
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Add Opportunity Fragment - Allow users to contribute new opportunities
 */
class AddOpportunityFragment : Fragment() {

    private lateinit var database: AppDatabase

    // Basic Information
    private lateinit var titleInput: TextInputEditText
    private lateinit var organizationInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var categorySpinner: Spinner

    // Contact Information
    private lateinit var contactEmailInput: TextInputEditText
    private lateinit var contactPhoneInput: TextInputEditText
    private lateinit var addressInput: TextInputEditText
    private lateinit var websiteInput: TextInputEditText

    // Deadlines & Dates
    private lateinit var deadlineInput: TextInputEditText
    private lateinit var rollingDeadlineCheckbox: MaterialCheckBox
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText

    // Eligibility
    private lateinit var minGradeInput: TextInputEditText
    private lateinit var maxGradeInput: TextInputEditText
    private lateinit var minGPAInput: TextInputEditText

    // Cost & Financial
    private lateinit var costInput: TextInputEditText
    private lateinit var scholarshipAvailableCheckbox: MaterialCheckBox
    private lateinit var scholarshipAmountInput: TextInputEditText
    private lateinit var scholarshipAmountLayout: View

    // Time Commitment
    private lateinit var hoursPerWeekInput: TextInputEditText
    private lateinit var totalHoursInput: TextInputEditText

    // Requirements & Application
    private lateinit var requirementsInput: TextInputEditText
    private lateinit var applicationComponentsInput: TextInputEditText
    private lateinit var workPermitCheckbox: MaterialCheckBox

    // Transportation & Location
    private lateinit var virtualCheckbox: MaterialCheckBox
    private lateinit var transitAccessibleCheckbox: MaterialCheckBox
    private lateinit var paceRoutesInput: TextInputEditText
    private lateinit var requiresCarCheckbox: MaterialCheckBox

    // Benefits
    private lateinit var benefitsInput: TextInputEditText
    private lateinit var collegeCreditCheckbox: MaterialCheckBox
    private lateinit var serviceHoursInput: TextInputEditText
    private lateinit var graduationCordCheckbox: MaterialCheckBox

    // Club-Specific
    private lateinit var clubSpecificSection: View
    private lateinit var sponsorInput: TextInputEditText
    private lateinit var sponsorEmailInput: TextInputEditText
    private lateinit var meetingDayInput: TextInputEditText
    private lateinit var meetingTimeInput: TextInputEditText
    private lateinit var meetingRoomInput: TextInputEditText
    private lateinit var schoologyCodeInput: TextInputEditText

    // Job-Specific
    private lateinit var jobSpecificSection: View
    private lateinit var wageInput: TextInputEditText

    // Additional
    private lateinit var tagsInput: TextInputEditText
    private lateinit var submitButton: MaterialButton

    // Date storage
    private var deadlineDate: Long? = null
    private var startDate: Long? = null
    private var endDate: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_opportunity_enhanced, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        setupCategorySpinner()
        setupSubmitButton()
        setupDatePickers()
    }

    private fun initializeViews(view: View) {
        // Basic Information
        titleInput = view.findViewById(R.id.titleInput)
        organizationInput = view.findViewById(R.id.organizationInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        categorySpinner = view.findViewById(R.id.categorySpinner)

        // Contact Information
        contactEmailInput = view.findViewById(R.id.contactEmailInput)
        contactPhoneInput = view.findViewById(R.id.contactPhoneInput)
        addressInput = view.findViewById(R.id.addressInput)
        websiteInput = view.findViewById(R.id.websiteInput)

        // Deadlines & Dates
        deadlineInput = view.findViewById(R.id.deadlineInput)
        rollingDeadlineCheckbox = view.findViewById(R.id.rollingDeadlineCheckbox)
        startDateInput = view.findViewById(R.id.startDateInput)
        endDateInput = view.findViewById(R.id.endDateInput)

        // Eligibility
        minGradeInput = view.findViewById(R.id.minGradeInput)
        maxGradeInput = view.findViewById(R.id.maxGradeInput)
        minGPAInput = view.findViewById(R.id.minGPAInput)

        // Cost & Financial
        costInput = view.findViewById(R.id.costInput)
        scholarshipAvailableCheckbox = view.findViewById(R.id.scholarshipAvailableCheckbox)
        scholarshipAmountInput = view.findViewById(R.id.scholarshipAmountInput)
        scholarshipAmountLayout = view.findViewById(R.id.scholarshipAmountLayout)

        // Time Commitment
        hoursPerWeekInput = view.findViewById(R.id.hoursPerWeekInput)
        totalHoursInput = view.findViewById(R.id.totalHoursInput)

        // Requirements & Application
        requirementsInput = view.findViewById(R.id.requirementsInput)
        applicationComponentsInput = view.findViewById(R.id.applicationComponentsInput)
        workPermitCheckbox = view.findViewById(R.id.workPermitCheckbox)

        // Transportation & Location
        virtualCheckbox = view.findViewById(R.id.virtualCheckbox)
        transitAccessibleCheckbox = view.findViewById(R.id.transitAccessibleCheckbox)
        paceRoutesInput = view.findViewById(R.id.paceRoutesInput)
        requiresCarCheckbox = view.findViewById(R.id.requiresCarCheckbox)

        // Benefits
        benefitsInput = view.findViewById(R.id.benefitsInput)
        collegeCreditCheckbox = view.findViewById(R.id.collegeCreditCheckbox)
        serviceHoursInput = view.findViewById(R.id.serviceHoursInput)
        graduationCordCheckbox = view.findViewById(R.id.graduationCordCheckbox)

        // Club-Specific
        clubSpecificSection = view.findViewById(R.id.clubSpecificSection)
        sponsorInput = view.findViewById(R.id.sponsorInput)
        sponsorEmailInput = view.findViewById(R.id.sponsorEmailInput)
        meetingDayInput = view.findViewById(R.id.meetingDayInput)
        meetingTimeInput = view.findViewById(R.id.meetingTimeInput)
        meetingRoomInput = view.findViewById(R.id.meetingRoomInput)
        schoologyCodeInput = view.findViewById(R.id.schoologyCodeInput)

        // Job-Specific
        jobSpecificSection = view.findViewById(R.id.jobSpecificSection)
        wageInput = view.findViewById(R.id.wageInput)

        // Additional
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

        // Show/hide category-specific sections
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val category = OpportunityCategory.values()[position]
                when (category) {
                    OpportunityCategory.CLUB, OpportunityCategory.HONOR_SOCIETY -> {
                        clubSpecificSection.visibility = View.VISIBLE
                        jobSpecificSection.visibility = View.GONE
                    }
                    OpportunityCategory.EMPLOYMENT, OpportunityCategory.INTERNSHIP -> {
                        clubSpecificSection.visibility = View.GONE
                        jobSpecificSection.visibility = View.VISIBLE
                    }
                    else -> {
                        clubSpecificSection.visibility = View.GONE
                        jobSpecificSection.visibility = View.GONE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            submitOpportunity()
        }
    }

    private fun setupDatePickers() {
        deadlineInput.setOnClickListener {
            showDatePicker { date ->
                deadlineDate = date
                deadlineInput.setText(formatDate(date))
            }
        }

        startDateInput.setOnClickListener {
            showDatePicker { date ->
                startDate = date
                startDateInput.setText(formatDate(date))
            }
        }

        endDateInput.setOnClickListener {
            showDatePicker { date ->
                endDate = date
                endDateInput.setText(formatDate(date))
            }
        }

        meetingTimeInput.setOnClickListener {
            showTimePicker { time ->
                meetingTimeInput.setText(time)
            }
        }

        // Show/hide scholarship amount based on checkbox
        scholarshipAvailableCheckbox.setOnCheckedChangeListener { _, isChecked ->
            scholarshipAmountLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun showDatePicker(onDateSet: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                onDateSet(calendar.timeInMillis)
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTimePicker(onTimeSet: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSet(time)
            },
            hour,
            minute,
            false
        ).show()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        return sdf.format(Date(timestamp))
    }

    private fun submitOpportunity() {
        // Basic Information
        val title = titleInput.text.toString().trim()
        val organization = organizationInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()

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

        // Collect all fields
        val contactEmail = contactEmailInput.text.toString().trim().ifEmpty { null }
        val contactPhone = contactPhoneInput.text.toString().trim().ifEmpty { null }
        val address = addressInput.text.toString().trim().ifEmpty { null }
        val website = websiteInput.text.toString().trim().ifEmpty { null }

        val isRolling = rollingDeadlineCheckbox.isChecked

        val minGrade = minGradeInput.text.toString().trim().toIntOrNull()
        val maxGrade = maxGradeInput.text.toString().trim().toIntOrNull()
        val minGPA = minGPAInput.text.toString().trim().toDoubleOrNull()

        val cost = costInput.text.toString().trim().ifEmpty { "Free" }
        val scholarshipAvailable = scholarshipAvailableCheckbox.isChecked
        val scholarshipAmount = if (scholarshipAvailable) {
            scholarshipAmountInput.text.toString().trim().ifEmpty { null }
        } else null

        val hoursPerWeek = hoursPerWeekInput.text.toString().trim().ifEmpty { null }
        val totalHours = totalHoursInput.text.toString().trim().toIntOrNull()

        val requirements = requirementsInput.text.toString().trim().ifEmpty { null }
        val applicationComponents = applicationComponentsInput.text.toString().trim().ifEmpty { null }
        val workPermitRequired = workPermitCheckbox.isChecked

        val isVirtual = virtualCheckbox.isChecked
        val transitAccessible = transitAccessibleCheckbox.isChecked
        val paceRoutes = paceRoutesInput.text.toString().trim().ifEmpty { null }
        val requiresCar = requiresCarCheckbox.isChecked

        val benefits = benefitsInput.text.toString().trim().ifEmpty { null }
        val collegeCredit = collegeCreditCheckbox.isChecked
        val serviceHours = serviceHoursInput.text.toString().trim().isNotEmpty() // Boolean: has service hours or not
        val graduationCord = graduationCordCheckbox.isChecked

        val tags = tagsInput.text.toString().trim().ifEmpty { null }

        // Club-specific fields
        val sponsor = if (clubSpecificSection.visibility == View.VISIBLE) {
            sponsorInput.text.toString().trim().ifEmpty { null }
        } else null
        val sponsorEmail = if (clubSpecificSection.visibility == View.VISIBLE) {
            sponsorEmailInput.text.toString().trim().ifEmpty { null }
        } else null
        val meetingDay = if (clubSpecificSection.visibility == View.VISIBLE) {
            meetingDayInput.text.toString().trim().ifEmpty { null }
        } else null
        val meetingTime = if (clubSpecificSection.visibility == View.VISIBLE) {
            meetingTimeInput.text.toString().trim().ifEmpty { null }
        } else null
        val meetingRoom = if (clubSpecificSection.visibility == View.VISIBLE) {
            meetingRoomInput.text.toString().trim().ifEmpty { null }
        } else null
        val schoologyCode = if (clubSpecificSection.visibility == View.VISIBLE) {
            schoologyCodeInput.text.toString().trim().ifEmpty { null }
        } else null

        // Job-specific fields
        val wage = if (jobSpecificSection.visibility == View.VISIBLE) {
            wageInput.text.toString().trim().ifEmpty { null }
        } else null

        // Create opportunity object with all fields
        val opportunity = Opportunity(
            title = title,
            description = description,
            category = category,
            type = formatCategory(category),
            organizationName = organization,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            address = address,
            website = website,
            deadline = deadlineDate,
            startDate = startDate,
            endDate = endDate,
            isRolling = isRolling,
            minGrade = minGrade,
            maxGrade = maxGrade,
            minGPA = minGPA,
            cost = cost,
            scholarshipAvailable = scholarshipAvailable,
            scholarshipAmount = scholarshipAmount,
            wage = wage,
            hoursPerWeek = hoursPerWeek,
            totalHoursRequired = totalHours,
            requirements = requirements,
            applicationComponents = applicationComponents,
            workPermitRequired = workPermitRequired,
            transitAccessible = transitAccessible,
            paceRoutes = paceRoutes,
            requiresCar = requiresCar,
            isVirtual = isVirtual,
            benefits = benefits,
            collegeCredit = collegeCredit,
            serviceHours = serviceHours,
            graduationCord = graduationCord,
            meetingDay = meetingDay,
            meetingTime = meetingTime,
            meetingRoom = meetingRoom,
            schoologyCode = schoologyCode,
            sponsor = sponsor,
            sponsorEmail = sponsorEmail,
            tags = tags,
            priority = 50 // Medium priority for user-submitted content
        )

        // Insert into database
        lifecycleScope.launch {
            try {
                database.opportunityDao().insert(opportunity)

                // Show success message
                Toast.makeText(
                    requireContext(),
                    "Opportunity added successfully! It's now live and visible to all students.",
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
        // Basic
        titleInput.text?.clear()
        organizationInput.text?.clear()
        descriptionInput.text?.clear()
        categorySpinner.setSelection(0)

        // Contact
        contactEmailInput.text?.clear()
        contactPhoneInput.text?.clear()
        addressInput.text?.clear()
        websiteInput.text?.clear()

        // Deadlines
        deadlineInput.text?.clear()
        rollingDeadlineCheckbox.isChecked = false
        startDateInput.text?.clear()
        endDateInput.text?.clear()
        deadlineDate = null
        startDate = null
        endDate = null

        // Eligibility
        minGradeInput.text?.clear()
        maxGradeInput.text?.clear()
        minGPAInput.text?.clear()

        // Cost
        costInput.text?.clear()
        scholarshipAvailableCheckbox.isChecked = false
        scholarshipAmountInput.text?.clear()

        // Time
        hoursPerWeekInput.text?.clear()
        totalHoursInput.text?.clear()

        // Requirements
        requirementsInput.text?.clear()
        applicationComponentsInput.text?.clear()
        workPermitCheckbox.isChecked = false

        // Transportation
        virtualCheckbox.isChecked = false
        transitAccessibleCheckbox.isChecked = false
        paceRoutesInput.text?.clear()
        requiresCarCheckbox.isChecked = false

        // Benefits
        benefitsInput.text?.clear()
        collegeCreditCheckbox.isChecked = false
        serviceHoursInput.text?.clear()
        graduationCordCheckbox.isChecked = false

        // Club-specific
        sponsorInput.text?.clear()
        sponsorEmailInput.text?.clear()
        meetingDayInput.text?.clear()
        meetingTimeInput.text?.clear()
        meetingRoomInput.text?.clear()
        schoologyCodeInput.text?.clear()

        // Job-specific
        wageInput.text?.clear()

        // Additional
        tagsInput.text?.clear()

        titleInput.requestFocus()
    }

    private fun formatCategory(category: OpportunityCategory): String {
        return category.name.replace('_', ' ').lowercase()
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
