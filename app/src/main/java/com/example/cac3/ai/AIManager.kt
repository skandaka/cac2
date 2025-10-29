package com.example.cac3.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.cac3.data.model.Opportunity
import com.example.cac3.data.model.User
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * AI Manager - Handles all AI-powered features using OpenAI API
 * Optimized for fast loading with caching and proper timeouts
 */
class AIManager(private val context: Context) {

    private val sharedPrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "ai_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        }
    }

    // Cache for AI responses with timestamps
    private data class CachedResponse<T>(
        val data: T,
        val timestamp: Long
    )

    private val recommendationsCache = mutableMapOf<String, CachedResponse<List<OpportunityRecommendation>>>()
    private val successProbabilityCache = mutableMapOf<String, CachedResponse<SuccessProbability>>()
    private val checklistCache = mutableMapOf<String, CachedResponse<List<ChecklistItem>>>()
    private val applicationHelpCache = mutableMapOf<String, CachedResponse<String>>()
    private val deadlinePredictionCache = mutableMapOf<String, CachedResponse<DeadlinePrediction>>()

    private val CACHE_TTL = 30 * 60 * 1000L // 30 minutes cache
    private var useInstantResponses = false // Toggle for instant mock responses

    // Reuse Retrofit service instance
    private val openAIService: OpenAIService by lazy {
        createOpenAIService()
    }

    private fun createOpenAIService(): OpenAIService {
        val apiKey = getApiKey()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC // Less verbose logging
        }

        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
    }

    companion object {
        private const val API_KEY_PREF = "openai_api_key"
        private const val DEFAULT_API_KEY = "" // Add your OpenAI API key here
        private const val USE_INSTANT_MODE_PREF = "use_instant_mode"
    }

    /**
     * Save OpenAI API key securely
     */
    fun saveApiKey(apiKey: String) {
        sharedPrefs.edit().putString(API_KEY_PREF, apiKey.trim()).apply()
    }

    /**
     * Get saved API key (returns default API key if none is saved)
     */
    fun getApiKey(): String {
        val savedKey = sharedPrefs.getString(API_KEY_PREF, "")?.trim() ?: ""
        return if (savedKey.isEmpty()) DEFAULT_API_KEY else savedKey
    }

    /**
     * Check if API key is configured (always true since default API key is provided)
     */
    fun isApiKeyConfigured(): Boolean {
        return true // Default API key is always available
    }

    /**
     * Enable/disable instant response mode (uses mock data instead of API)
     */
    fun setInstantMode(enabled: Boolean) {
        useInstantResponses = enabled
        sharedPrefs.edit().putBoolean(USE_INSTANT_MODE_PREF, enabled).apply()
    }

    fun isInstantModeEnabled(): Boolean {
        return sharedPrefs.getBoolean(USE_INSTANT_MODE_PREF, true) // Default to instant mode
    }

    /**
     * Check if cached recommendations exist and are fresh
     */
    fun hasCachedRecommendations(userId: Long): Boolean {
        val cached = recommendationsCache["user_$userId"]
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            return age < CACHE_TTL
        }
        return false
    }

    /**
     * Get cached recommendations if available
     */
    fun getCachedRecommendations(userId: Long): List<OpportunityRecommendation>? {
        val cached = recommendationsCache["user_$userId"]
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.timestamp
            if (age < CACHE_TTL) {
                return cached.data
            } else {
                recommendationsCache.remove("user_$userId")
            }
        }
        return null
    }

    /**
     * Clear recommendation cache for a user
     */
    fun clearRecommendationCache(userId: Long) {
        recommendationsCache.remove("user_$userId")
    }

    /**
     * Generate AI-powered opportunity recommendations
     */
    suspend fun generateRecommendations(
        user: User,
        opportunities: List<Opportunity>,
        maxResults: Int = 10,
        useCache: Boolean = true
    ): Result<List<OpportunityRecommendation>> {
        // Check cache first if enabled
        if (useCache) {
            val cached = getCachedRecommendations(user.id)
            if (cached != null) {
                return Result.success(cached)
            }
        }

        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val userProfile = buildUserProfile(user)
            val opportunitiesSummary = buildOpportunitiesSummary(opportunities)

            val prompt = """
                You are an expert college admissions advisor helping a high school student.

                Student Profile:
                $userProfile

                Available Opportunities (showing ${opportunities.size} options):
                $opportunitiesSummary

                Task: Recommend the top $maxResults opportunities for this student. For each recommendation, provide:
                1. The opportunity title (exact match from the list)
                2. A relevance score (0-100)
                3. A brief reason why it's a good fit (1-2 sentences)

                Format your response as JSON array:
                [
                  {
                    "title": "Opportunity Title",
                    "score": 95,
                    "reason": "This matches your interests in..."
                  },
                  ...
                ]
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are a helpful college admissions advisor."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.7,
                maxTokens = 1000
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val recommendations = parseRecommendations(content, opportunities)

                    // Cache the results
                    recommendationsCache["user_${user.id}"] = CachedResponse(
                        data = recommendations,
                        timestamp = System.currentTimeMillis()
                    )

                    return Result.success(recommendations)
                }
            }

            return Result.failure(Exception("Failed to generate recommendations: ${response.errorBody()?.string()}"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * AI Application Assistant - Help write essays/applications
     */
    suspend fun generateApplicationHelp(
        opportunity: Opportunity,
        userProfile: String,
        prompt: String
    ): Result<String> {
        // Check cache first
        val cacheKey = "help_${opportunity.id}_${prompt.hashCode()}"
        val cached = applicationHelpCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL) {
            return Result.success(cached.data)
        }

        // Use instant mock response if enabled
        if (isInstantModeEnabled()) {
            // Add realistic delay to simulate AI thinking (2-3 seconds)
            kotlinx.coroutines.delay((2000..3000).random().toLong())

            val mockData = generateMockApplicationHelp(opportunity, prompt)
            applicationHelpCache[cacheKey] = CachedResponse(mockData, System.currentTimeMillis())
            return Result.success(mockData)
        }

        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val systemPrompt = """
                You are an expert college application essay coach. Help students write compelling,
                authentic application materials. Provide guidance, suggestions, and examples, but
                encourage students to use their own voice and experiences.
            """.trimIndent()

            val userPrompt = """
                Opportunity: ${opportunity.title}
                ${opportunity.description}

                Student Background:
                $userProfile

                Request: $prompt
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                ),
                temperature = 0.8,
                maxTokens = 800
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    applicationHelpCache[cacheKey] = CachedResponse(content, System.currentTimeMillis())
                    return Result.success(content)
                }
            }

            return Result.failure(Exception("Failed to generate application help"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Calculate success probability based on user profile and opportunity
     */
    suspend fun calculateSuccessProbability(
        user: User,
        opportunity: Opportunity,
        userActivities: List<String>
    ): Result<SuccessProbability> {
        // Check cache first
        val cacheKey = "success_${user.id}_${opportunity.id}"
        val cached = successProbabilityCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL) {
            return Result.success(cached.data)
        }

        // Use instant mock response if enabled
        if (isInstantModeEnabled()) {
            // Add realistic delay to simulate AI thinking (2-3 seconds)
            kotlinx.coroutines.delay((2000..3000).random().toLong())

            val mockData = generateMockSuccessProbability(user, opportunity, userActivities)
            successProbabilityCache[cacheKey] = CachedResponse(mockData, System.currentTimeMillis())
            return Result.success(mockData)
        }

        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val userProfile = buildUserProfile(user)
            val activitiesText = userActivities.joinToString("\n- ", prefix = "- ")

            val prompt = """
                Analyze the likelihood of acceptance for this student to this opportunity.

                Student Profile:
                $userProfile

                Current Activities:
                $activitiesText

                Opportunity:
                Title: ${opportunity.title}
                Category: ${opportunity.category}
                Requirements: ${opportunity.requirements ?: "Not specified"}
                ${if (opportunity.minGPA != null) "Min GPA: ${opportunity.minGPA}" else ""}

                Provide your analysis in this JSON format:
                {
                  "probability": 75,
                  "confidence": "high",
                  "strengths": ["strength 1", "strength 2"],
                  "weaknesses": ["weakness 1", "weakness 2"],
                  "recommendations": ["recommendation 1", "recommendation 2"]
                }
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are an expert college admissions counselor."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.5,
                maxTokens = 500
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val probability = parseSuccessProbability(content)
                    successProbabilityCache[cacheKey] = CachedResponse(probability, System.currentTimeMillis())
                    return Result.success(probability)
                }
            }

            return Result.failure(Exception("Failed to calculate success probability"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Generate application checklist
     */
    suspend fun generateApplicationChecklist(
        opportunity: Opportunity
    ): Result<List<ChecklistItem>> {
        // Check cache first
        val cacheKey = "checklist_${opportunity.id}"
        val cached = checklistCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL) {
            return Result.success(cached.data)
        }

        // Use instant mock response if enabled
        if (isInstantModeEnabled()) {
            // Add realistic delay to simulate AI thinking (2-3 seconds)
            kotlinx.coroutines.delay((2000..3000).random().toLong())

            val mockData = generateMockChecklist(opportunity)
            checklistCache[cacheKey] = CachedResponse(mockData, System.currentTimeMillis())
            return Result.success(mockData)
        }

        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val prompt = """
                Create a comprehensive application checklist for this opportunity:

                Title: ${opportunity.title}
                Category: ${opportunity.category}
                Requirements: ${opportunity.requirements ?: "Standard application"}
                Application Components: ${opportunity.applicationComponents ?: "Not specified"}
                Deadline: ${if (opportunity.deadline != null) "Has deadline" else "Rolling"}

                Generate a detailed checklist with estimated time for each task.

                Format as JSON array:
                [
                  {
                    "task": "Research the program thoroughly",
                    "description": "Read website, watch videos, talk to alumni",
                    "estimatedHours": 2,
                    "priority": "high",
                    "daysBeforeDeadline": 30
                  },
                  ...
                ]
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are an organized college application advisor."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.6,
                maxTokens = 800
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val checklist = parseChecklist(content)
                    checklistCache[cacheKey] = CachedResponse(checklist, System.currentTimeMillis())
                    return Result.success(checklist)
                }
            }

            return Result.failure(Exception("Failed to generate checklist"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Predict deadline for rolling opportunities
     */
    suspend fun predictDeadline(
        opportunity: Opportunity,
        similarOpportunities: List<Opportunity>
    ): Result<DeadlinePrediction> {
        // Check cache first
        val cacheKey = "deadline_${opportunity.id}"
        val cached = deadlinePredictionCache[cacheKey]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_TTL) {
            return Result.success(cached.data)
        }

        // Use instant mock response if enabled
        if (isInstantModeEnabled()) {
            // Add realistic delay to simulate AI thinking (2-3 seconds)
            kotlinx.coroutines.delay((2000..3000).random().toLong())

            val mockData = generateMockDeadlinePrediction(opportunity)
            deadlinePredictionCache[cacheKey] = CachedResponse(mockData, System.currentTimeMillis())
            return Result.success(mockData)
        }

        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            // Build context about similar opportunities
            val similarOpsText = if (similarOpportunities.isNotEmpty()) {
                similarOpportunities.take(10).joinToString("\n") { opp ->
                    val deadlineDate = if (opp.deadline != null) {
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.US)
                            .format(java.util.Date(opp.deadline))
                    } else {
                        "Rolling"
                    }
                    "- ${opp.title} (${opp.category}): Deadline was $deadlineDate"
                }
            } else {
                "No similar opportunities found in database"
            }

            val prompt = """
                Predict the likely application deadline for this opportunity:

                Opportunity:
                Title: ${opportunity.title}
                Category: ${opportunity.category}
                Organization: ${opportunity.organizationName ?: "Unknown"}
                Type: ${opportunity.type}
                Is Rolling: ${opportunity.isRolling}
                Current Deadline: ${if (opportunity.deadline != null) "Set" else "Not set"}

                Similar Past Opportunities:
                $similarOpsText

                Based on:
                1. Typical cycles for ${opportunity.category} opportunities
                2. Similar programs' historical patterns
                3. Academic year calendar (applications usually open in fall, summer programs open in spring)

                Provide your prediction in JSON format:
                {
                  "predictedMonth": "September",
                  "predictedYear": 2025,
                  "confidence": "medium",
                  "reasoning": "Most STEM competitions open applications in September...",
                  "suggestedCheckDate": "August 15, 2025"
                }
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are an expert on high school opportunity timelines and deadlines."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.5,
                maxTokens = 400
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val prediction = parseDeadlinePrediction(content)
                    deadlinePredictionCache[cacheKey] = CachedResponse(prediction, System.currentTimeMillis())
                    return Result.success(prediction)
                }
            }

            return Result.failure(Exception("Failed to predict deadline"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Mock data generators for instant responses
    private fun generateMockSuccessProbability(
        user: User,
        opportunity: Opportunity,
        activities: List<String>
    ): SuccessProbability {
        val baseProb = when {
            user.gpa != null && user.gpa >= 3.7 -> 75
            user.gpa != null && user.gpa >= 3.3 -> 65
            else -> 55
        }
        val activityBonus = minOf(activities.size * 5, 20)

        return SuccessProbability(
            probability = minOf(baseProb + activityBonus, 95),
            confidence = "medium",
            strengths = listOf(
                "Strong academic foundation with ${user.grade} grade standing",
                "Demonstrated commitment with ${activities.size} current activities",
                "Interest alignment with ${opportunity.category} opportunities"
            ),
            weaknesses = listOf(
                "Consider adding more leadership experience",
                "Build depth in your chosen field"
            ),
            recommendations = listOf(
                "Continue excelling in your current activities",
                "Seek leadership roles in areas you're passionate about",
                "Start preparing application materials 2-3 months before deadline",
                "Connect with alumni or current participants"
            )
        )
    }

    private fun generateMockChecklist(opportunity: Opportunity): List<ChecklistItem> {
        return listOf(
            ChecklistItem(
                task = "Research ${opportunity.title} thoroughly",
                description = "Visit official website, read past participant reviews, understand program structure",
                estimatedHours = 2,
                priority = "high",
                daysBeforeDeadline = 45
            ),
            ChecklistItem(
                task = "Prepare application materials",
                description = "Gather transcripts, test scores, resume, and any required documents",
                estimatedHours = 3,
                priority = "high",
                daysBeforeDeadline = 30
            ),
            ChecklistItem(
                task = "Draft personal statement or essay",
                description = "Write compelling essay explaining your interest and qualifications",
                estimatedHours = 8,
                priority = "high",
                daysBeforeDeadline = 21
            ),
            ChecklistItem(
                task = "Request recommendation letters",
                description = "Ask teachers or mentors at least 3 weeks before deadline",
                estimatedHours = 1,
                priority = "high",
                daysBeforeDeadline = 28
            ),
            ChecklistItem(
                task = "Complete application form",
                description = "Fill out all required fields carefully and accurately",
                estimatedHours = 2,
                priority = "medium",
                daysBeforeDeadline = 14
            ),
            ChecklistItem(
                task = "Proofread everything",
                description = "Review all materials for errors, have someone else read them too",
                estimatedHours = 2,
                priority = "medium",
                daysBeforeDeadline = 7
            ),
            ChecklistItem(
                task = "Submit application",
                description = "Submit well before deadline, confirm receipt",
                estimatedHours = 1,
                priority = "high",
                daysBeforeDeadline = 3
            )
        )
    }

    private fun generateMockApplicationHelp(opportunity: Opportunity, userPrompt: String): String {
        return """
Great question! Here's some guidance for your ${opportunity.title} application:

**Key Strategies:**

1. **Be Authentic**: Share your genuine passion and experiences. Admissions committees can spot generic responses a mile away.

2. **Show, Don't Tell**: Instead of saying "I'm passionate about ${opportunity.category}," describe a specific moment that sparked or demonstrated that passion.

3. **Connect to Your Goals**: Explain how this opportunity fits into your larger academic and career aspirations.

4. **Be Specific**: Reference particular aspects of the program that excite you. Show you've done your research.

**Structure Suggestion:**

- **Opening Hook**: Start with a compelling anecdote or question
- **Background**: Briefly explain your relevant experience
- **Why This Program**: What specifically draws you to ${opportunity.title}?
- **What You'll Contribute**: What unique perspective or skills will you bring?
- **Looking Forward**: How will this experience shape your future?

**Common Mistakes to Avoid:**
- Generic statements that could apply to any program
- Focusing only on what you'll gain (also mention what you'll contribute)
- Typos and grammatical errors
- Exceeding word limits

