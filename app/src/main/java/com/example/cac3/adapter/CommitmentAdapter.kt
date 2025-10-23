package com.example.cac3.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.data.model.CommitmentStatus
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory
import com.example.cac3.data.model.UserCommitment

/**
 * Data class combining commitment and opportunity information for display
 */
data class CommitmentWithOpportunity(
    val commitment: UserCommitment,
    val opportunity: Opportunity
)

/**
 * Adapter for displaying user commitments in a RecyclerView
 */
class CommitmentAdapter(
    private val onItemClick: (CommitmentWithOpportunity) -> Unit
) : ListAdapter<CommitmentWithOpportunity, CommitmentAdapter.CommitmentViewHolder>(CommitmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommitmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_commitment, parent, false)
        return CommitmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommitmentViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, onItemClick)
    }

    class CommitmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.commitmentTitleTextView)
        private val organizationTextView: TextView = itemView.findViewById(R.id.organizationTextView)
        private val statusBadge: TextView = itemView.findViewById(R.id.statusBadge)
        private val hoursTextView: TextView = itemView.findViewById(R.id.hoursTextView)
        private val categoryChip: TextView = itemView.findViewById(R.id.categoryChip)

        fun bind(item: CommitmentWithOpportunity, onItemClick: (CommitmentWithOpportunity) -> Unit) {
            val commitment = item.commitment
            val opportunity = item.opportunity

            titleTextView.text = opportunity.title
            organizationTextView.text = opportunity.organizationName ?: opportunity.type

            // Hours per week
            hoursTextView.text = "${commitment.hoursPerWeek} hrs/week"

            // Status badge
            statusBadge.text = formatStatus(commitment.status)
            statusBadge.setBackgroundColor(getStatusColor(commitment.status))

            // Category
            categoryChip.text = formatCategory(opportunity.category)

            // Click listener
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun formatStatus(status: CommitmentStatus): String {
            return when (status) {
                CommitmentStatus.INTERESTED -> "Interested"
                CommitmentStatus.APPLIED -> "Applied"
                CommitmentStatus.ACCEPTED -> "Accepted"
                CommitmentStatus.PARTICIPATING -> "Active"
                CommitmentStatus.COMPLETED -> "Completed"
            }
        }

        private fun getStatusColor(status: CommitmentStatus): Int {
            return when (status) {
                CommitmentStatus.INTERESTED -> itemView.context.getColor(R.color.status_interested)
                CommitmentStatus.APPLIED -> itemView.context.getColor(R.color.status_applied)
                CommitmentStatus.ACCEPTED -> itemView.context.getColor(R.color.status_accepted)
                CommitmentStatus.PARTICIPATING -> itemView.context.getColor(R.color.status_participating)
                CommitmentStatus.COMPLETED -> itemView.context.getColor(R.color.status_completed)
            }
        }

        private fun formatCategory(category: OpportunityCategory): String {
            return category.name.replace('_', ' ').lowercase()
                .split(' ')
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
    }

    class CommitmentDiffCallback : DiffUtil.ItemCallback<CommitmentWithOpportunity>() {
        override fun areItemsTheSame(
            oldItem: CommitmentWithOpportunity,
            newItem: CommitmentWithOpportunity
        ): Boolean {
            return oldItem.commitment.id == newItem.commitment.id
        }

        override fun areContentsTheSame(
            oldItem: CommitmentWithOpportunity,
            newItem: CommitmentWithOpportunity
        ): Boolean {
            return oldItem == newItem
        }
    }
}
