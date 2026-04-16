package dev.jefrien.walkrush.data.remote.openai

import android.util.Log
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.SessionType
import dev.jefrien.walkrush.domain.model.routine.WeeklyPlan
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

private const val TAG = "OpenAIRoutineGenerator"

class OpenAIRoutineGenerator(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val json: Json
) {

    suspend fun generateRoutine(profile: UserProfile): Result<Routine> = try {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("OPENAI_API_KEY no está configurada"))
        }

        val request = OpenAIRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                OpenAIMessage(
                    role = "system",
                    content = SYSTEM_PROMPT
                ),
                OpenAIMessage(
                    role = "user",
                    content = buildUserPrompt(profile)
                )
            ),
            responseFormat = ResponseFormat("json_object"),
            maxTokens = 3500,
            temperature = 0.5f
        )

        Log.d(TAG, "Enviando solicitud a OpenAI...")

        val response: OpenAIResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(request)
        }.body()

        val content = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Respuesta vacía de OpenAI")

        Log.d(TAG, "Respuesta recibida, parseando JSON...")

        val parsed = json.decodeFromString<OpenAIRoutineJson>(content)
        val routine = parsed.toDomain(profile.id)

        Log.d(TAG, "Rutina generada: ${routine.totalWeeks} semanas, ${routine.weeklyPlans.sumOf { it.sessions.size }} sesiones")

        Result.success(routine)
    } catch (e: Exception) {
        Log.e(TAG, "Error generando rutina con OpenAI: ${e.message}", e)
        Result.failure(e)
    }

    private fun buildUserPrompt(profile: UserProfile): String {
        val totalWeeks = 4 // Generar solo primer mes para velocidad
        val bmi = profile.weightKg / ((profile.heightCm / 100) * (profile.heightCm / 100))
        val inclineConstraint = if (profile.treadmillCapabilities.hasIncline) {
            "Inclinacion max: ${profile.treadmillCapabilities.maxInclinePercent}%."
        } else {
            "NO inclinacion. inclineMin=0, inclineMax=0 siempre."
        }

        return """Crea un plan de caminadora. Solo 4 semanas. JSON valido.

Usuario: ${profile.weightKg}kg, ${profile.heightCm}cm, ${profile.age}años, IMC ${String.format("%.1f", bmi)}
Meta: ${profile.fitnessGoal.type.name}, ${profile.targetWeightKg}kg en ${profile.timelineMonths}meses
Dias: ${profile.daysPerWeek}/semana | Nivel: ${profile.intensityLevel.name}
Max speed: ${profile.treadmillCapabilities.maxSpeedKmh}km/h | $inclineConstraint

Reglas:
- 4 semanas exactas
- ${profile.daysPerWeek} sesiones por semana
- dayOfWeek 1-7
- types: STEADY_STATE, INTERVALS, INCLINE_WALK, RECOVERY
- Progresion gradual
- 3 recomendaciones en array

JSON:
{"totalWeeks":4,"projectedWeightLossKg":3.0,"recommendations":["r1","r2","r3"],"weeklyPlans":[{"weekNumber":1,"focus":"adaptacion","sessions":[{"dayOfWeek":1,"durationMinutes":25,"speedMin":5.0,"speedMax":5.5,"inclineMin":0,"inclineMax":1,"type":"STEADY_STATE","estimatedCalories":180,"notes":"Mantén postura erguida"}]}]}"""
    }

    companion object {
        private val SYSTEM_PROMPT = """
Eres un entrenador personal certificado especializado en caminadoras (treadmill).
Tu tarea es crear planes de entrenamiento seguros, efectivos y personalizados.
SIEMPRE responde con JSON válido siguiendo exactamente la estructura solicitada.
No incluyas markdown, explicaciones ni texto fuera del JSON.
        """.trimIndent()
    }
}

// OpenAI API DTOs
@Serializable
private data class OpenAIRequest(
    val model: String,
    val messages: List<OpenAIMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat,
    @SerialName("max_tokens") val maxTokens: Int = 3500,
    val temperature: Float = 0.5f
)

@Serializable
private data class OpenAIMessage(
    val role: String,
    val content: String
)

@Serializable
private data class ResponseFormat(
    val type: String
)

@Serializable
private data class OpenAIResponse(
    val choices: List<OpenAIChoice>
)

@Serializable
private data class OpenAIChoice(
    val message: OpenAIMessage
)

// JSON response from OpenAI
@Serializable
private data class OpenAIRoutineJson(
    val totalWeeks: Int,
    val projectedWeightLossKg: Float? = null,
    val recommendations: List<String> = emptyList(),
    val weeklyPlans: List<OpenAIWeeklyPlanJson> = emptyList()
) {
    fun toDomain(userId: String): Routine {
        val routineId = UUID.randomUUID().toString()
        return Routine(
            id = routineId,
            userId = userId,
            totalWeeks = totalWeeks,
            projectedWeightLossKg = projectedWeightLossKg,
            recommendations = recommendations,
            isActive = true,
            weeklyPlans = weeklyPlans.map { it.toDomain(routineId) }
        )
    }
}

@Serializable
private data class OpenAIWeeklyPlanJson(
    val weekNumber: Int,
    val focus: String,
    val sessions: List<OpenAISessionJson> = emptyList()
) {
    fun toDomain(routineId: String): WeeklyPlan {
        val weeklyPlanId = UUID.randomUUID().toString()
        return WeeklyPlan(
            id = weeklyPlanId,
            routineId = routineId,
            weekNumber = weekNumber,
            focus = focus,
            sessions = sessions.map { it.toDomain(weeklyPlanId) }
        )
    }
}

@Serializable
private data class OpenAISessionJson(
    val dayOfWeek: Int,
    val durationMinutes: Int,
    val speedMin: Float,
    val speedMax: Float,
    val inclineMin: Float? = 0f,
    val inclineMax: Float? = 0f,
    val type: String,
    val estimatedCalories: Int,
    val notes: String
) {
    fun toDomain(weeklyPlanId: String): WorkoutSession {
        return WorkoutSession(
            id = UUID.randomUUID().toString(),
            weeklyPlanId = weeklyPlanId,
            dayOfWeek = dayOfWeek,
            durationMinutes = durationMinutes,
            speedMin = speedMin,
            speedMax = speedMax,
            inclineMin = inclineMin,
            inclineMax = inclineMax,
            type = SessionType.valueOf(type.uppercase()),
            estimatedCalories = estimatedCalories,
            notes = notes
        )
    }
}
