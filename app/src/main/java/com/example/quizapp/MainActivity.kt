package com.example.quizapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.quizapp.ui.theme.QuizAppTheme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.ai.client.generativeai.GenerativeModel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {


// initialize model
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuizAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QuizScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

data class Question(
    val text: String,
    val options: List<String>,
    val correctIndex: Set<Int>
)


@Composable
fun QuizScreen(modifier: Modifier = Modifier) {
    var data by remember { mutableStateOf<List<Question>?>(null) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedOptions by remember { mutableStateOf(setOf<Int>()) }

    var isFinished by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var score by remember { mutableStateOf(0) }
//    those are setting for the memory of the app.
    var apiKey = BuildConfig.apiKey
    val generativeModel = remember{ GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = apiKey
    )}
//    official authentication step that unlocks the connection
//    between my Android app and the Gemini AI.

    val scope = rememberCoroutineScope()
//    define the Coroutinesscope tied to this Composable's lifecycle

    Column(modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        if (data ==null && !isFinished) {
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFFbf0d4c))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "quiz in progress...", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            } else {
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = ""
                        val prompt = """
                            You are an elementary-level math question-and-answer system.
                            Generate 5 multiple-choice questions about basic knowledge.
                            Requirement:1. Every question MUST have at least 2 correct answers (e.g., both A and C are correct).
                                        2. Append "(Select all that apply)" to the end of every question text.
                                        3. Return ONLY a plain JSON array. Do NOT include Markdown formatting like ```json.
                                        JSON Structure:
                                [
                                 {
                                    "question": "Which of these numbers are even? (Select all that apply)",
                                    "options": ["2", "5", "8", "11"],
                                    "correctIndices": [0, 2]
                                 }
                               ] """.trimIndent()
// loading page and generating new questions.
                        scope.launch {
                            try {
                                val response = withContext(Dispatchers.IO) {
                                    generativeModel.generateContent(prompt)
                                }
                                val cleanJson =
                                    (response.text ?: "").replace("```json", "").replace("```", "")
                                        .trim()
                                val list = mutableListOf<Question>()
                                val array = JSONArray(cleanJson)
                                for (i in 0 until array.length()) {
                                    val obj = array.getJSONObject(i)
                                    val optionArray = obj.getJSONArray("options")
                                    val answerArray = obj.getJSONArray("correctIndices")
                                    list.add(
                                        Question(
                                            obj.getString("question"),
                                            List(optionArray.length()) { optionArray.getString(it) },
                                            setOf(*Array(answerArray.length()) {
                                                answerArray.getInt(
                                                    it
                                                )
                                            })
                                        )
                                    )
                                }
                                data = list
                            } catch (e: Exception) {
                                errorMessage = "Error: ${e.message}"
                            } finally {
                                isLoading = false
                            }
                        }

                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(56.dp)

                ) {

                    Text(
                        text = "Ready to challenge yourself?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                }
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

            }


        }else if (data != null && !isFinished) {
            val currentQuestion = data!![currentQuestionIndex]
            Text(
                "Question ${currentQuestionIndex + 1} of ${data!!.size}",
                fontSize = 16.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                currentQuestion.text,
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(18.dp))




            currentQuestion.options.forEachIndexed { index, option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Checkbox(

                        checked = selectedOptions.contains(index),
                        onCheckedChange = { isChecked ->
                            val newSet = selectedOptions.toMutableSet()
                            if (isChecked) {
                                newSet.add(index)
                            } else {
                                newSet.remove(index)
                            }
                            selectedOptions = newSet
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF2196F3)
                        )
                    )
                    Text(text = option, fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))



            Button(
                onClick = {
                    if (selectedOptions == currentQuestion.correctIndex) score++
                    if (currentQuestionIndex < data!!.size - 1) {
                        currentQuestionIndex++
                        selectedOptions = setOf()
                    } else {
                        isFinished = true
                    }
                },
                enabled = selectedOptions.isNotEmpty(),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(if (currentQuestionIndex < data!!.size - 1) "Next" else "Finish")
            }
        }
                else if (isFinished) {
                    Text("You finished all questions",fontSize = 32.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(18.dp))
                Text("Your score is $score", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(48.dp))
                OutlinedButton(
                    onClick = {
                        data = null; currentQuestionIndex = 0; score = 0; selectedOptions = setOf(); isFinished = false

                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text(" try again", fontSize = 18.sp)
                }            }










        }}



















