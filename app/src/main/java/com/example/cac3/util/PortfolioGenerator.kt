package com.example.cac3.util

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Typeface
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.OpportunityCategory
import com.example.cac3.data.model.User
import com.example.cac3.data.model.UserCommitment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Portfolio Generator - Creates professional activity resumes for college applications
 */
class PortfolioGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595  // A4 width in points (8.27 inches)
        private const val PAGE_HEIGHT = 842 // A4 height in points (11.69 inches)
        private const val MARGIN = 50
        private const val LINE_HEIGHT = 20
    }

    data class ActivityEntry(
        val commitment: UserCommitment,
        val opportunity: Opportunity
    )

    /**
     * Generate PDF portfolio
     */
    fun generatePortfolio(
        user: User,
        activities: List<ActivityEntry>
    ): Result<File> {
        return try {
            val document = PdfDocument()
            var currentPage = 1
            var yPosition = MARGIN

            // Create first page
            var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas

            // Title Paint
            val titlePaint = Paint().apply {
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }

            // Header Paint
            val headerPaint = Paint().apply {
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.BLACK
            }

            // Body Paint
            val bodyPaint = Paint().apply {
                textSize = 12f
                color = android.graphics.Color.BLACK
            }

            // Small Paint
            val smallPaint = Paint().apply {
                textSize = 10f
                color = android.graphics.Color.GRAY
            }

            // Document Title
            canvas.drawText("Activities Resume", MARGIN.toFloat(), yPosition.toFloat(), titlePaint)
            yPosition += LINE_HEIGHT * 2

            // User Info
            canvas.drawText(user.fullName, MARGIN.toFloat(), yPosition.toFloat(), headerPaint)
            yPosition += LINE_HEIGHT

            canvas.drawText("Grade ${user.grade} • ${user.schoolName}", MARGIN.toFloat(), yPosition.toFloat(), bodyPaint)
            yPosition += LINE_HEIGHT

            if (user.gpa != null) {
                canvas.drawText("GPA: ${"%.2f".format(user.gpa)}", MARGIN.toFloat(), yPosition.toFloat(), bodyPaint)
                yPosition += LINE_HEIGHT
            }

            canvas.drawText("Email: ${user.email}", MARGIN.toFloat(), yPosition.toFloat(), bodyPaint)
            yPosition += LINE_HEIGHT * 2

            // Statistics
            val totalActivities = activities.size
            val totalHours = activities.sumOf { it.commitment.hoursPerWeek }

            canvas.drawText("Summary", MARGIN.toFloat(), yPosition.toFloat(), headerPaint)
            yPosition += LINE_HEIGHT

            canvas.drawText("Total Activities: $totalActivities", MARGIN.toFloat(), yPosition.toFloat(), bodyPaint)
            yPosition += LINE_HEIGHT

            canvas.drawText("Total Hours per Week: $totalHours", MARGIN.toFloat(), yPosition.toFloat(), bodyPaint)
            yPosition += LINE_HEIGHT * 2

            // Activities by Category
            val categorized = activities.groupBy { it.opportunity.category }

            for ((category, categoryActivities) in categorized) {
                // Check if we need a new page
                if (yPosition > PAGE_HEIGHT - MARGIN - 100) {
                    document.finishPage(page)
                    currentPage++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    yPosition = MARGIN
                }

                // Category Header
                canvas.drawText(formatCategory(category), MARGIN.toFloat(), yPosition.toFloat(), headerPaint)
                yPosition += LINE_HEIGHT + 5

                for (entry in categoryActivities) {
                    // Check if we need a new page
                    if (yPosition > PAGE_HEIGHT - MARGIN - 80) {
                        document.finishPage(page)
                        currentPage++
                        pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPage).create()
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = MARGIN
                    }

                    val opportunity = entry.opportunity
                    val commitment = entry.commitment

                    // Activity Title
                    canvas.drawText("• ${opportunity.title}", MARGIN + 20f, yPosition.toFloat(), bodyPaint)
                    yPosition += LINE_HEIGHT

                    // Organization
                    if (opportunity.organizationName != null) {
                        canvas.drawText("  ${opportunity.organizationName}", MARGIN + 30f, yPosition.toFloat(), smallPaint)
                        yPosition += LINE_HEIGHT - 5
                    }

                    // Time Commitment
                    val hoursText = "${commitment.hoursPerWeek} hrs/week"
                    val datesText = if (commitment.startDate != null && commitment.endDate != null) {
                        val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)
                        " • ${dateFormat.format(Date(commitment.startDate))} - ${dateFormat.format(Date(commitment.endDate))}"
                    } else ""

                    canvas.drawText("  $hoursText$datesText", MARGIN + 30f, yPosition.toFloat(), smallPaint)
                    yPosition += LINE_HEIGHT - 5

                    // Description (first 100 chars)
                    val description = opportunity.description.take(100) + if (opportunity.description.length > 100) "..." else ""
                    val descLines = wrapText(description, 60)
                    for (line in descLines.take(2)) {
                        canvas.drawText("  $line", MARGIN + 30f, yPosition.toFloat(), smallPaint)
                        yPosition += LINE_HEIGHT - 5
                    }

                    // Status
                    canvas.drawText("  Status: ${formatStatus(commitment.status)}", MARGIN + 30f, yPosition.toFloat(), smallPaint)
                    yPosition += LINE_HEIGHT + 5
                }

                yPosition += LINE_HEIGHT
            }

            // Footer on last page
            yPosition = PAGE_HEIGHT - MARGIN
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            canvas.drawText("Generated on ${dateFormat.format(Date())}", MARGIN.toFloat(), yPosition.toFloat(), smallPaint)

            document.finishPage(page)

            // Save to file
            val fileName = "Activities_Resume_${user.fullName.replace(" ", "_")}.pdf"
            val file = File(context.getExternalFilesDir(null), fileName)

            FileOutputStream(file).use { outputStream ->
                document.writeTo(outputStream)
            }

            document.close()

            Result.success(file)

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Generate text-based portfolio for sharing
     */
    fun generateTextPortfolio(
        user: User,
        activities: List<ActivityEntry>
    ): String {
        val builder = StringBuilder()

        // Header
        builder.appendLine("=".repeat(60))
        builder.appendLine("ACTIVITIES RESUME")
        builder.appendLine("=".repeat(60))
        builder.appendLine()

        // User Info
        builder.appendLine(user.fullName)
        builder.appendLine("Grade ${user.grade} • ${user.schoolName}")
        if (user.gpa != null) {
            builder.appendLine("GPA: ${"%.2f".format(user.gpa)}")
        }
        builder.appendLine("Email: ${user.email}")
        builder.appendLine()

        // Summary
        builder.appendLine("SUMMARY")
        builder.appendLine("-".repeat(60))
        builder.appendLine("Total Activities: ${activities.size}")
        builder.appendLine("Total Hours per Week: ${activities.sumOf { it.commitment.hoursPerWeek }}")
        builder.appendLine()

        // Activities by Category
        val categorized = activities.groupBy { it.opportunity.category }

        for ((category, categoryActivities) in categorized) {
            builder.appendLine()
            builder.appendLine(formatCategory(category).uppercase())
            builder.appendLine("-".repeat(60))

            for (entry in categoryActivities) {
                val opportunity = entry.opportunity
                val commitment = entry.commitment

                builder.appendLine()
                builder.appendLine("• ${opportunity.title}")

                if (opportunity.organizationName != null) {
                    builder.appendLine("  Organization: ${opportunity.organizationName}")
                }

                builder.appendLine("  Time Commitment: ${commitment.hoursPerWeek} hrs/week")

                if (commitment.startDate != null && commitment.endDate != null) {
                    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.US)
                    builder.appendLine("  Duration: ${dateFormat.format(Date(commitment.startDate))} - ${dateFormat.format(Date(commitment.endDate))}")
                }

                builder.appendLine("  Status: ${formatStatus(commitment.status)}")

                val description = opportunity.description.take(200)
                builder.appendLine("  Description: $description${if (opportunity.description.length > 200) "..." else ""}")
            }
        }

        // Footer
        builder.appendLine()
        builder.appendLine("=".repeat(60))
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
        builder.appendLine("Generated on ${dateFormat.format(Date())}")

        return builder.toString()
    }

    private fun formatCategory(category: OpportunityCategory): String {
        return category.name.replace('_', ' ')
            .split(' ')
            .joinToString(" ") { it.lowercase().replaceFirstChar { char -> char.uppercase() } }
    }

    private fun formatStatus(status: com.example.cac3.data.model.CommitmentStatus): String {
        return when (status) {
            com.example.cac3.data.model.CommitmentStatus.INTERESTED -> "Interested"
            com.example.cac3.data.model.CommitmentStatus.APPLIED -> "Applied"
            com.example.cac3.data.model.CommitmentStatus.ACCEPTED -> "Accepted"
            com.example.cac3.data.model.CommitmentStatus.PARTICIPATING -> "Active"
            com.example.cac3.data.model.CommitmentStatus.COMPLETED -> "Completed"
        }
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if ((currentLine + word).length <= maxChars) {
                currentLine += "$word "
            } else {
                lines.add(currentLine.trim())
                currentLine = "$word "
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.trim())
        }

        return lines
    }
}
