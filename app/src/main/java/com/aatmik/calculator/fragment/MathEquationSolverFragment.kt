import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aatmik.calculator.databinding.FragmentMathEquationSolverBinding
import java.util.*

class MathEquationSolverFragment : Fragment() {

    private var _binding: FragmentMathEquationSolverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMathEquationSolverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.backIv.setOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.solveBtn.setOnClickListener {
            solveEquation()
        }
    }

    private fun solveEquation() {
        val equation = binding.equationEt.text.toString().trim()

        if (equation.isEmpty()) {
            binding.equationInputLayout.error = "Please enter an equation"
            return
        } else {
            binding.equationInputLayout.error = null
        }

        try {
            val (result, steps) = processEquation(equation)
            binding.resultTv.text = "Result: $result"
            binding.stepsTv.text = "Steps:\n${steps.joinToString("\n")}"
        } catch (e: Exception) {
            binding.resultTv.text = "Error: Unable to solve the equation"
            binding.stepsTv.text = "Please check the equation format and try again"
        }
    }

    private fun processEquation(equation: String): Pair<String, List<String>> {
        val steps = mutableListOf<String>()
        steps.add("Original equation: $equation")

        // Check if it's a simple arithmetic expression
        if (!equation.contains("=") && !equation.contains("x")) {
            val result = evaluateArithmeticExpression(equation)
            steps.add("Evaluating arithmetic expression")
            return Pair(result.toString(), steps)
        }

        // Handle linear equations
        if (equation.contains("=")) {
            return solveLinearEquation(equation, steps)
        }

        throw IllegalArgumentException("Unsupported equation type")
    }

    private fun evaluateArithmeticExpression(expression: String): Double {
        val tokens = tokenizeExpression(expression)
        return evaluateTokens(tokens)
    }

    private fun tokenizeExpression(expression: String): List<String> {
        return expression.replace("(", "( ")
            .replace(")", " )")
            .split("\\s+".toRegex())
    }

    private fun evaluateTokens(tokens: List<String>): Double {
        val numbers = Stack<Double>()
        val operators = Stack<String>()

        for (token in tokens) {
            when {
                token.matches(Regex("-?\\d+(\\.\\d+)?")) -> numbers.push(token.toDouble())
                token in setOf("+", "-", "*", "/") -> {
                    while (operators.isNotEmpty() && hasPrecedence(token, operators.peek())) {
                        numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()))
                    }
                    operators.push(token)
                }
                token == "(" -> operators.push(token)
                token == ")" -> {
                    while (operators.peek() != "(") {
                        numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()))
                    }
                    operators.pop()
                }
            }
        }

        while (operators.isNotEmpty()) {
            numbers.push(applyOperator(operators.pop(), numbers.pop(), numbers.pop()))
        }

        return numbers.pop()
    }

    private fun hasPrecedence(op1: String, op2: String): Boolean {
        if (op2 == "(" || op2 == ")") return false
        if ((op1 == "*" || op1 == "/") && (op2 == "+" || op2 == "-")) return false
        return true
    }

    private fun applyOperator(operator: String, b: Double, a: Double): Double {
        return when (operator) {
            "+" -> a + b
            "-" -> a - b
            "*" -> a * b
            "/" -> a / b
            else -> throw IllegalArgumentException("Invalid operator")
        }
    }

    private fun solveLinearEquation(equation: String, steps: MutableList<String>): Pair<String, List<String>> {
        var (leftSide, rightSide) = equation.split("=").map { it.trim() }
        var coefficient = 1.0
        var constant = 0.0

        // Move all terms with x to the left side and all constants to the right side
        if (rightSide.contains("x")) {
            val temp = leftSide
            leftSide = rightSide
            rightSide = temp
        }

        val leftTerms = leftSide.split(Regex("(?=[-+])"))
        for (term in leftTerms) {
            if (term.contains("x")) {
                coefficient += if (term == "x" || term == "+x") 1.0
                else if (term == "-x") -1.0
                else term.replace("x", "").toDouble()
            } else {
                constant -= term.toDouble()
            }
        }

        constant += rightSide.toDouble()

        steps.add("Rearranged equation: ${coefficient}x = $constant")

        val result = constant / coefficient
        steps.add("Divided both sides by $coefficient")
        steps.add("x = $result")

        return Pair(result.toString(), steps)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}