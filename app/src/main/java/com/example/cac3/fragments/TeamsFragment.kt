package com.example.cac3.fragments

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.data.database.AppDatabase
import com.example.cac3.data.model.*
import com.example.cac3.util.AuthManager
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.launch

/**
 * Teams Fragment - Find and create teams for collaborative opportunities
 */
class TeamsFragment : Fragment() {

    private lateinit var authManager: AuthManager
    private lateinit var database: AppDatabase

    private lateinit var myTeamsRecyclerView: RecyclerView
    private lateinit var discoverTeamsRecyclerView: RecyclerView
    private lateinit var noTeamsTextView: TextView
    private lateinit var noOpenTeamsTextView: TextView
    private lateinit var createTeamFab: ExtendedFloatingActionButton

    private lateinit var myTeamsAdapter: TeamAdapter
    private lateinit var discoverTeamsAdapter: TeamAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_teams, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authManager = AuthManager(requireContext())
        database = AppDatabase.getDatabase(requireContext())

        initializeViews(view)
        setupRecyclerViews()
        setupCreateButton()
        loadTeams()
    }

    private fun initializeViews(view: View) {
        myTeamsRecyclerView = view.findViewById(R.id.myTeamsRecyclerView)
        discoverTeamsRecyclerView = view.findViewById(R.id.discoverTeamsRecyclerView)
        noTeamsTextView = view.findViewById(R.id.noTeamsTextView)
        noOpenTeamsTextView = view.findViewById(R.id.noOpenTeamsTextView)
        createTeamFab = view.findViewById(R.id.createTeamFab)
    }

    private fun setupRecyclerViews() {
        // My Teams
        myTeamsAdapter = TeamAdapter(isMyTeam = true) { team ->
            showTeamDetails(team, isMyTeam = true)
        }
        myTeamsRecyclerView.adapter = myTeamsAdapter
        myTeamsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Discover Teams
        discoverTeamsAdapter = TeamAdapter(isMyTeam = false) { team ->
            handleJoinTeam(team)
        }
        discoverTeamsRecyclerView.adapter = discoverTeamsAdapter
        discoverTeamsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupCreateButton() {
        createTeamFab.setOnClickListener {
            showCreateTeamDialog()
        }
    }

    private fun loadTeams() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) return

        lifecycleScope.launch {
            try {
                // Load my teams
                val myTeamMembers = database.teamDao().getUserTeams(userId)
                val myTeams = mutableListOf<TeamWithOpportunity>()

                for (member in myTeamMembers) {
                    if (member.status == TeamMemberStatus.ACCEPTED) {
                        val team = database.teamDao().getTeamById(member.teamId)
                        val opportunity = database.opportunityDao().getOpportunityByIdSync(team?.opportunityId ?: 0)
                        if (team != null && opportunity != null) {
                            myTeams.add(TeamWithOpportunity(team, opportunity))
                        }
                    }
                }

                myTeamsAdapter.submitList(myTeams)
                noTeamsTextView.visibility = if (myTeams.isEmpty()) View.VISIBLE else View.GONE
                myTeamsRecyclerView.visibility = if (myTeams.isEmpty()) View.GONE else View.VISIBLE

                // Load all open teams
                database.teamDao().getAllOpenTeams().observe(viewLifecycleOwner) { teams ->
                    lifecycleScope.launch {
                        val teamsWithOpps = mutableListOf<TeamWithOpportunity>()

                        for (team in teams) {
                            // Don't show teams I'm already in
                            val isMember = myTeamMembers.any { it.teamId == team.id }
                            if (!isMember && team.currentMembers < team.maxMembers) {
                                val opportunity = database.opportunityDao().getOpportunityByIdSync(team.opportunityId)
                                if (opportunity != null) {
                                    teamsWithOpps.add(TeamWithOpportunity(team, opportunity))
                                }
                            }
                        }

                        discoverTeamsAdapter.submitList(teamsWithOpps)
                        noOpenTeamsTextView.visibility = if (teamsWithOpps.isEmpty()) View.VISIBLE else View.GONE
                        discoverTeamsRecyclerView.visibility = if (teamsWithOpps.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error loading teams", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCreateTeamDialog() {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Get user's commitments to choose from
            val commitments = database.userDao().getUserCommitmentsSync(userId)
            if (commitments.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Add some opportunities to your commitments first",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val opportunities = mutableListOf<Opportunity>()
            val opportunityNames = mutableListOf<String>()

            for (commitment in commitments) {
                val opp = database.opportunityDao().getOpportunityByIdSync(commitment.opportunityId)
                if (opp != null) {
                    opportunities.add(opp)
                    opportunityNames.add(opp.title)
                }
            }

            // Show dialog on main thread
            requireActivity().runOnUiThread {
                val dialogView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 30, 50, 30)
                }

                // Opportunity selector
                val oppLabel = TextView(requireContext()).apply {
                    text = "Select Opportunity:"
                    textSize = 16f
                    setPadding(0, 0, 0, 8)
                }
                dialogView.addView(oppLabel)

                val oppSpinner = Spinner(requireContext()).apply {
                    adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, opportunityNames).apply {
                        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    }
                }
                dialogView.addView(oppSpinner)

                // Team name input
                val nameLabel = TextView(requireContext()).apply {
                    text = "Team Name:"
                    textSize = 16f
                    setPadding(0, 20, 0, 8)
                }
                dialogView.addView(nameLabel)

                val nameInput = EditText(requireContext()).apply {
                    hint = "e.g., Code Warriors"
                    inputType = InputType.TYPE_CLASS_TEXT
                }
                dialogView.addView(nameInput)

                // Description input
                val descLabel = TextView(requireContext()).apply {
                    text = "Description:"
                    textSize = 16f
                    setPadding(0, 20, 0, 8)
                }
                dialogView.addView(descLabel)

                val descInput = EditText(requireContext()).apply {
                    hint = "What are you looking for?"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                }
                dialogView.addView(descInput)

                // Max members input
                val membersLabel = TextView(requireContext()).apply {
                    text = "Max Team Size:"
                    textSize = 16f
                    setPadding(0, 20, 0, 8)
                }
                dialogView.addView(membersLabel)

                val membersInput = EditText(requireContext()).apply {
                    hint = "4"
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setText("4")
                }
                dialogView.addView(membersInput)

                // Skills input
                val skillsLabel = TextView(requireContext()).apply {
                    text = "Required Skills (optional):"
                    textSize = 16f
                    setPadding(0, 20, 0, 8)
                }
                dialogView.addView(skillsLabel)

                val skillsInput = EditText(requireContext()).apply {
                    hint = "Coding, Design, Writing"
                    inputType = InputType.TYPE_CLASS_TEXT
                }
                dialogView.addView(skillsInput)

                AlertDialog.Builder(requireContext())
                    .setTitle("Create New Team")
                    .setView(dialogView)
                    .setPositiveButton("Create") { _, _ ->
                        val selectedOpp = opportunities[oppSpinner.selectedItemPosition]
                        val teamName = nameInput.text.toString().trim()
                        val description = descInput.text.toString().trim()
                        val maxMembers = membersInput.text.toString().toIntOrNull() ?: 4
                        val skills = skillsInput.text.toString().trim()

                        if (teamName.isEmpty() || description.isEmpty()) {
                            Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                            return@setPositiveButton
                        }

                        createTeam(selectedOpp.id, teamName, description, maxMembers, skills, userId)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun createTeam(
        opportunityId: Long,
        teamName: String,
        description: String,
        maxMembers: Int,
        skills: String,
        leaderId: Long
    ) {
        lifecycleScope.launch {
            try {
                val team = Team(
                    opportunityId = opportunityId,
                    teamName = teamName,
                    description = description,
                    leaderUserId = leaderId,
                    maxMembers = maxMembers,
                    currentMembers = 1,
                    requiredSkills = if (skills.isNotEmpty()) skills else null
                )

                val teamId = database.teamDao().insertTeam(team)

                // Add creator as first member
                val member = TeamMember(
                    teamId = teamId,
                    userId = leaderId,
                    role = "Team Leader",
                    status = TeamMemberStatus.ACCEPTED
                )
                database.teamDao().insertTeamMember(member)

                Toast.makeText(requireContext(), "Team created successfully!", Toast.LENGTH_SHORT).show()
                loadTeams()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error creating team", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleJoinTeam(teamWithOpp: TeamWithOpportunity) {
        val userId = authManager.getCurrentUserId()
        if (userId == -1L) {
            Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Show dialog to send join request
        val messageInput = EditText(requireContext()).apply {
            hint = "Why do you want to join? (optional)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            setPadding(50, 30, 50, 30)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Join ${teamWithOpp.team.teamName}")
            .setMessage("Send a request to join this team:")
            .setView(messageInput)
            .setPositiveButton("Send Request") { _, _ ->
                val message = messageInput.text.toString().trim()
                sendJoinRequest(teamWithOpp.team.id, userId, message)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendJoinRequest(teamId: Long, userId: Long, message: String) {
        lifecycleScope.launch {
            try {
                // Check if request already exists
                val existing = database.teamDao().getExistingRequest(userId, teamId)
                if (existing != null) {
                    Toast.makeText(requireContext(), "You already sent a request to this team", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val request = TeamRequest(
                    teamId = teamId,
                    userId = userId,
                    message = if (message.isNotEmpty()) message else null
                )

                database.teamDao().insertTeamRequest(request)

                Toast.makeText(
                    requireContext(),
                    "Request sent! The team leader will review it.",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error sending request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTeamDetails(teamWithOpp: TeamWithOpportunity, isMyTeam: Boolean) {
        val userId = authManager.getCurrentUserId()

        lifecycleScope.launch {
            val team = teamWithOpp.team
            val members = database.teamDao().getTeamMembers(team.id)
            val isLeader = team.leaderUserId == userId

            val dialogView = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 30, 50, 30)
            }

            // Team info
            val infoText = TextView(requireContext()).apply {
                text = """
                    ${teamWithOpp.opportunity.title}

                    ${team.description}

                    Members: ${team.currentMembers}/${team.maxMembers}
                    ${if (team.requiredSkills != null) "\nSkills: ${team.requiredSkills}" else ""}
                """.trimIndent()
                textSize = 14f
            }
            dialogView.addView(infoText)

            val builder = AlertDialog.Builder(requireContext())
                .setTitle(team.teamName)
                .setView(dialogView)
                .setPositiveButton("Close", null)

            // If leader, show manage options
            if (isLeader) {
                builder.setNeutralButton("Manage Requests") { _, _ ->
                    showManageRequests(team.id)
                }
            }

            builder.show()
        }
    }

    private fun showManageRequests(teamId: Long) {
        lifecycleScope.launch {
            val requests = database.teamDao().getPendingRequestsForTeam(teamId)

            if (requests.isEmpty()) {
                Toast.makeText(requireContext(), "No pending requests", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val requestNames = mutableListOf<String>()
            for (request in requests) {
                val user = database.userDao().getUserById(request.userId)
                val name = user?.fullName ?: "User ${request.userId}"
                val msg = if (request.message != null) " - ${request.message}" else ""
                requestNames.add("$name$msg")
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Join Requests")
                .setItems(requestNames.toTypedArray()) { _, which ->
                    showRequestDecision(requests[which])
                }
                .setNegativeButton("Close", null)
                .show()
        }
    }

    private fun showRequestDecision(request: TeamRequest) {
        AlertDialog.Builder(requireContext())
            .setTitle("Accept this member?")
            .setPositiveButton("Accept") { _, _ ->
                approveRequest(request)
            }
            .setNegativeButton("Reject") { _, _ ->
                rejectRequest(request)
            }
            .show()
    }

    private fun approveRequest(request: TeamRequest) {
        lifecycleScope.launch {
            try {
                // Update request status
                val updatedRequest = request.copy(
                    status = RequestStatus.APPROVED,
                    respondedAt = System.currentTimeMillis()
                )
                database.teamDao().updateTeamRequest(updatedRequest)

                // Add member to team
                val member = TeamMember(
                    teamId = request.teamId,
                    userId = request.userId,
                    status = TeamMemberStatus.ACCEPTED
                )
                database.teamDao().insertTeamMember(member)

                // Update team member count
                val team = database.teamDao().getTeamById(request.teamId)
                if (team != null) {
                    val updatedTeam = team.copy(
                        currentMembers = team.currentMembers + 1,
                        isOpen = (team.currentMembers + 1) < team.maxMembers
                    )
                    database.teamDao().updateTeam(updatedTeam)
                }

                Toast.makeText(requireContext(), "Member added to team!", Toast.LENGTH_SHORT).show()
                loadTeams()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error approving request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectRequest(request: TeamRequest) {
        lifecycleScope.launch {
            try {
                val updatedRequest = request.copy(
                    status = RequestStatus.REJECTED,
                    respondedAt = System.currentTimeMillis()
                )
                database.teamDao().updateTeamRequest(updatedRequest)

                Toast.makeText(requireContext(), "Request rejected", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error rejecting request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadTeams()
    }
}

/**
 * Data class combining team and opportunity
 */
data class TeamWithOpportunity(
    val team: Team,
    val opportunity: Opportunity
)

/**
 * Adapter for displaying teams
 */
class TeamAdapter(
    private val isMyTeam: Boolean,
    private val onTeamClick: (TeamWithOpportunity) -> Unit
) : RecyclerView.Adapter<TeamAdapter.TeamViewHolder>() {

    private var teams: List<TeamWithOpportunity> = emptyList()

    fun submitList(newTeams: List<TeamWithOpportunity>) {
        teams = newTeams
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_team, parent, false)
        return TeamViewHolder(view)
    }

    override fun onBindViewHolder(holder: TeamViewHolder, position: Int) {
        holder.bind(teams[position], isMyTeam, onTeamClick)
    }

    override fun getItemCount() = teams.size

    class TeamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val teamNameTextView: TextView = itemView.findViewById(R.id.teamNameTextView)
        private val opportunityNameTextView: TextView = itemView.findViewById(R.id.opportunityNameTextView)
        private val teamDescriptionTextView: TextView = itemView.findViewById(R.id.teamDescriptionTextView)
        private val membersTextView: TextView = itemView.findViewById(R.id.membersTextView)
        private val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)
        private val skillsTextView: TextView = itemView.findViewById(R.id.skillsTextView)
        private val actionButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.actionButton)

        fun bind(
            teamWithOpp: TeamWithOpportunity,
            isMyTeam: Boolean,
            onClick: (TeamWithOpportunity) -> Unit
        ) {
            val team = teamWithOpp.team
            val opportunity = teamWithOpp.opportunity

            teamNameTextView.text = team.teamName
            opportunityNameTextView.text = opportunity.title
            teamDescriptionTextView.text = team.description
            membersTextView.text = "${team.currentMembers}/${team.maxMembers} members"

            // Status badge
            statusBadge.text = if (team.isOpen) "OPEN" else "FULL"
            statusBadge.setBackgroundColor(
                if (team.isOpen)
                    itemView.context.getColor(R.color.success)
                else
                    itemView.context.getColor(R.color.error)
            )

            // Skills
            if (team.requiredSkills != null) {
                skillsTextView.visibility = View.VISIBLE
                skillsTextView.text = "Skills: ${team.requiredSkills}"
            } else {
                skillsTextView.visibility = View.GONE
            }

            // Action button
            if (isMyTeam) {
                actionButton.text = "View Details"
            } else {
                actionButton.text = "Request to Join"
            }

            actionButton.setOnClickListener {
                onClick(teamWithOpp)
            }

            itemView.setOnClickListener {
                onClick(teamWithOpp)
            }
        }
    }
}
