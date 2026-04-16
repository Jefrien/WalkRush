package dev.jefrien.walkrush.data.remote.openai

import android.util.Log
import dev.jefrien.walkrush.domain.model.routine.PhaseType
import dev.jefrien.walkrush.domain.model.routine.Routine
import dev.jefrien.walkrush.domain.model.routine.SessionType
import dev.jefrien.walkrush.domain.model.routine.WeeklyPlan
import dev.jefrien.walkrush.domain.model.routine.WorkoutPhase
import dev.jefrien.walkrush.domain.model.routine.WorkoutSession
import dev.jefrien.walkrush.domain.model.userprofile.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.statement.bodyAsText
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
            maxTokens = 4096,
            temperature = 0.5f
        )

        Log.d(TAG, "Enviando solicitud a OpenAI...")

        val responseBody = httpClient.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(request)
        }.bodyAsText()

        Log.d(TAG, "Respuesta cruda de OpenAI: ${responseBody.take(500)}")

        val response = json.decodeFromString<OpenAIResponse>(responseBody)

        val content = response.choices?.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Respuesta vacía o inválida de OpenAI: ${response.error?.message ?: responseBody}")

        Log.d(TAG, "Respuesta recibida, parseando JSON...")

        val parsed = json.decodeFromString<OpenAIRoutineJson>(content)
        val routine = parsed.toDomain(profile.id)

        val totalPhases = routine.weeklyPlans.sumOf { it.sessions.sumOf { s -> s.phases.size } }
        Log.d(TAG, "Rutina generada: ${routine.totalWeeks} semanas, ${totalPhases} fases totales")

        Result.success(routine)
    } catch (e: Exception) {
        Log.e(TAG, "Error generando rutina con OpenAI: ${e.message}", e)
        Result.failure(e)
    }

    private fun buildUserPrompt(profile: UserProfile): String {
        val totalWeeks = 4
        val bmi = profile.weightKg / ((profile.heightCm / 100) * (profile.heightCm / 100))
        val inclineConstraint = if (profile.treadmillCapabilities.hasIncline) {
            "Inclinacion max: ${profile.treadmillCapabilities.maxInclinePercent}%."
        } else {
            "NO inclinacion. targetInclinePercent siempre = 0."
        }

        val (minMinutes, maxMinutes, warmUpMin, coolDownMin) = when (profile.intensityLevel.name) {
            "BEGINNER" -> listOf(25, 40, 5, 5)
            "INTERMEDIATE" -> listOf(35, 50, 5, 5)
            "INTENSE" -> listOf(45, 70, 5, 5)
            else -> listOf(30, 45, 5, 5)
        }

        val runRatio = when (profile.intensityLevel.name) {
            "BEGINNER" -> "10-20% del tiempo total en RUN"
            "INTERMEDIATE" -> "30-40% del tiempo total en RUN"
            "INTENSE" -> "45-60% del tiempo total en RUN (usa muchos intervalos WALK/RUN alternados)"
            else -> "30% en RUN"
        }

        return """Crea un plan de caminadora/cinta INTENSO y DETALLADO. 4 semanas. JSON valido.

Usuario: ${profile.weightKg}kg, ${profile.heightCm}cm, ${profile.age}años, IMC ${String.format("%.1f", bmi)}
Meta: ${profile.fitnessGoal.type.name}, ${profile.targetWeightKg}kg en ${profile.timelineMonths}meses
Dias: ${profile.daysPerWeek}/semana | Nivel: ${profile.intensityLevel.name}
Max speed: ${profile.treadmillCapabilities.maxSpeedKmh}km/h | $inclineConstraint

REGLAS OBLIGATORIAS DE DURACION:
- Nivel ${profile.intensityLevel.name}: CADA sesion debe durar entre $minMinutes y $maxMinutes minutos en TOTAL (incluyendo calentamiento y enfriamiento).
- Calentamiento: exactamente $warmUpMin minutos.
- Enfriamiento: exactamente $coolDownMin minutos.
- $runRatio
- Si es INTENSE: alterna WALK/RUN en intervalos cortos (2-4 min cada uno), repetidos muchas veces.
- Si es INTENSE: usa inclinacion elevada en fases WALK para aumentar dificultad.
- Si es BEGINNER: usa fases WALK largas (8-12 min) y pocas fases RUN cortas (1-2 min).

REGLAS DE VELOCIDAD POR NIVEL:
- BEGINNER: WALK 4.5-5.5 km/h, RUN max 7 km/h
- INTERMEDIATE: WALK 5.0-6.0 km/h, RUN 7-9 km/h  
- INTENSE: WALK 5.5-6.5 km/h, RUN 8-${profile.treadmillCapabilities.maxSpeedKmh.coerceAtMost(12f)} km/h
- NUNCA superes ${profile.treadmillCapabilities.maxSpeedKmh} km/h.

ESTRUCTURA DE FASES:
Cada fase tiene: type (WARM_UP, WALK, RUN, RECOVERY, COOL_DOWN), title (corto en español), targetSpeedKmh, targetInclinePercent, durationSeconds, notes.

EJEMPLO INTENSO (50 min total):
1. WARM_UP: 5min a 4km/h
2. WALK: 3min a 6km/h, inclinacion 2%
3. RUN: 3min a 9km/h
4. WALK: 3min a 6km/h, inclinacion 3%
5. RUN: 3min a 10km/h
6. WALK: 3min a 6km/h
7. RUN: 3min a 9km/h
8. RECOVERY: 2min a 5km/h
9. RUN: 3min a 10km/h
10. WALK: 3min a 6km/h
11. RUN: 3min a 9km/h
12. COOL_DOWN: 5min a 3.5km/h

Cada semana: ${profile.daysPerWeek} sesiones. Distribuye dayOfWeek 1-7. Varia sessionType.
Progresion: semana 4 mas dificil que semana 1 (aumenta velocidades o inclinacion levemente).

JSON exacto:
{"totalWeeks":4,"projectedWeightLossKg":5.0,"recommendations":["Consejo personalizado 1","Consejo 2","Consejo 3"],"weeklyPlans":[{"weekNumber":1,"focus":"Adaptacion","sessions":[{"dayOfWeek":1,"type":"INTERVALS","estimatedCalories":450,"notes":"Mantén postura erguida","phases":[{"type":"WARM_UP","title":"Calentamiento","targetSpeedKmh":4.0,"targetInclinePercent":0,"durationSeconds":300,"notes":"Paso ligero"},{"type":"WALK","title":"Caminata activa","targetSpeedKmh":6.0,"targetInclinePercent":2,"durationSeconds":180,"notes":"Ritmo constante"},{"type":"RUN","title":"Trote intenso","targetSpeedKmh":9.0,"targetInclinePercent":0,"durationSeconds":180,"notes":"Respira profundo"},{"type":"COOL_DOWN","title":"Enfriamiento","targetSpeedKmh":3.5,"targetInclinePercent":0,"durationSeconds":300,"notes":"Relaja el cuerpo"}]}]}]}""".trimIndent()
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
private data class OpenAIError(
    val message: String? = null,
    val type: String? = null
)

@Serializable
private data class OpenAIResponse(
    val choices: List<OpenAIChoice>? = null,
    val error: OpenAIError? = null
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
    val type: String,
    val estimatedCalories: Int,
    val notes: String,
    val phases: List<OpenAIPhaseJson> = emptyList()
) {
    fun toDomain(weeklyPlanId: String): WorkoutSession {
        val sessionId = UUID.randomUUID().toString()
        return WorkoutSession(
            id = sessionId,
            weeklyPlanId = weeklyPlanId,
            dayOfWeek = dayOfWeek,
            type = SessionType.valueOf(type.uppercase()),
            estimatedCalories = estimatedCalories,
            notes = notes,
            phases = phases.mapIndexed { index, phase ->
                phase.toDomain(sessionId, index)
            }
        )
    }
}

@Serializable
private data class OpenAIPhaseJson(
    val type: String,
    val title: String,
    val targetSpeedKmh: Float,
    val targetInclinePercent: Float? = 0f,
    val durationSeconds: Int,
    val notes: String? = null
) {
    fun toDomain(sessionId: String, index: Int): WorkoutPhase {
        return WorkoutPhase(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            orderIndex = index,
            type = PhaseType.valueOf(type.uppercase()),
            title = title,
            targetSpeedKmh = targetSpeedKmh,
            targetInclinePercent = targetInclinePercent,
            durationSeconds = durationSeconds,
            notes = notes
        )
    }
}