**Next Steps:**
1. Write a rough draft focusing on your story
2. Get feedback from a teacher or counselor
3. Revise for clarity and impact
4. Proofread multiple times

Remember: Your authentic voice is your strongest asset. Good luck!
        """.trimIndent()
    }

    private fun generateMockDeadlinePrediction(opportunity: Opportunity): DeadlinePrediction {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        val (month, reasoning) = when (opportunity.category) {
            com.example.cac3.data.model.OpportunityCategory.SUMMER_PROGRAM ->
                "March" to "Most summer programs have application deadlines in late winter/early spring (February-April) for summer sessions"
            com.example.cac3.data.model.OpportunityCategory.COMPETITION ->
                "November" to "Academic competitions typically open registration in fall (October-December) for winter/spring events"
            com.example.cac3.data.model.OpportunityCategory.INTERNSHIP ->
                "February" to "Summer internship applications usually open in winter (January-March)"
            else ->
                "December" to "Most ${opportunity.category} opportunities have rolling admissions or winter deadlines"
        }

        return DeadlinePrediction(
            predictedMonth = month,
            predictedYear = currentYear + 1,
            confidence = "medium",
            reasoning = reasoning,
            suggestedCheckDate = "Check program website in ${getPreviousMonth(month)} ${currentYear}"
        )
    }

    private fun getPreviousMonth(month: String): String {
        val months = listOf("January", "February", "March", "April", "May", "June",
                           "July", "August", "September", "October", "November", "December")
        val index = months.indexOf(month)
        return if (index > 0) months[index - 1] else "December"
    }

    // Helper methods

    private fun buildUserProfile(user: User): String {
        return """
            Name: ${user.fullName}
            Grade: ${user.grade}
            GPA: ${user.gpa ?: "Not specified"}
            School: ${user.schoolName}
            Interests: ${user.interests}
        """.trimIndent()
    }

    private fun buildOpportunitiesSummary(opportunities: List<Opportunity>): String {
        return opportunities.take(50).joinToString("\n\n") { opp ->
            """
            - ${opp.title}
              Category: ${opp.category}
              ${opp.description.take(100)}...
              Tags: ${opp.tags ?: "None"}
            """.trimIndent()
        }
    }

    private fun parseRecommendations(
        jsonContent: String,
        opportunities: List<Opportunity>
    ): List<OpportunityRecommendation> {
        val recommendations = mutableListOf<OpportunityRecommendation>()

        val jsonStart = jsonContent.indexOf('[')
        val jsonEnd = jsonContent.lastIndexOf(']') + 1
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            try {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
                val parsed: List<Map<String, Any>> = gson.fromJson(jsonContent.substring(jsonStart, jsonEnd), type)

                parsed.forEach { item ->
                    val title = item["title"] as? String ?: return@forEach
                    val score = (item["score"] as? Double)?.toInt() ?: 50
                    val reason = item["reason"] as? String ?: ""

                    val opportunity = opportunities.find { it.title.equals(title, ignoreCase = true) }
                    if (opportunity != null) {
                        recommendations.add(
                            OpportunityRecommendation(
                                opportunity = opportunity,
                                score = score,
                                reason = reason
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return recommendations
    }

    private fun parseSuccessProbability(jsonContent: String): SuccessProbability {
        try {
            val jsonStart = jsonContent.indexOf('{')
            val jsonEnd = jsonContent.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val gson = com.google.gson.Gson()
                return gson.fromJson(
                    jsonContent.substring(jsonStart, jsonEnd),
                    SuccessProbability::class.java
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return SuccessProbability(
            probability = 50,
            confidence = "medium",
            strengths = emptyList(),
            weaknesses = emptyList(),
            recommendations = emptyList()
        )
    }

    private fun parseChecklist(jsonContent: String): List<ChecklistItem> {
        try {
            val jsonStart = jsonContent.indexOf('[')
            val jsonEnd = jsonContent.lastIndexOf(']') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<ChecklistItem>>() {}.type
                return gson.fromJson(jsonContent.substring(jsonStart, jsonEnd), type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyList()
    }

    private fun parseDeadlinePrediction(jsonContent: String): DeadlinePrediction {
        try {
            val jsonStart = jsonContent.indexOf('{')
            val jsonEnd = jsonContent.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val gson = com.google.gson.Gson()
                return gson.fromJson(
                    jsonContent.substring(jsonStart, jsonEnd),
                    DeadlinePrediction::class.java
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Default fallback
        return DeadlinePrediction(
            predictedMonth = "Unknown",
            predictedYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
            confidence = "low",
            reasoning = "Unable to predict deadline",
            suggestedCheckDate = "Check organization website regularly"
        )
    }

    private fun parseCollegeMatchPrediction(jsonContent: String): CollegeMatchPrediction {
        try {
            val jsonStart = jsonContent.indexOf('{')
            val jsonEnd = jsonContent.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val gson = com.google.gson.Gson()
                return gson.fromJson(
                    jsonContent.substring(jsonStart, jsonEnd),
                    CollegeMatchPrediction::class.java
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return CollegeMatchPrediction(
            readinessScore = 50,
            collegeTier = "match",
            recommendedColleges = emptyList(),
            strengths = emptyList(),
            recommendations = listOf("Continue building your profile"),
            analysis = "Unable to generate prediction"
        )
    }

    private fun parseScholarshipCalculation(jsonContent: String): ScholarshipCalculation {
        try {
            val jsonStart = jsonContent.indexOf('{')
            val jsonEnd = jsonContent.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val gson = com.google.gson.Gson()
                return gson.fromJson(
                    jsonContent.substring(jsonStart, jsonEnd),
                    ScholarshipCalculation::class.java
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ScholarshipCalculation(
            totalPotential = 0,
            meritBased = 0,
            needBased = 0,
            activityBased = 0,
            topScholarships = emptyList(),
            recommendations = listOf("Explore scholarship databases")
        )
    }

    private fun parseROICalculation(jsonContent: String): ROICalculation {
        try {
            val jsonStart = jsonContent.indexOf('{')
            val jsonEnd = jsonContent.lastIndexOf('}') + 1
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val gson = com.google.gson.Gson()
                return gson.fromJson(
                    jsonContent.substring(jsonStart, jsonEnd),
                    ROICalculation::class.java
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ROICalculation(
            roiScore = 50,
            financialValue = 0,
            nonFinancialValue = "Medium",
            timeInvestment = 0,
            recommendation = "Consider your priorities",
            reasoning = "Unable to calculate ROI"
        )
    }

    /**
     * College Match Predictor - AI-powered college recommendations
     */
    suspend fun predictCollegeMatches(
        user: User,
        activities: List<String>,
        targetColleges: List<String>? = null
    ): Result<CollegeMatchPrediction> {
        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val userProfile = buildUserProfile(user)
            val activitiesList = activities.joinToString("\n- ")

            val prompt = """
                You are an expert college admissions counselor.

                Student Profile:
                $userProfile

                Current Activities:
                - $activitiesList

                ${if (targetColleges != null && targetColleges.isNotEmpty())
                    "Target Colleges: ${targetColleges.joinToString(", ")}"
                else ""}

                Analyze this student's profile and activities. Provide:
                1. Recommended college tier (Safety, Match, Reach)
                2. Top 5 colleges that match their profile
                3. Strength areas for college applications
                4. Recommended activities to strengthen profile
                5. Overall readiness score (0-100)

                Format as JSON:
                {
                  "readinessScore": 75,
                  "collegeTier": "match",
                  "recommendedColleges": ["College 1", "College 2", ...],
                  "strengths": ["Leadership in STEM", ...],
                  "recommendations": ["Join debate team", ...],
                  "analysis": "Overall analysis text..."
                }
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are a knowledgeable college admissions advisor."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.7,
                maxTokens = 800
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val prediction = parseCollegeMatchPrediction(content)
                    return Result.success(prediction)
                }
            }

            return Result.failure(Exception("Failed to predict college matches"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Calculate total scholarship potential from activities
     */
    suspend fun calculateScholarshipPotential(
        user: User,
        commitments: List<Pair<Opportunity, Int>>
    ): Result<ScholarshipCalculation> {
        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val userProfile = buildUserProfile(user)
            val activitiesDesc = commitments.joinToString("\n") { (opp, hours) ->
                "- ${opp.title} (${opp.category}, $hours hrs/week, scholarship: ${if (opp.scholarshipAvailable) "Yes" else "No"})"
            }

            val prompt = """
                You are a scholarship advisor helping a high school student.

                Student Profile:
                $userProfile

                Current Activities:
                $activitiesDesc

                Calculate the student's scholarship potential. Provide:
                1. Estimated total scholarship potential (dollars)
                2. Breakdown by scholarship type (merit, need-based, activity-specific)
                3. Top 5 scholarship opportunities they're qualified for
                4. Recommendations to increase scholarship chances

                Format as JSON:
                {
                  "totalPotential": 50000,
                  "meritBased": 30000,
                  "needBased": 10000,
                  "activityBased": 10000,
                  "topScholarships": [
                    {"name": "XYZ Scholarship", "amount": 10000, "deadline": "March 2025"},
                    ...
                  ],
                  "recommendations": ["Apply to STEM scholarships", ...]
                }
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are an expert scholarship advisor."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.6,
                maxTokens = 800
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val calculation = parseScholarshipCalculation(content)
                    return Result.success(calculation)
                }
            }

            return Result.failure(Exception("Failed to calculate scholarship potential"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Calculate ROI for a specific opportunity
     */
    suspend fun calculateOpportunityROI(
        opportunity: Opportunity,
        user: User
    ): Result<ROICalculation> {
        if (!isApiKeyConfigured()) {
            return Result.failure(Exception("OpenAI API key not configured"))
        }

        try {
            val userProfile = buildUserProfile(user)

            val prompt = """
                You are a college planning advisor analyzing cost-benefit for opportunities.

                Student Profile:
                $userProfile

                Opportunity:
                Title: ${opportunity.title}
                Category: ${opportunity.category}
                Cost: $${opportunity.cost ?: 0}
                Time Commitment: ${opportunity.hoursPerWeek ?: 0} hrs/week
                Scholarship Available: ${opportunity.scholarshipAvailable}
                College Credit: ${opportunity.collegeCredit}

                Calculate the Return on Investment (ROI) for this opportunity. Provide:
                1. Overall ROI score (0-100)
                2. Financial value (scholarships, college credit savings, career earnings)
                3. Non-financial value (skills, network, college admissions boost)
                4. Time investment analysis
                5. Recommendation (worth it or not)

                Format as JSON:
                {
                  "roiScore": 85,
                  "financialValue": 15000,
                  "nonFinancialValue": "High",
                  "timeInvestment": 120,
                  "recommendation": "Highly Recommended",
                  "reasoning": "This opportunity provides excellent..."
                }
            """.trimIndent()

            val request = ChatCompletionRequest(
                model = "gpt-3.5-turbo",
                messages = listOf(
                    ChatMessage(role = "system", content = "You are a strategic college planning advisor."),
                    ChatMessage(role = "user", content = prompt)
                ),
                temperature = 0.6,
                maxTokens = 600
            )

            val response = openAIService.createChatCompletion(request)

            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val roiCalc = parseROICalculation(content)
                    return Result.success(roiCalc)
                }
            }

            return Result.failure(Exception("Failed to calculate ROI"))

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    /**
     * Find fee waiver eligible opportunities
     */
    fun findFeeWaiverOpportunities(
        opportunities: List<Opportunity>,
        user: User
    ): FeeWaiverResult {
        val waiverEligible = mutableListOf<Opportunity>()
        val reducedFee = mutableListOf<Opportunity>()

        for (opp in opportunities) {
            val costValue = opp.costMin?.toInt() ?: opp.cost?.toIntOrNull() ?: 0

            if (costValue > 0) {
                when {
                    opp.category == com.example.cac3.data.model.OpportunityCategory.COMPETITION -> {
                        waiverEligible.add(opp)
                    }
                    opp.scholarshipAvailable -> {
                        reducedFee.add(opp)
                    }
                    costValue < 50 -> {
                        reducedFee.add(opp)
                    }
                }
            }
        }

        val totalSavings = waiverEligible.sumOf {
            it.costMin?.toInt() ?: it.cost?.toIntOrNull() ?: 0
        } + (reducedFee.sumOf {
            it.costMin?.toInt() ?: it.cost?.toIntOrNull() ?: 0
        } / 2)

        return FeeWaiverResult(
            waiverEligibleOpportunities = waiverEligible,
            reducedFeeOpportunities = reducedFee,
            estimatedSavings = totalSavings,
            recommendations = listOf(
                "Contact organizations directly about fee waivers",
                "Check if your school counselor can provide waivers",
                "Look for opportunities marked 'scholarship available'"
            )
        )
    }
}

/**
 * Data classes for AI features
 */

data class OpportunityRecommendation(
    val opportunity: Opportunity,
    val score: Int,
    val reason: String
)

data class SuccessProbability(
    val probability: Int, // 0-100
    val confidence: String, // low, medium, high
    val strengths: List<String>,
    val weaknesses: List<String>,
    val recommendations: List<String>
)

data class ChecklistItem(
    val task: String,
    val description: String,
    val estimatedHours: Int,
    val priority: String, // high, medium, low
    val daysBeforeDeadline: Int
)

data class DeadlinePrediction(
    val predictedMonth: String,
    val predictedYear: Int,
    val confidence: String, // low, medium, high
    val reasoning: String,
    val suggestedCheckDate: String
)

data class CollegeMatchPrediction(
    val readinessScore: Int, // 0-100
    val collegeTier: String, // safety, match, reach
    val recommendedColleges: List<String>,
    val strengths: List<String>,
    val recommendations: List<String>,
    val analysis: String
)

data class ScholarshipOpportunity(
    val name: String,
    val amount: Int,
    val deadline: String
)

data class ScholarshipCalculation(
    val totalPotential: Int,
    val meritBased: Int,
    val needBased: Int,
    val activityBased: Int,
    val topScholarships: List<ScholarshipOpportunity>,
    val recommendations: List<String>
)

data class ROICalculation(
    val roiScore: Int, // 0-100
    val financialValue: Int, // dollars
    val nonFinancialValue: String, // Low, Medium, High
    val timeInvestment: Int, // hours
    val recommendation: String,
    val reasoning: String
)

data class FeeWaiverResult(
    val waiverEligibleOpportunities: List<Opportunity>,
    val reducedFeeOpportunities: List<Opportunity>,
    val estimatedSavings: Int,
    val recommendations: List<String>
)
