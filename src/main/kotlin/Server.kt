import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

// Модели данных
@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val thinking: String? = null
)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false
)

@Serializable
data class ChatResponse(
    val model: String,
    val message: ChatMessage,
    val done: Boolean
)

@Serializable
data class ApiChatRequest(
    val message: String,
    val model: String = "qwen2.5:1.5b"
)

@Serializable
data class ApiChatResponse(
    val response: String,
    val model: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)

@Serializable
data class HealthResponse(
    val status: String,
    val ollamaConnected: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Сервис для работы с Ollama
class OllamaService(
    private val baseUrl: String = System.getenv("OLLAMA_URL") ?: "http://localhost:11434",
    private val defaultModel: String = System.getenv("OLLAMA_MODEL") ?: "qwen2.5:1.5b"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 120000
        }
    }

    private val conversationHistories = mutableMapOf<String, MutableList<ChatMessage>>()

    suspend fun chat(userMessage: String, model: String = defaultModel, sessionId: String = "default"): String {
        val history = conversationHistories.getOrPut(sessionId) { mutableListOf() }
        history.add(ChatMessage(role = "user", content = userMessage))

        val request = ChatRequest(
            model = model,
            messages = history,
            stream = false
        )

        return try {
            val response: HttpResponse = client.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseText = response.bodyAsText()
            val lines = responseText.trim().lines().filter { it.isNotBlank() }

            // Ищем последнюю строку с done=true и берем из нее content
            var actualContent = "Нет ответа от модели"

            for (line in lines) {
                try {
                    val resp = json.decodeFromString<ChatResponse>(line)
                    if (resp.done && resp.message.content.isNotBlank()) {
                        actualContent = resp.message.content
                    }
                } catch (_: Exception) {
                    // Игнорируем ошибки парсинга
                }
            }

            history.add(ChatMessage(role = "assistant", content = actualContent))

            actualContent
        } catch (e: Exception) {
            history.removeLast() // Удаляем сообщение пользователя при ошибке
            throw e
        }
    }

    suspend fun checkHealth(): Boolean {
        return try {
            val response: HttpResponse = client.get("$baseUrl/api/tags")
            response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    fun clearHistory(sessionId: String = "default") {
        conversationHistories[sessionId]?.clear()
    }

    fun close() {
        client.close()
    }
}

fun Application.module() {
    val ollamaService = OllamaService()

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Delete)
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "InternalServerError",
                    message = cause.message ?: "Неизвестная ошибка"
                )
            )
        }
    }

    routing {
        get("/") {
            call.respondText(
                """
                Ollama Chat API Server

                Endpoints:
                - GET  /health       - Проверка состояния сервиса
                - POST /api/chat     - Отправить сообщение
                - POST /api/clear    - Очистить историю чата
                - GET  /api/info     - Информация о сервисе
                """.trimIndent(),
                ContentType.Text.Plain
            )
        }

        get("/health") {
            val ollamaConnected = ollamaService.checkHealth()
            call.respond(
                if (ollamaConnected) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(
                    status = if (ollamaConnected) "healthy" else "unhealthy",
                    ollamaConnected = ollamaConnected
                )
            )
        }

        route("/api") {
            post("/chat") {
                try {
                    val request = call.receive<ApiChatRequest>()
                    val sessionId = call.request.header("X-Session-ID") ?: "default"

                    if (request.message.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(
                                error = "BadRequest",
                                message = "Сообщение не может быть пустым"
                            )
                        )
                        return@post
                    }

                    val response = ollamaService.chat(
                        userMessage = request.message,
                        model = request.model,
                        sessionId = sessionId
                    )

                    call.respond(
                        ApiChatResponse(
                            response = response,
                            model = request.model
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(
                            error = "ChatError",
                            message = e.message ?: "Ошибка при обработке запроса"
                        )
                    )
                }
            }

            post("/clear") {
                val sessionId = call.request.header("X-Session-ID") ?: "default"
                ollamaService.clearHistory(sessionId)
                call.respond(
                    HttpStatusCode.OK,
                    mapOf("message" to "История чата очищена")
                )
            }

            get("/info") {
                call.respond(
                    mapOf(
                        "service" to "Ollama Chat API",
                        "version" to "1.0.0",
                        "ollamaUrl" to (System.getenv("OLLAMA_URL") ?: "http://localhost:11434"),
                        "defaultModel" to (System.getenv("OLLAMA_MODEL") ?: "qwen2.5:1.5b")
                    )
                )
            }
        }
    }

    environment.monitor.subscribe(ApplicationStopped) {
        ollamaService.close()
    }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = System.getenv("HOST") ?: "0.0.0.0"

    embeddedServer(
        Netty,
        port = port,
        host = host,
        module = Application::module
    ).start(wait = true)
}