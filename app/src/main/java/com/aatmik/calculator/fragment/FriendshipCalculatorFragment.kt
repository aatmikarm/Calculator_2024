package com.aatmik.calculator.fragment

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentFriendshipCalculatorBinding
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

class FriendshipCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentFriendshipCalculatorBinding
    private var currentScore = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFriendshipCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupClickListeners()
        setupTextWatchers()
        updateCalculateButtonState()
        updateShareButtonVisibility() // Hide share button initially
    }

    private fun setupClickListeners() {
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnCalculate.setOnClickListener {
            calculateFriendship()
        }

        binding.btnClear.setOnClickListener {
            clearFields()
        }

        binding.btnSwapNames.setOnClickListener {
            swapNames()
        }

        binding.shareBt.setOnClickListener {
            captureAndShareScreenshot()
        }

        // Toggle insights visibility
        binding.root.findViewById<Button>(R.id.btnToggleInsights)?.setOnClickListener {
            toggleInsightsVisibility()
        }
    }

    private fun toggleInsightsVisibility() {
        val friendshipTipsContent = binding.root.findViewById<TextView>(R.id.friendshipTipsContent)
        val toggleButton = binding.root.findViewById<Button>(R.id.btnToggleInsights)

        if (friendshipTipsContent?.maxLines == 10) {
            // Expand
            friendshipTipsContent.maxLines = Int.MAX_VALUE
            toggleButton?.text = "Show Less"
        } else {
            // Collapse
            friendshipTipsContent?.maxLines = 10
            toggleButton?.text = "Show More"
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCalculateButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        binding.etFirstName.addTextChangedListener(textWatcher)
        binding.etSecondName.addTextChangedListener(textWatcher)
    }

    private fun updateShareButtonVisibility() {
        // Hide share button when no results are displayed
        binding.shareBt.visibility = if (binding.resultLayout.visibility == View.VISIBLE) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun updateCalculateButtonState() {
        val firstName = binding.etFirstName.text.toString().trim()
        val secondName = binding.etSecondName.text.toString().trim()

        binding.btnCalculate.isEnabled = firstName.isNotEmpty() && secondName.isNotEmpty()
        binding.btnCalculate.alpha = if (binding.btnCalculate.isEnabled) 1.0f else 0.5f
    }

    private fun captureAndShareScreenshot() {
        // Check if results are visible and have valid dimensions
        if (binding.resultLayout.visibility != View.VISIBLE ||
            binding.friendshipResultCard.width <= 0 ||
            binding.friendshipResultCard.height <= 0) {
            // Show a message or return early if no results to share
            return
        }

        val screenshotView = binding.friendshipResultCard
        val bitmap = Bitmap.createBitmap(
            screenshotView.width,
            screenshotView.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        screenshotView.draw(canvas)

        // Save the bitmap to a file
        val file = File(requireContext().cacheDir, "friendship_result_screenshot.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // Get a content URI for the file using FileProvider
        val contentUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )

        // Create a share intent
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Friendship Result"))
    }

    private fun calculateFriendship() {
        val firstName = binding.etFirstName.text.toString().trim()
        val secondName = binding.etSecondName.text.toString().trim()

        if (firstName.isEmpty() || secondName.isEmpty()) {
            return
        }

        // Hide keyboard
        hideKeyboard()

        // Show result layout and start calculation
        binding.resultLayout.visibility = View.VISIBLE
        binding.friendshipResultCard.visibility = View.VISIBLE

        // Show share button now that results are visible
        updateShareButtonVisibility()

        // Calculate friendship score
        val score = calculateFriendshipScore(firstName, secondName)

        // Animate the score
        animateScore(score)

        // Update result text and colors
        updateResultDisplay(score, firstName, secondName)
    }

    /**
     * The Ultimate Friendship Compatibility Calculator
     * Analyzes multiple factors for friendship potential
     */
    private fun calculateFriendshipScore(name1: String, name2: String): Int {
        val cleanName1 = name1.trim().lowercase()
        val cleanName2 = name2.trim().lowercase()

        var totalScore = 0.0
        var totalWeight = 0.0

        // 1. PERSONALITY COMPATIBILITY (Weight: 30%)
        val personalityScore = calculatePersonalityCompatibility(cleanName1, cleanName2)
        totalScore += personalityScore * 0.30
        totalWeight += 0.30

        // 2. COMMUNICATION STYLE (Weight: 20%)
        val communicationScore = calculateCommunicationCompatibility(cleanName1, cleanName2)
        totalScore += communicationScore * 0.20
        totalWeight += 0.20

        // 3. SHARED INTERESTS POTENTIAL (Weight: 15%)
        val interestsScore = calculateSharedInterests(cleanName1, cleanName2)
        totalScore += interestsScore * 0.15
        totalWeight += 0.15

        // 4. LOYALTY & TRUST FACTORS (Weight: 15%)
        val loyaltyScore = calculateLoyaltyCompatibility(cleanName1, cleanName2)
        totalScore += loyaltyScore * 0.15
        totalWeight += 0.15

        // 5. ENERGY MATCHING (Weight: 10%)
        val energyScore = calculateEnergyCompatibility(cleanName1, cleanName2)
        totalScore += energyScore * 0.10
        totalWeight += 0.10

        // 6. FRIENDSHIP LONGEVITY POTENTIAL (Weight: 10%)
        val longevityScore = calculateLongevityScore(cleanName1, cleanName2)
        totalScore += longevityScore * 0.10
        totalWeight += 0.10

        // Calculate final weighted score
        val finalScore = (totalScore / totalWeight).toInt()

        // Ensure score is between 1-100
        return when {
            finalScore < 1 -> 1
            finalScore > 100 -> 100
            else -> finalScore
        }
    }

    /**
     * PERSONALITY COMPATIBILITY
     * Based on name characteristics and personality traits
     */
    private fun calculatePersonalityCompatibility(name1: String, name2: String): Double {
        fun getPersonalityType(name: String): String {
            val firstLetter = name.firstOrNull() ?: 'a'
            return when (firstLetter) {
                in "aeiou" -> "Extrovert" // Vowels = outgoing
                in "bcdfg" -> "Steady" // Early consonants = reliable
                in "hjklm" -> "Social" // Mid consonants = sociable
                in "npqrs" -> "Thoughtful" // Later consonants = introspective
                else -> "Unique" // Remaining letters = creative
            }
        }

        val type1 = getPersonalityType(name1)
        val type2 = getPersonalityType(name2)

        // Friendship compatibility matrix
        val compatibilityMap = mapOf(
            "Extrovert" to mapOf("Extrovert" to 85, "Steady" to 90, "Social" to 95, "Thoughtful" to 75, "Unique" to 80),
            "Steady" to mapOf("Extrovert" to 90, "Steady" to 88, "Social" to 85, "Thoughtful" to 92, "Unique" to 78),
            "Social" to mapOf("Extrovert" to 95, "Steady" to 85, "Social" to 90, "Thoughtful" to 80, "Unique" to 88),
            "Thoughtful" to mapOf("Extrovert" to 75, "Steady" to 92, "Social" to 80, "Thoughtful" to 85, "Unique" to 90),
            "Unique" to mapOf("Extrovert" to 80, "Steady" to 78, "Social" to 88, "Thoughtful" to 90, "Unique" to 85)
        )

        return compatibilityMap[type1]?.get(type2)?.toDouble() ?: 75.0
    }

    /**
     * COMMUNICATION COMPATIBILITY
     * Analyzes how well they might communicate
     */
    private fun calculateCommunicationCompatibility(name1: String, name2: String): Double {
        // Length similarity (similar communication styles)
        val lengthDiff = abs(name1.length - name2.length)
        val lengthScore = when (lengthDiff) {
            0 -> 95.0
            1 -> 85.0
            2 -> 75.0
            else -> 60.0
        }

        // Vowel-consonant ratio (communication flow)
        val vowels = "aeiou"
        val vowelRatio1 = name1.count { it in vowels }.toDouble() / name1.length
        val vowelRatio2 = name2.count { it in vowels }.toDouble() / name2.length
        val ratioHarmony = 100 - (abs(vowelRatio1 - vowelRatio2) * 150)

        // First letter compatibility (initial connection)
        val firstLetterBonus = if (name1.firstOrNull() == name2.firstOrNull()) 15.0 else 0.0

        return (lengthScore * 0.5 + ratioHarmony * 0.4 + firstLetterBonus * 0.1).coerceIn(0.0, 100.0)
    }

    /**
     * SHARED INTERESTS POTENTIAL
     * Predicts likelihood of common hobbies/interests
     */
    private fun calculateSharedInterests(name1: String, name2: String): Double {
        // Common letters indicate shared interests
        val letters1 = name1.toSet()
        val letters2 = name2.toSet()
        val commonLetters = (letters1 intersect letters2).size
        val totalUniqueLetters = (letters1 union letters2).size

        val commonScore = if (totalUniqueLetters > 0) {
            (commonLetters.toDouble() / totalUniqueLetters) * 100
        } else 50.0

        // Activity letters bonus (letters associated with activities)
        val activityLetters = "sportu" // s-sports, p-photography, o-outdoors, r-reading, t-tech, u-music
        val activityCount = (name1 + name2).count { it in activityLetters }
        val activityBonus = minOf(activityCount * 5, 25).toDouble()

        return (commonScore * 0.7 + activityBonus * 0.3).coerceIn(0.0, 100.0)
    }

    /**
     * LOYALTY & TRUST COMPATIBILITY
     */
    private fun calculateLoyaltyCompatibility(name1: String, name2: String): Double {
        // Trust letters (associated with reliability)
        val trustLetters = "honest" // h-honest, o-open, n-noble, e-ethical, s-sincere, t-trustworthy
        val trustCount1 = name1.count { it in trustLetters }
        val trustCount2 = name2.count { it in trustLetters }
        val trustScore = ((trustCount1 + trustCount2).toDouble() / (name1.length + name2.length)) * 200

        // Name stability (shorter names often indicate straightforward people)
        val stabilityScore = when {
            name1.length <= 4 && name2.length <= 4 -> 90.0
            name1.length <= 6 && name2.length <= 6 -> 80.0
            else -> 70.0
        }

        return (trustScore * 0.6 + stabilityScore * 0.4).coerceIn(0.0, 100.0)
    }

    /**
     * ENERGY COMPATIBILITY
     * Matching energy levels for friendship
     */
    private fun calculateEnergyCompatibility(name1: String, name2: String): Double {
        fun getEnergyLevel(name: String): Int {
            // Count high-energy letters
            val highEnergyLetters = "abcdefghijk"
            return name.count { it in highEnergyLetters }
        }

        val energy1 = getEnergyLevel(name1)
        val energy2 = getEnergyLevel(name2)
        val energyDiff = abs(energy1 - energy2)

        return when (energyDiff) {
            0 -> 95.0
            1 -> 85.0
            2 -> 75.0
            3 -> 65.0
            else -> 50.0
        }
    }

    /**
     * FRIENDSHIP LONGEVITY SCORE
     */
    private fun calculateLongevityScore(name1: String, name2: String): Double {
        // Traditional friendship indicators
        val friendshipLetters = "friend"
        val friendshipCount = (name1 + name2).count { it in friendshipLetters }
        val friendshipBonus = minOf(friendshipCount * 8, 40).toDouble()

        // Name uniqueness (unique combinations often create strong bonds)
        val uniqueLetters = (name1.toSet() union name2.toSet()).size
        val uniquenessScore = minOf(uniqueLetters * 4, 60).toDouble()

        return (friendshipBonus + uniquenessScore) / 2
    }

    private fun animateScore(targetScore: Int) {
        val animator = ValueAnimator.ofInt(0, targetScore)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            currentScore = animation.animatedValue as Int
            binding.tvFriendshipScore.text = "$currentScore%"
            updateProgressBar(currentScore)
        }

        animator.start()
    }

    private fun updateProgressBar(score: Int) {
        binding.progressBar.progress = score

        // Change progress bar color based on score
        val colorRes = when {
            score >= 80 -> R.color.friendship_excellent
            score >= 60 -> R.color.friendship_good
            score >= 40 -> R.color.friendship_average
            else -> R.color.friendship_poor
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), colorRes)
    }

    private fun updateResultDisplay(score: Int, firstName: String, secondName: String) {
        // Set names
        binding.tvNameResult.text = "$firstName 🤝 $secondName"

        // Set friendship message and icon
        val (message, emoji, colorRes) = when {
            score >= 95 -> Triple("Soulmate Friends! Unbreakable bond!", "👑", R.color.friendship_excellent)
            score >= 90 -> Triple("Best Friends Forever! Perfect match!", "🌟", R.color.friendship_excellent)
            score >= 85 -> Triple("Amazing friendship potential! Kindred spirits!", "✨", R.color.friendship_excellent)
            score >= 80 -> Triple("Excellent friends! Strong connection!", "💫", R.color.friendship_excellent)
            score >= 75 -> Triple("Great friendship! Very compatible!", "🎉", R.color.friendship_good)
            score >= 70 -> Triple("Very good friends! Fun together!", "🎈", R.color.friendship_good)
            score >= 65 -> Triple("Good friendship potential! Nice bond!", "🌈", R.color.friendship_good)
            score >= 60 -> Triple("Solid friendship! Work well together!", "🤗", R.color.friendship_good)
            score >= 55 -> Triple("Decent friends! Some shared interests!", "😊", R.color.friendship_average)
            score >= 50 -> Triple("Average friendship. Effort needed!", "🙂", R.color.friendship_average)
            score >= 45 -> Triple("Below average. Different personalities!", "😐", R.color.friendship_average)
            score >= 40 -> Triple("Challenging friendship. Need patience!", "🤔", R.color.friendship_average)
            score >= 30 -> Triple("Difficult match. Very different styles!", "😕", R.color.friendship_poor)
            score >= 20 -> Triple("Poor compatibility. Casual acquaintances!", "😬", R.color.friendship_poor)
            else -> Triple("Better as distant friends. Too different!", "🤷", R.color.friendship_poor)
        }

        binding.tvCompatibilityMessage.text = message
        binding.tvFriendshipEmoji.text = emoji

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.tvFriendshipScore.setTextColor(color)
        binding.tvCompatibilityMessage.setTextColor(color)

        // Generate detailed insights
        generateDetailedInsights(score, firstName, secondName)
    }

    private fun generateDetailedInsights(score: Int, firstName: String, secondName: String) {
        val cleanName1 = firstName.trim().lowercase()
        val cleanName2 = secondName.trim().lowercase()

        val insights = buildFriendshipAnalysis(score, firstName, secondName, cleanName1, cleanName2)
        updateFriendshipTipsWithInsights(insights)
    }

    private fun buildFriendshipAnalysis(
        score: Int, firstName: String, secondName: String,
        cleanName1: String, cleanName2: String
    ): String {

        val personalityType1 = getPersonalityInsight(cleanName1)
        val personalityType2 = getPersonalityInsight(cleanName2)

        val strengthsAndChallenges = getFriendshipDynamics(score)
        val activities = getSuggestedActivities(cleanName1, cleanName2)
        val advice = getFriendshipAdvice(score)

        return """
🌟 Personality Analysis:
• $firstName: $personalityType1
• $secondName: $personalityType2

$strengthsAndChallenges

🎯 Perfect Activities Together:
$activities

💡 Friendship Tips:
$advice

🔮 Friendship Forecast:
${getFriendshipForecast(score)}

Remember: Great friendships are built on understanding, respect, and shared experiences! This analysis is for fun and reflection. 🤝
        """.trimIndent()
    }

    private fun getPersonalityInsight(name: String): String {
        val firstLetter = name.firstOrNull() ?: 'a'
        return when (firstLetter) {
            in "aeiou" -> "Outgoing, energetic, loves social gatherings"
            in "bcdfg" -> "Reliable, steady, great listener and supporter"
            in "hjklm" -> "Social butterfly, connects people, natural networker"
            in "npqrs" -> "Thoughtful, deep thinker, values meaningful conversations"
            else -> "Creative, unique perspective, brings fresh ideas"
        }
    }

    private fun getFriendshipDynamics(score: Int): String = when {
        score >= 85 -> """
💪 Strengths:
• Natural understanding and communication
• Shared values and similar humor
• Supportive during tough times
• Easy to spend time together

⚡ Minor Challenges:
• Might be too similar - need to try new things together
• Could become dependent on each other
        """.trimIndent()

        score >= 70 -> """
💪 Strengths:
• Good communication and mutual respect
• Complement each other's personalities
• Enjoy spending time together
• Trustworthy and reliable

⚡ Areas to Work On:
• Different communication styles need patience
• May need effort to find common interests
        """.trimIndent()

        score >= 55 -> """
💪 Strengths:
• Potential for growth and learning from differences
• Can balance each other out
• Opportunities for new experiences

⚡ Challenges:
• Need more time to understand each other
• Different energy levels require compromise
• Communication needs extra effort
        """.trimIndent()

        else -> """
💪 Potential Strengths:
• Can learn a lot from each other's differences
• Opportunity for personal growth

⚡ Main Challenges:
• Very different personalities and approaches
• Need significant effort to bridge gaps
• Better suited for casual, occasional friendship
        """.trimIndent()
    }

    private fun getSuggestedActivities(name1: String, name2: String): String {
        val combinedName = name1 + name2
        val activities = mutableListOf<String>()

        if (combinedName.contains('s')) activities.add("• Sports and outdoor adventures")
        if (combinedName.contains('a')) activities.add("• Art classes or creative workshops")
        if (combinedName.contains('m')) activities.add("• Music concerts or learning instruments")
        if (combinedName.contains('r')) activities.add("• Reading book clubs or library visits")
        if (combinedName.contains('t')) activities.add("• Tech meetups or gaming sessions")
        if (combinedName.contains('c')) activities.add("• Cooking or trying new restaurants")
        if (combinedName.contains('n')) activities.add("• Nature walks or hiking trips")

        if (activities.isEmpty()) {
            activities.addAll(listOf(
                "• Coffee dates and deep conversations",
                "• Movie nights and series binges",
                "• Volunteer work together"
            ))
        }

        return activities.joinToString("\n")
    }

    private fun getFriendshipAdvice(score: Int): String = when {
        score >= 80 -> """
• Celebrate your natural connection
• Try new activities together to keep growing
• Be supportive during each other's challenges
• Don't take the friendship for granted
        """.trimIndent()

        score >= 60 -> """
• Focus on your common interests
• Be patient with communication differences
• Make regular plans to stay connected
• Practice active listening
        """.trimIndent()

        else -> """
• Start with casual, low-pressure activities
• Focus on finding one shared interest
• Be open to learning from each other
• Don't force the friendship - let it develop naturally
        """.trimIndent()
    }

    private fun getFriendshipForecast(score: Int): String = when {
        score >= 85 -> "This friendship has incredible potential for lifelong connection. You'll likely become each other's go-to support system and create countless amazing memories together."

        score >= 70 -> "A promising friendship that will grow stronger with time. You'll enjoy each other's company and develop mutual trust and respect."

        score >= 55 -> "Moderate potential that depends on effort from both sides. With patience and understanding, you could develop a meaningful friendship."

        else -> "This connection works better as a casual acquaintance. You might enjoy occasional interactions but probably won't become close friends."
    }

    private fun updateFriendshipTipsWithInsights(insights: String) {
        val friendshipTipsContent = binding.root.findViewById<TextView>(R.id.friendshipTipsContent)
        if (friendshipTipsContent != null) {
            friendshipTipsContent.text = insights
        }

        // Show the toggle button after insights are loaded
        binding.root.findViewById<Button>(R.id.btnToggleInsights)?.visibility = View.VISIBLE
    }

    private fun clearFields() {
        binding.etFirstName.text?.clear()
        binding.etSecondName.text?.clear()
        binding.resultLayout.visibility = View.GONE
        binding.friendshipResultCard.visibility = View.GONE
        currentScore = 0

        // Hide share button when results are cleared
        updateShareButtonVisibility()

        // Reset focus to first name field
        binding.etFirstName.requestFocus()
    }

    private fun swapNames() {
        val firstName = binding.etFirstName.text.toString()
        val secondName = binding.etSecondName.text.toString()

        binding.etFirstName.setText(secondName)
        binding.etSecondName.setText(firstName)

        // If result is visible, recalculate
        if (binding.resultLayout.visibility == View.VISIBLE) {
            calculateFriendship()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    companion object {
        private const val TAG = "FriendshipCalculatorFragment"
    }
}