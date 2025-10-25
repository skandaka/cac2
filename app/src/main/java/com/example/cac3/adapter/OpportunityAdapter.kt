package com.example.cac3.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cac3.R
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying opportunities in a RecyclerView
 */
class OpportunityAdapter(
    private val onItemClick: (Opportunity) -> Unit
) : ListAdapter<Opportunity, OpportunityAdapter.OpportunityViewHolder>(OpportunityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpportunityViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_opportunity, parent, false)
        return OpportunityViewHolder(view)
    }

    override fun onBindViewHolder(holder: OpportunityViewHolder, position: Int) {
        val opportunity = getItem(position)
        holder.bind(opportunity, onItemClick)
    }

    class OpportunityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val organizationTextView: TextView = itemView.findViewById(R.id.organizationTextView)
        private val categoryTextView: TextView = itemView.findViewById(R.id.categoryTextView)
        private val descriptionTextView: TextView = itemView.findViewById(R.id.descriptionTextView)
        private val deadlineTextView: TextView = itemView.findViewById(R.id.deadlineTextView)
        private val costTextView: TextView = itemView.findViewById(R.id.costTextView)
        private val featuredBadge: TextView? = itemView.findViewById(R.id.featuredBadge)
        private val hiddenGemBadge: TextView? = itemView.findViewById(R.id.hiddenGemBadge)

        fun bind(opportunity: Opportunity, onItemClick: (Opportunity) -> Unit) {
            titleTextView.text = opportunity.title
            organizationTextView.text = opportunity.organizationName ?: opportunity.type
            categoryTextView.text = formatCategory(opportunity.category)
            descriptionTextView.text = opportunity.description

            // Set category color
            categoryTextView.setBackgroundColor(getCategoryColor(opportunity.category))

            // Deadline
            if (opportunity.deadline != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                deadlineTextView.text = "Deadline: ${dateFormat.format(Date(opportunity.deadline))}"
                deadlineTextView.visibility = View.VISIBLE
            } else {
                deadlineTextView.visibility = View.GONE
            }

            // Cost
            if (opportunity.cost != null) {
                costTextView.text = opportunity.cost
                if (opportunity.costMin == 0.0) {
                    costTextView.setTextColor(itemView.context.getColor(R.color.free))
                } else {
                    costTextView.setTextColor(itemView.context.getColor(R.color.text_secondary))
                }
                costTextView.visibility = View.VISIBLE
            } else {
                costTextView.visibility = View.GONE
            }

            // Click listener
            cardView.setOnClickListener {
                onItemClick(opportunity)
            }
        }

        private fun formatCategory(category: OpportunityCategory): String {
            return category.name.replace('_', ' ').lowercase()
                .split(' ')
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }

        private fun getCategoryColor(category: OpportunityCategory): Int {
            return when (category) {
                OpportunityCategory.COMPETITION -> Color.parseColor("#FF6B6B")
                OpportunityCategory.EMPLOYMENT -> Color.parseColor("#4ECDC4")
                OpportunityCategory.VOLUNTEERING -> Color.parseColor("#95E1D3")
                OpportunityCategory.COLLEGE -> Color.parseColor("#F38181")
                OpportunityCategory.CLUB -> Color.parseColor("#AA96DA")
                OpportunityCategory.HONOR_SOCIETY -> Color.parseColor("#FCBAD3")
                OpportunityCategory.SUMMER_PROGRAM -> Color.parseColor("#FFFFD2")
                OpportunityCategory.TEST_PREP -> Color.parseColor("#A8D8EA")
                OpportunityCategory.INTERNSHIP -> Color.parseColor("#FFD93D")
                else -> Color.parseColor("#E0E0E0")
            }
        }
    }

    class OpportunityDiffCallback : DiffUtil.ItemCallback<Opportunity>() {
        override fun areItemsTheSame(oldItem: Opportunity, newItem: Opportunity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Opportunity, newItem: Opportunity): Boolean {
            return oldItem == newItem
        }
    }
}
