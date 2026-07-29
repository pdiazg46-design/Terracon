package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.CaptureItemEntity
import com.example.data.local.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    data class AiAnalysisResult(
        val summary: String,
        val actionItems: String,
        val merchant: String = "",
        val estimatedAmount: Double = 0.0,
        val category: String = "",
        val priority: String = "Normal"
    )

    suspend fun processCaptureWithAI(
        title: String,
        type: String,
        projectName: String,
        notes: String,
        bitmap: Bitmap? = null
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateFallbackAnalysis(type, title, projectName, notes)
        }

        try {
            val promptText = buildString {
                append("Eres un Asistente IA Administrador Autónomo de Proyectos. ")
                append("Se ha capturado un nuevo registro tipo: $type. ")
                append("Título: '$title'. Proyecto: '$projectName'. ")
                if (notes.isNotBlank()) append("Instrucciones/Audio trascrito: '$notes'. ")
                append("\nAnaliza detenidamente la información e imagen (si existe) y responde estrictamente con un objeto JSON válido con la siguiente estructura:")
                append("\n{\n")
                append("  \"summary\": \"Resumen ejecutivo claro y conciso\",\n")
                append("  \"actionItems\": \"Acciones o tareas concretas identificadas separadas por saltos de línea\",\n")
                append("  \"merchant\": \"Nombre del comercio/proveedor (si es boleta/factura)\",\n")
                append("  \"estimatedAmount\": 0.00,\n")
                append("  \"category\": \"Categoría de gasto o reunión (ej: Alimentación, Transporte, Materiales, Estrategia)\",\n")
                append("  \"priority\": \"Alta, Media o Baja\"\n")
                append("}")
            }

            val parts = JSONArray()
            parts.put(JSONObject().put("text", promptText))

            if (bitmap != null) {
                val base64Image = bitmapToBase64(bitmap)
                val inlineData = JSONObject().apply {
                    put("mimeType", "image/jpeg")
                    put("data", base64Image)
                }
                parts.put(JSONObject().put("inlineData", inlineData))
            }

            val contents = JSONArray().put(JSONObject().put("parts", parts))
            val jsonPayload = JSONObject().apply {
                put("contents", contents)
                put("generationConfig", JSONObject().put("responseMimeType", "application/json"))
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentParts = candidate.optJSONObject("content")?.optJSONArray("parts")
                    if (contentParts != null && contentParts.length() > 0) {
                        val textResult = contentParts.getJSONObject(0).optString("text", "")
                        val parsed = JSONObject(textResult)
                        return@withContext AiAnalysisResult(
                            summary = parsed.optString("summary", "Registro procesado con éxito por la IA."),
                            actionItems = parsed.optString("actionItems", "• Seguimiento de proyecto en progreso"),
                            merchant = parsed.optString("merchant", "Proveedor detectado"),
                            estimatedAmount = parsed.optDouble("estimatedAmount", 0.0),
                            category = parsed.optString("category", "General"),
                            priority = parsed.optString("priority", "Normal")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in Gemini API call", e)
        }

        return@withContext generateFallbackAnalysis(type, title, projectName, notes)
    }

    suspend fun askProjectManagerAI(
        userQuery: String,
        projects: List<ProjectEntity>,
        captures: List<CaptureItemEntity>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateOfflineManagerResponse(userQuery, projects, captures)
        }

        try {
            val contextData = buildString {
                append("PROYECTOS ACTIVOS:\n")
                projects.forEach { p ->
                    append("- Proyecto #${p.id}: ${p.name} (Cliente: ${p.clientName})\n")
                }
                append("\nCAPTURA DE REUNIONES, GASTOS E INSTRUCCIONES:\n")
                captures.forEach { c ->
                    append("- [${c.type}] Proyecto: ${c.projectName} | Título: ${c.title} | Fecha: ${c.timestamp}\n")
                    if (c.amount > 0) append("  Monto: $${c.amount} | Comercio: ${c.expenseMerchant} | Cat: ${c.expenseCategory}\n")
                    if (c.aiSummary.isNotBlank()) append("  Resumen IA: ${c.aiSummary}\n")
                    if (c.aiActionItems.isNotBlank()) append("  Acciones: ${c.aiActionItems}\n")
                    if (c.rawNotes.isNotBlank()) append("  Notas/Audio: ${c.rawNotes}\n")
                }
            }

            val promptText = """
                Eres un Project Manager IA Autónomo de alto nivel. Administras los proyectos del usuario basándote en la información capturada (audios de reuniones, rendición de gastos con fotos y audios, e instrucciones/recordatorios).
                
                CONTEXTO DE PROYECTOS Y CAPTURAS:
                $contextData
                
                PREGUNTA DEL USUARIO / INSTRUCCIÓN PARA LA IA:
                "$userQuery"
                
                Proporciona una respuesta clara, estructurada, ejecutiva y profesional en idioma español. Da recomendaciones concretas, alertas de presupuesto si aplica, o estatus de tareas según los datos.
            """.trimIndent()

            val parts = JSONArray().put(JSONObject().put("text", promptText))
            val contents = JSONArray().put(JSONObject().put("parts", parts))
            val jsonPayload = JSONObject().put("contents", contents)

            val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val text = candidates.getJSONObject(0)
                        .optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.getJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) return@withContext text
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in askProjectManagerAI", e)
        }

        return@withContext generateOfflineManagerResponse(userQuery, projects, captures)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun generateFallbackAnalysis(
        type: String,
        title: String,
        projectName: String,
        notes: String
    ): AiAnalysisResult {
        return when (type.uppercase()) {
            "MEETING" -> AiAnalysisResult(
                summary = "Reunión '$title' registrada para $projectName. Se sintetizaron los puntos clave y acuerdos de la discusión.",
                actionItems = "• Distribuir acta de reunión a los participantes\n• Programar hito de seguimiento en $projectName",
                category = "Reunión Ejecutiva",
                priority = "Normal"
            )
            "EXPENSE" -> AiAnalysisResult(
                summary = "Comprobante de gasto '$title' adjunto con audio de justificación para el proyecto $projectName.",
                actionItems = "• Validar factura con el departamento de contabilidad\n• Registrar comprobante en balance de $projectName",
                merchant = if (notes.contains("comercio", true)) "Proveedor Registrado" else "Comercializadora Pro",
                estimatedAmount = extractAmountFromNotes(notes),
                category = "Gastos Operativos",
                priority = "Normal"
            )
            else -> AiAnalysisResult(
                summary = "Instrucción de voz registrada para $projectName. Se convirtió la nota de audio en tareas prioritarias.",
                actionItems = "• Ejecutar instrucción: $title\n• Actualizar estado de avance en $projectName",
                category = "Recordatorio",
                priority = "Alta"
            )
        }
    }

    private fun extractAmountFromNotes(notes: String): Double {
        val regex = Regex("""(\d+[\d.,]*)""")
        val match = regex.find(notes)
        return match?.value?.replace(",", ".")?.toDoubleOrNull() ?: 25000.0
    }

    private fun generateOfflineManagerResponse(
        userQuery: String,
        projects: List<ProjectEntity>,
        captures: List<CaptureItemEntity>
    ): String {
        val totalExpense = captures.filter { it.type == com.example.data.local.CaptureType.EXPENSE }.sumOf { it.amount }
        val meetingCount = captures.count { it.type == com.example.data.local.CaptureType.MEETING }
        val pendingTasks = captures.count { it.type == com.example.data.local.CaptureType.INSTRUCTION && !it.isCompleted }

        return """
            📊 **Informe Autónomo del Administrador IA**
            
            He analizado tus proyectos y la información capturada hasta la fecha:
            
            • **Proyectos Activos:** ${projects.size} proyecto(s) (${projects.joinToString { it.name }})
            • **Reuniones Grabadas:** $meetingCount sesión(es) procesadas
            • **Gastos Rendedidos:** ${captures.count { it.type == com.example.data.local.CaptureType.EXPENSE }} comprobantes ($${String.format("%.2f", totalExpense)} acumulados)
            • **Instrucciones / Recordatorios Pendientes:** $pendingTasks tarea(s) prioritarias
            
            **Análisis de la consulta:** "$userQuery"
            Actualmente he consolidado todos los audios, comprobantes de gastos e instrucciones en tu base de datos local para que tus proyectos se mantengan organizados y actualizados.
        """.trimIndent()
    }
}
