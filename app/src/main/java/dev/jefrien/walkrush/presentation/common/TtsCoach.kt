package dev.jefrien.walkrush.presentation.common

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dev.jefrien.walkrush.domain.model.routine.WorkoutPhase
import java.util.Locale

class TtsCoach(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pendingQueue = mutableListOf<String>()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    engine.language = Locale.of("es", "ES")
                    engine.setSpeechRate(0.92f)
                    engine.setPitch(1.0f)
                    isReady = true
                    pendingQueue.forEach { speakInternal(it) }
                    pendingQueue.clear()
                }
            } else {
                Log.e("TtsCoach", "Error inicializando TTS: $status")
            }
        }
    }

    fun announcePhaseStart(phase: WorkoutPhase) {
        val text = buildString {
            append(phase.title)
            when {
                phase.type.name == "WARM_UP" -> append(", calentamiento")
                phase.type.name == "COOL_DOWN" -> append(", enfriamiento")
                phase.type.name == "RUN" -> append(", corre")
                phase.type.name == "WALK" -> append(", camina")
                phase.type.name == "RECOVERY" -> append(", recuperación")
            }
            append(" a ${phase.targetSpeedKmh.toInt()} kilómetros por hora")
            if ((phase.targetInclinePercent ?: 0f) > 0f) {
                append(", inclinación ${phase.targetInclinePercent?.toInt()} por ciento")
            }
            append(", ${phase.durationSeconds / 60} minutos")
            if (!phase.notes.isNullOrBlank() && phase.notes.length <= 60) {
                append(". ${phase.notes}")
            }
        }
        speak(text)
    }

    fun announceWorkoutStart() {
        speak("¡Entrenamiento iniciado! Dale lo mejor de ti.")
    }

    fun announceWorkoutComplete() {
        speak("¡Entrenamiento completado! Excelente trabajo.")
    }

    fun announceResume() {
        speak("Continuamos.")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }

    private fun speak(text: String) {
        if (isReady) {
            speakInternal(text)
        } else {
            pendingQueue.add(text)
        }
    }

    private fun speakInternal(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }
}
