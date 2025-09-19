package com.aatmik.calculator.fragment

import android.animation.ValueAnimator
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
import androidx.fragment.app.Fragment
import com.aatmik.calculator.R
import com.aatmik.calculator.databinding.FragmentLoveCalculatorBinding
import kotlin.math.abs

class LoveCalculatorFragment : Fragment() {

    private lateinit var binding: FragmentLoveCalculatorBinding
    private var currentScore = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoveCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
        setupClickListeners()
        setupTextWatchers()
        updateCalculateButtonState()
    }

    private fun setupClickListeners() {
        binding.backIv.setOnClickListener {
            activity?.onBackPressedDispatcher?.onBackPressed()
        }

        binding.btnCalculate.setOnClickListener {
            calculateLove()
        }

        binding.btnClear.setOnClickListener {
            clearFields()
        }

        binding.btnSwapNames.setOnClickListener {
            swapNames()
        }

        // Toggle insights visibility
        binding.root.findViewById<Button>(R.id.btnToggleInsights)?.setOnClickListener {
            toggleInsightsVisibility()
        }
    }

    private fun toggleInsightsVisibility() {
        val loveTipsContent = binding.root.findViewById<TextView>(R.id.loveTipsContent)
        val toggleButton = binding.root.findViewById<Button>(R.id.btnToggleInsights)

        if (loveTipsContent?.maxLines == 10) {
            // Expand
            loveTipsContent.maxLines = Int.MAX_VALUE
            toggleButton?.text = "Show Less"
        } else {
            // Collapse
            loveTipsContent?.maxLines = 10
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

    private fun updateCalculateButtonState() {
        val firstName = binding.etFirstName.text.toString().trim()
        val secondName = binding.etSecondName.text.toString().trim()

        binding.btnCalculate.isEnabled = firstName.isNotEmpty() && secondName.isNotEmpty()
        binding.btnCalculate.alpha = if (binding.btnCalculate.isEnabled) 1.0f else 0.5f
    }

    private fun calculateLove() {
        val firstName = binding.etFirstName.text.toString().trim()
        val secondName = binding.etSecondName.text.toString().trim()

        if (firstName.isEmpty() || secondName.isEmpty()) {
            return
        }

        // Hide keyboard
        hideKeyboard()

        // Show result layout and start calculation
        binding.resultLayout.visibility = View.VISIBLE
        binding.loveResultCard.visibility = View.VISIBLE

        // Calculate love score
        val score = calculateLoveScore(firstName, secondName)

        // Animate the score
        animateScore(score)

        // Update result text and colors
        updateResultDisplay(score, firstName, secondName)
    }

    /**
     * The Ultimate Love Compatibility Calculator
     * Incorporates multiple scientific and traditional approaches for maximum authenticity
     */
    private fun calculateLoveScore(name1: String, name2: String): Int {
        val cleanName1 = name1.trim().lowercase()
        val cleanName2 = name2.trim().lowercase()

        var totalScore = 0.0
        var totalWeight = 0.0

        // 1. NUMEROLOGY ANALYSIS (Weight: 25%)
        val numerologyScore = calculateNumerologyCompatibility(cleanName1, cleanName2)
        totalScore += numerologyScore * 0.25
        totalWeight += 0.25

        // 2. PHONETIC HARMONY (Weight: 15%)
        val phoneticScore = calculatePhoneticHarmony(cleanName1, cleanName2)
        totalScore += phoneticScore * 0.15
        totalWeight += 0.15

        // 3. LETTER FREQUENCY & PATTERNS (Weight: 15%)
        val letterScore = calculateLetterCompatibility(cleanName1, cleanName2)
        totalScore += letterScore * 0.15
        totalWeight += 0.15

        // 4. TRADITIONAL FLAMES METHOD (Weight: 10%)
        val flamesScore = calculateFlamesCompatibility(cleanName1, cleanName2)
        totalScore += flamesScore * 0.10
        totalWeight += 0.10

        // 5. LINGUISTIC PATTERNS (Weight: 10%)
        val linguisticScore = calculateLinguisticCompatibility(cleanName1, cleanName2)
        totalScore += linguisticScore * 0.10
        totalWeight += 0.10

        // 6. MATHEMATICAL HARMONY (Weight: 8%)
        val mathScore = calculateMathematicalHarmony(cleanName1, cleanName2)
        totalScore += mathScore * 0.08
        totalWeight += 0.08

        // 7. CULTURAL & ORIGIN COMPATIBILITY (Weight: 7%)
        val culturalScore = calculateCulturalCompatibility(cleanName1, cleanName2)
        totalScore += culturalScore * 0.07
        totalWeight += 0.07

        // 8. SPECIAL BONUSES & PENALTIES (Weight: 5%)
        val bonusScore = calculateSpecialBonuses(cleanName1, cleanName2)
        totalScore += bonusScore * 0.05
        totalWeight += 0.05

        // 9. PSYCHOLOGICAL FACTORS (Weight: 5%)
        val psychScore = calculatePsychologicalCompatibility(cleanName1, cleanName2)
        totalScore += psychScore * 0.05
        totalWeight += 0.05

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
     * NUMEROLOGY COMPATIBILITY
     * Uses Pythagorean numerology system
     */
    private fun calculateNumerologyCompatibility(name1: String, name2: String): Double {
        fun getNameNumber(name: String): Int {
            val values = mapOf(
                'a' to 1, 'b' to 2, 'c' to 3, 'd' to 4, 'e' to 5, 'f' to 6, 'g' to 7, 'h' to 8, 'i' to 9,
                'j' to 1, 'k' to 2, 'l' to 3, 'm' to 4, 'n' to 5, 'o' to 6, 'p' to 7, 'q' to 8, 'r' to 9,
                's' to 1, 't' to 2, 'u' to 3, 'v' to 4, 'w' to 5, 'x' to 6, 'y' to 7, 'z' to 8
            )

            var sum = name.filter { it.isLetter() }.sumOf { values[it] ?: 0 }

            // Reduce to single digit (except 11, 22, 33 - master numbers)
            while (sum > 9 && sum != 11 && sum != 22 && sum != 33) {
                sum = sum.toString().sumOf { it.digitToInt() }
            }
            return sum
        }

        val num1 = getNameNumber(name1)
        val num2 = getNameNumber(name2)

        // Numerology compatibility matrix
        val compatibilityMatrix = mapOf(
            1 to mapOf(1 to 75, 2 to 85, 3 to 90, 4 to 60, 5 to 95, 6 to 70, 7 to 65, 8 to 80, 9 to 85),
            2 to mapOf(1 to 85, 2 to 80, 3 to 70, 4 to 95, 5 to 60, 6 to 90, 7 to 75, 8 to 85, 9 to 65),
            3 to mapOf(1 to 90, 2 to 70, 3 to 85, 4 to 65, 5 to 80, 6 to 75, 7 to 95, 8 to 60, 9 to 90),
            4 to mapOf(1 to 60, 2 to 95, 3 to 65, 4 to 85, 5 to 70, 6 to 80, 7 to 75, 8 to 90, 9 to 75),
            5 to mapOf(1 to 95, 2 to 60, 3 to 80, 4 to 70, 5 to 85, 6 to 65, 7 to 90, 8 to 75, 9 to 80),
            6 to mapOf(1 to 70, 2 to 90, 3 to 75, 4 to 80, 5 to 65, 6 to 85, 7 to 60, 8 to 95, 9 to 70),
            7 to mapOf(1 to 65, 2 to 75, 3 to 95, 4 to 75, 5 to 90, 6 to 60, 7 to 80, 8 to 70, 9 to 85),
            8 to mapOf(1 to 80, 2 to 85, 3 to 60, 4 to 90, 5 to 75, 6 to 95, 7 to 70, 8 to 85, 9 to 65),
            9 to mapOf(1 to 85, 2 to 65, 3 to 90, 4 to 75, 5 to 80, 6 to 70, 7 to 85, 8 to 65, 9 to 95)
        )

        return compatibilityMatrix[num1]?.get(num2)?.toDouble() ?: 50.0
    }

    /**
     * PHONETIC HARMONY
     * Analyzes sound patterns and rhythm
     */
    private fun calculatePhoneticHarmony(name1: String, name2: String): Double {
        val vowels = "aeiou"

        // Vowel-consonant ratio harmony
        val vowelRatio1 = name1.count { it in vowels }.toDouble() / name1.length
        val vowelRatio2 = name2.count { it in vowels }.toDouble() / name2.length
        val ratioHarmony = 100 - (abs(vowelRatio1 - vowelRatio2) * 200)

        // Syllable count harmony
        val syllables1 = maxOf(1, name1.count { it in vowels })
        val syllables2 = maxOf(1, name2.count { it in vowels })
        val syllableHarmony = when (abs(syllables1 - syllables2)) {
            0 -> 100.0
            1 -> 80.0
            2 -> 60.0
            else -> 40.0
        }

        // Starting sound compatibility
        val firstLetterBonus = if (name1.firstOrNull() == name2.firstOrNull()) 20.0 else 0.0

        return (ratioHarmony * 0.4 + syllableHarmony * 0.4 + firstLetterBonus * 0.2).coerceIn(0.0, 100.0)
    }

    /**
     * LETTER FREQUENCY & PATTERN ANALYSIS
     */
    private fun calculateLetterCompatibility(name1: String, name2: String): Double {
        // Love letters bonus
        val loveLetters = "love"
        val loveCount = (name1 + name2).count { it in loveLetters }
        val loveBonus = minOf(loveCount * 8, 40).toDouble()

        // Common letters analysis
        val letters1 = name1.toSet()
        val letters2 = name2.toSet()
        val commonLetters = (letters1 intersect letters2).size
        val totalUniqueLetters = (letters1 union letters2).size
        val commonLetterScore = if (totalUniqueLetters > 0) {
            (commonLetters.toDouble() / totalUniqueLetters) * 60
        } else 0.0

        // Rare letter bonus (less common letters get higher scores)
        val rareLetters = "qxzjkv"
        val rareBonus = (name1 + name2).count { it in rareLetters } * 5.0

        return (loveBonus + commonLetterScore + rareBonus).coerceIn(0.0, 100.0)
    }

    /**
     * TRADITIONAL FLAMES METHOD
     * Friends, Lovers, Affectionate, Marriage, Enemies, Siblings
     */
    private fun calculateFlamesCompatibility(name1: String, name2: String): Double {
        var str1 = name1.replace(" ", "")
        var str2 = name2.replace(" ", "")

        // Remove common characters
        val commonChars = str1.toSet() intersect str2.toSet()
        commonChars.forEach { char ->
            val count1 = str1.count { it == char }
            val count2 = str2.count { it == char }
            val commonCount = minOf(count1, count2)
            repeat(commonCount) {
                str1 = str1.replaceFirst(char.toString(), "")
                str2 = str2.replaceFirst(char.toString(), "")
            }
        }

        val remainingCount = str1.length + str2.length
        val flames = "FLAMES"
        val result = flames[remainingCount % 6]

        return when (result) {
            'F' -> 45.0  // Friends
            'L' -> 95.0  // Lovers
            'A' -> 85.0  // Affectionate
            'M' -> 98.0  // Marriage
            'E' -> 15.0  // Enemies
            'S' -> 60.0  // Siblings
            else -> 50.0
        }
    }

    /**
     * LINGUISTIC PATTERN COMPATIBILITY
     */
    private fun calculateLinguisticCompatibility(name1: String, name2: String): Double {
        // Length harmony
        val lengthDiff = abs(name1.length - name2.length)
        val lengthScore = when (lengthDiff) {
            0 -> 100.0
            1 -> 90.0
            2 -> 75.0
            3 -> 60.0
            else -> 40.0
        }

        // Alphabetical position analysis
        fun getAlphabeticalValue(name: String): Double {
            return name.sumOf { (it - 'a' + 1).toDouble() } / name.length
        }

        val alpha1 = getAlphabeticalValue(name1)
        val alpha2 = getAlphabeticalValue(name2)
        val alphaHarmony = 100 - abs(alpha1 - alpha2) * 3

        return (lengthScore * 0.6 + alphaHarmony * 0.4).coerceIn(0.0, 100.0)
    }

    /**
     * MATHEMATICAL HARMONY
     * Golden ratio, Fibonacci, and geometric patterns
     */
    private fun calculateMathematicalHarmony(name1: String, name2: String): Double {
        val goldenRatio = 1.618

        // Golden ratio in name lengths
        val ratio = maxOf(name1.length, name2.length).toDouble() / minOf(name1.length, name2.length)
        val goldenScore = 100 - abs(ratio - goldenRatio) * 30

        // ASCII value harmony
        val ascii1 = name1.sumOf { it.code }
        val ascii2 = name2.sumOf { it.code }
        val asciiHarmony = 100 - (abs(ascii1 - ascii2) % 100)

        // Fibonacci check
        val fibNumbers = listOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89)
        val fibBonus = if (fibNumbers.contains(name1.length) || fibNumbers.contains(name2.length)) 15.0 else 0.0

        return (goldenScore * 0.5 + asciiHarmony * 0.4 + fibBonus * 0.1).coerceIn(0.0, 100.0)
    }

    /**
     * CULTURAL & ORIGIN COMPATIBILITY
     */
    private fun calculateCulturalCompatibility(name1: String, name2: String): Double {
        // Common name patterns and origins
        val patterns = mapOf(
            "an" to "Germanic", "er" to "Germanic", "son" to "Nordic",
            "ez" to "Spanish", "ini" to "Italian", "ski" to "Polish",
            "sen" to "Nordic", "ova" to "Slavic", "ian" to "Armenian"
        )

        fun getNameOrigin(name: String): String {
            return patterns.entries.find { name.endsWith(it.key) }?.value ?: "Universal"
        }

        val origin1 = getNameOrigin(name1)
        val origin2 = getNameOrigin(name2)

        return when {
            origin1 == origin2 && origin1 != "Universal" -> 85.0
            origin1 == "Universal" || origin2 == "Universal" -> 70.0
            else -> 60.0
        }
    }

    /**
     * SPECIAL BONUSES & EASTER EGGS
     */
    private fun calculateSpecialBonuses(name1: String, name2: String): Double {
        var bonusScore = 50.0

        // Same first letter bonus
        if (name1.firstOrNull() == name2.firstOrNull()) bonusScore += 15

        // Palindrome bonus
        if (name1 == name1.reversed() || name2 == name2.reversed()) bonusScore += 20

        // Rhyming bonus (same ending)
        if (name1.length > 2 && name2.length > 2) {
            val ending1 = name1.takeLast(2)
            val ending2 = name2.takeLast(2)
            if (ending1 == ending2) bonusScore += 10
        }

        // Celebrity couple patterns
        val famousCouples = listOf(
            "romeo" to "juliet", "bonnie" to "clyde", "adam" to "eve",
            "john" to "jane", "jack" to "jill", "beauty" to "beast"
        )

        val lowerName1 = name1.lowercase()
        val lowerName2 = name2.lowercase()

        famousCouples.forEach { (first, second) ->
            if ((lowerName1.contains(first) && lowerName2.contains(second)) ||
                (lowerName1.contains(second) && lowerName2.contains(first))) {
                bonusScore += 25
            }
        }

        return bonusScore.coerceIn(0.0, 100.0)
    }

    /**
     * PSYCHOLOGICAL COMPATIBILITY
     * Based on name psychology research
     */
    private fun calculatePsychologicalCompatibility(name1: String, name2: String): Double {
        // Personality traits based on first letter
        val traits = mapOf(
            'a' to "ambitious", 'b' to "balanced", 'c' to "creative", 'd' to "determined",
            'e' to "empathetic", 'f' to "friendly", 'g' to "generous", 'h' to "honest",
            'i' to "intuitive", 'j' to "joyful", 'k' to "kind", 'l' to "loyal",
            'm' to "motivated", 'n' to "nurturing", 'o' to "optimistic", 'p' to "passionate",
            'q' to "quirky", 'r' to "reliable", 's' to "sensitive", 't' to "trustworthy",
            'u' to "understanding", 'v' to "vibrant", 'w' to "wise", 'x' to "exciting",
            'y' to "youthful", 'z' to "zealous"
        )

        val trait1 = traits[name1.firstOrNull()]
        val trait2 = traits[name2.firstOrNull()]

        // Complementary traits get higher scores
        val complementaryPairs = mapOf(
            "ambitious" to "supportive", "creative" to "practical", "sensitive" to "protective",
            "energetic" to "calm", "outgoing" to "thoughtful", "adventurous" to "stable"
        )

        val isComplementary = complementaryPairs.any { (key, value) ->
            (trait1 == key && trait2 == value) || (trait1 == value && trait2 == key)
        }

        return if (isComplementary) 85.0 else 65.0
    }

    private fun animateScore(targetScore: Int) {
        val animator = ValueAnimator.ofInt(0, targetScore)
        animator.duration = 2000
        animator.interpolator = AccelerateDecelerateInterpolator()

        animator.addUpdateListener { animation ->
            currentScore = animation.animatedValue as Int
            binding.tvLoveScore.text = "$currentScore%"
            updateProgressBar(currentScore)
        }

        animator.start()
    }

    private fun updateProgressBar(score: Int) {
        binding.progressBar.progress = score

        // Change progress bar color based on score
        val colorRes = when {
            score >= 80 -> R.color.love_excellent
            score >= 60 -> R.color.love_good
            score >= 40 -> R.color.love_average
            else -> R.color.love_poor
        }

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.progressBar.progressTintList = ContextCompat.getColorStateList(requireContext(), colorRes)
    }

    private fun updateResultDisplay(score: Int, firstName: String, secondName: String) {
        // Set names
        binding.tvNameResult.text = "$firstName ❤️ $secondName"

        // Set compatibility message and icon with more detailed ranges
        val (message, emoji, colorRes) = when {
            score >= 95 -> Triple("Cosmic Soulmates! Written in the stars!", "💫", R.color.love_excellent)
            score >= 90 -> Triple("Perfect Match! You're destined!", "💕", R.color.love_excellent)
            score >= 85 -> Triple("Exceptional compatibility! True love!", "❤️", R.color.love_excellent)
            score >= 80 -> Triple("Excellent match! Strong bond!", "💖", R.color.love_excellent)
            score >= 75 -> Triple("Very high compatibility! Great potential!", "💗", R.color.love_good)
            score >= 70 -> Triple("Very good match! Strong connection!", "💝", R.color.love_good)
            score >= 65 -> Triple("Good compatibility! Promising future!", "💘", R.color.love_good)
            score >= 60 -> Triple("Decent match! Work together well!", "💚", R.color.love_good)
            score >= 55 -> Triple("Average plus! Room to grow!", "💛", R.color.love_average)
            score >= 50 -> Triple("Average match. Effort needed!", "🧡", R.color.love_average)
            score >= 45 -> Triple("Below average. Challenges ahead!", "💙", R.color.love_average)
            score >= 40 -> Triple("Low compatibility. Need work!", "💜", R.color.love_average)
            score >= 30 -> Triple("Difficult match. Major effort required!", "🤍", R.color.love_poor)
            score >= 20 -> Triple("Very challenging. Consider friendship!", "🖤", R.color.love_poor)
            else -> Triple("Better as friends. Different paths!", "💔", R.color.love_poor)
        }

        binding.tvCompatibilityMessage.text = message
        binding.tvLoveEmoji.text = emoji

        val color = ContextCompat.getColor(requireContext(), colorRes)
        binding.tvLoveScore.setTextColor(color)
        binding.tvCompatibilityMessage.setTextColor(color)

        // Generate and display detailed insights
        generateDetailedInsights(score, firstName, secondName)
    }

    /**
     * Generate comprehensive relationship insights based on all algorithm factors
     */
    private fun generateDetailedInsights(score: Int, firstName: String, secondName: String) {
        val cleanName1 = firstName.trim().lowercase()
        val cleanName2 = secondName.trim().lowercase()

        // Recalculate individual scores for detailed analysis
        val numerologyScore = calculateNumerologyCompatibility(cleanName1, cleanName2)
        val phoneticScore = calculatePhoneticHarmony(cleanName1, cleanName2)
        val letterScore = calculateLetterCompatibility(cleanName1, cleanName2)
        val flamesScore = calculateFlamesCompatibility(cleanName1, cleanName2)
        val linguisticScore = calculateLinguisticCompatibility(cleanName1, cleanName2)
        val mathScore = calculateMathematicalHarmony(cleanName1, cleanName2)
        val culturalScore = calculateCulturalCompatibility(cleanName1, cleanName2)
        val bonusScore = calculateSpecialBonuses(cleanName1, cleanName2)
        val psychScore = calculatePsychologicalCompatibility(cleanName1, cleanName2)

        // Generate personality insights
        val personality1 = getPersonalityInsights(cleanName1)
        val personality2 = getPersonalityInsights(cleanName2)

        // Create comprehensive analysis
        val insights = buildDetailedAnalysis(
            score, firstName, secondName,
            numerologyScore, phoneticScore, letterScore, flamesScore,
            linguisticScore, mathScore, culturalScore, bonusScore, psychScore,
            personality1, personality2
        )

        // Update the love tips section with detailed insights
        updateLoveTipsWithInsights(insights)
    }

    /**
     * Get personality insights based on name analysis
     */
    private fun getPersonalityInsights(name: String): PersonalityProfile {
        val firstLetter = name.firstOrNull() ?: 'a'
        val nameLength = name.length
        val vowelCount = name.count { it in "aeiou" }
        val consonantCount = nameLength - vowelCount

        // Personality traits based on first letter
        val primaryTrait = when (firstLetter) {
            'a' -> "Ambitious Leader"
            'b' -> "Balanced Mediator"
            'c' -> "Creative Visionary"
            'd' -> "Determined Achiever"
            'e' -> "Empathetic Healer"
            'f' -> "Friendly Connector"
            'g' -> "Generous Giver"
            'h' -> "Honest Communicator"
            'i' -> "Intuitive Thinker"
            'j' -> "Joyful Entertainer"
            'k' -> "Kind Supporter"
            'l' -> "Loyal Protector"
            'm' -> "Motivated Driver"
            'n' -> "Nurturing Caregiver"
            'o' -> "Optimistic Dreamer"
            'p' -> "Passionate Creator"
            'q' -> "Quirky Individual"
            'r' -> "Reliable Foundation"
            's' -> "Sensitive Empath"
            't' -> "Trustworthy Guide"
            'u' -> "Understanding Counselor"
            'v' -> "Vibrant Energizer"
            'w' -> "Wise Teacher"
            'x' -> "Exciting Adventurer"
            'y' -> "Youthful Spirit"
            'z' -> "Zealous Pioneer"
            else -> "Unique Individual"
        }

        // Secondary traits based on name characteristics
        val secondaryTraits = mutableListOf<String>()

        if (vowelCount > consonantCount) {
            secondaryTraits.add("Expressive and outgoing")
        } else {
            secondaryTraits.add("Thoughtful and introspective")
        }

        when (nameLength) {
            in 3..4 -> secondaryTraits.add("Direct and straightforward")
            in 5..6 -> secondaryTraits.add("Balanced and harmonious")
            in 7..8 -> secondaryTraits.add("Complex and sophisticated")
            else -> secondaryTraits.add("Unique and distinctive")
        }

        // Love style prediction
        val loveStyle = when {
            firstLetter in "aeiou" -> "Emotional and passionate lover"
            firstLetter in "bcdfg" -> "Practical and steady partner"
            firstLetter in "hjklm" -> "Loyal and committed companion"
            firstLetter in "npqrs" -> "Nurturing and supportive mate"
            else -> "Adventurous and dynamic partner"
        }

        return PersonalityProfile(primaryTrait, secondaryTraits, loveStyle)
    }

    /**
     * Build comprehensive relationship analysis
     */
    private fun buildDetailedAnalysis(
        score: Int, firstName: String, secondName: String,
        numerology: Double, phonetic: Double, letter: Double, flames: Double,
        linguistic: Double, math: Double, cultural: Double, bonus: Double, psych: Double,
        personality1: PersonalityProfile, personality2: PersonalityProfile
    ): RelationshipInsights {

        // Analyze strongest compatibility factors
        val scores = mapOf(
            "Numerology" to numerology,
            "Communication" to phonetic,
            "Connection" to letter,
            "Destiny" to flames,
            "Harmony" to linguistic,
            "Balance" to math,
            "Background" to cultural,
            "Chemistry" to bonus,
            "Psychology" to psych
        )

        val strengths = scores.filter { it.value >= 75 }.keys.toList()
        val challenges = scores.filter { it.value < 50 }.keys.toList()

        // Generate relationship dynamics
        val dynamics = generateRelationshipDynamics(score, strengths, challenges)

        // Predict future together
        val future = generateFuturePredictions(score, firstName, secondName, strengths)

        // Generate advice
        val advice = generateRelationshipAdvice(score, strengths, challenges)

        return RelationshipInsights(
            personalityAnalysis = "🌟 ${firstName}: ${personality1.primaryTrait} - ${personality1.loveStyle}\n" +
                    "✨ ${secondName}: ${personality2.primaryTrait} - ${personality2.loveStyle}",

            strengthAreas = if (strengths.isNotEmpty()) {
                "💪 Your strongest areas:\n• ${strengths.joinToString("\n• ")}"
            } else {
                "💪 Building compatibility through understanding"
            },

            challengeAreas = if (challenges.isNotEmpty()) {
                "⚡ Areas to work on:\n• ${challenges.joinToString("\n• ")}"
            } else {
                "⚡ Great foundation with minor adjustments needed"
            },

            relationshipDynamics = dynamics,
            futureTogether = future,
            practicalAdvice = advice
        )
    }

    /**
     * Generate relationship dynamics description
     */
    private fun generateRelationshipDynamics(score: Int, strengths: List<String>, challenges: List<String>): String {
        return when {
            score >= 90 -> {
                "🔥 Intense magnetic attraction with deep understanding. You complement each other perfectly, " +
                        "creating a relationship that feels both exciting and secure. Natural flow in communication " +
                        "and shared vision for the future."
            }
            score >= 80 -> {
                "💕 Strong emotional connection with great potential for growth. You bring out the best " +
                        "in each other and share core values. Minor differences add spice rather than conflict."
            }
            score >= 70 -> {
                "🌈 Solid foundation with genuine compatibility. You understand each other well and have " +
                        "good communication. Some areas need attention but nothing that can't be worked through."
            }
            score >= 60 -> {
                "🌱 Growing compatibility with room for development. You have potential but need to invest " +
                        "in understanding each other better. Patience and effort will strengthen your bond."
            }
            score >= 50 -> {
                "⚖️ Balanced relationship requiring mutual effort. You have both similarities and differences " +
                        "that can either complement or clash. Success depends on your commitment to growth."
            }
            else -> {
                "🔄 Challenging but potentially transformative connection. Major differences require significant " +
                        "understanding and compromise. Consider whether you're both willing to put in the work."
            }
        }
    }

    /**
     * Generate future predictions
     */
    private fun generateFuturePredictions(score: Int, firstName: String, secondName: String, strengths: List<String>): String {
        val timeframes = when {
            score >= 85 -> "Your future looks incredibly bright together"
            score >= 70 -> "A promising future with careful nurturing"
            score >= 55 -> "Potential for growth with mutual effort"
            else -> "Friendship may serve you better than romance"
        }

        val specificPredictions = mutableListOf<String>()

        if ("Communication" in strengths) {
            specificPredictions.add("Excellent communication will resolve conflicts quickly")
        }
        if ("Numerology" in strengths) {
            specificPredictions.add("Destiny seems aligned for long-term success")
        }
        if ("Chemistry" in strengths) {
            specificPredictions.add("Strong physical and emotional attraction will endure")
        }
        if ("Psychology" in strengths) {
            specificPredictions.add("Deep psychological understanding creates lasting intimacy")
        }

        val predictions = if (specificPredictions.isNotEmpty()) {
            specificPredictions.joinToString(". ") + "."
        } else {
            "Focus on building understanding and patience."
        }

        return "🔮 $timeframes. $predictions\n\n" +
                "📅 6 months: ${getSixMonthPrediction(score)}\n" +
                "📅 1 year: ${getOneYearPrediction(score)}\n" +
                "📅 5 years: ${getFiveYearPrediction(score)}"
    }

    private fun getSixMonthPrediction(score: Int): String = when {
        score >= 80 -> "Deep bonding phase, likely discussing future plans"
        score >= 60 -> "Settling into comfortable rhythm, working through differences"
        score >= 40 -> "Testing period - discovering true compatibility"
        else -> "Major decision point about relationship direction"
    }

    private fun getOneYearPrediction(score: Int): String = when {
        score >= 80 -> "Considering major commitments, possibly moving in together"
        score >= 60 -> "Solid partnership with clear relationship direction"
        score >= 40 -> "Either significantly stronger or naturally drifting apart"
        else -> "Likely transitioned to friendship or ended"
    }

    private fun getFiveYearPrediction(score: Int): String = when {
        score >= 80 -> "Marriage or long-term commitment, possibly starting a family"
        score >= 60 -> "Stable long-term relationship with shared life goals"
        score >= 40 -> "Either deeply committed or have moved on to better matches"
        else -> "Probably happy in other relationships, good memories of this time"
    }

    /**
     * Generate practical relationship advice
     */
    private fun generateRelationshipAdvice(score: Int, strengths: List<String>, challenges: List<String>): String {
        val advice = mutableListOf<String>()

        // Score-based general advice
        when {
            score >= 80 -> {
                advice.add("Celebrate your natural compatibility while staying open to growth")
                advice.add("Don't take your connection for granted - nurture it actively")
            }
            score >= 60 -> {
                advice.add("Focus on your strengths while addressing challenges together")
                advice.add("Regular honest communication will deepen your bond")
            }
            else -> {
                advice.add("Approach this relationship with realistic expectations")
                advice.add("Consider whether you're compatible as friends first")
            }
        }

        // Challenge-specific advice
        if ("Communication" in challenges) {
            advice.add("Practice active listening and express feelings clearly")
        }
        if ("Psychology" in challenges) {
            advice.add("Spend time understanding each other's personalities and needs")
        }
        if ("Background" in challenges) {
            advice.add("Embrace your different backgrounds as learning opportunities")
        }

        // Strength-based advice
        if ("Numerology" in strengths) {
            advice.add("Trust your instincts - this connection has deep significance")
        }
        if ("Chemistry" in strengths) {
            advice.add("Your natural attraction is strong - balance passion with emotional intimacy")
        }

        return "💡 Relationship Guidance:\n• ${advice.joinToString("\n• ")}"
    }

    /**
     * Update the love tips section with detailed insights
     */
    private fun updateLoveTipsWithInsights(insights: RelationshipInsights) {
        val fullInsights = "${insights.personalityAnalysis}\n\n" +
                "${insights.strengthAreas}\n\n" +
                "${insights.challengeAreas}\n\n" +
                "💫 Relationship Dynamics:\n${insights.relationshipDynamics}\n\n" +
                "${insights.futureTogether}\n\n" +
                "${insights.practicalAdvice}\n\n" +
                "Remember: This analysis is for entertainment and self-reflection. Real relationships depend on communication, respect, shared values, and mutual effort! 💕"

        // Find the love tips TextView and update it
        val loveTipsContent = binding.root.findViewById<TextView>(R.id.loveTipsContent)
        if (loveTipsContent != null) {
            loveTipsContent.text = fullInsights
        } else {
            // Fallback - try to find any TextView in the love tips card
            val loveTipsCard = binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.loveTipsCard)
            val textView = loveTipsCard?.findViewById<TextView>(android.R.id.text1)
            textView?.text = fullInsights
        }

        // Show the toggle button after insights are loaded
        binding.root.findViewById<Button>(R.id.btnToggleInsights)?.visibility = View.VISIBLE

    }

    // Data classes for structured insights
    data class PersonalityProfile(
        val primaryTrait: String,
        val secondaryTraits: List<String>,
        val loveStyle: String
    )

    data class RelationshipInsights(
        val personalityAnalysis: String,
        val strengthAreas: String,
        val challengeAreas: String,
        val relationshipDynamics: String,
        val futureTogether: String,
        val practicalAdvice: String
    )

    private fun clearFields() {
        binding.etFirstName.text?.clear()
        binding.etSecondName.text?.clear()
        binding.resultLayout.visibility = View.GONE
        binding.loveResultCard.visibility = View.GONE
        currentScore = 0

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
            calculateLove()
        }
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    companion object {
        private const val TAG = "LoveCalculatorFragment"
    }
}