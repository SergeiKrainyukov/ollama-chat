import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

class OllamaClient(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "qwen3:4b"
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 120000  // 2 минуты
            connectTimeoutMillis = 30000   // 30 секунд
            socketTimeoutMillis = 120000   // 2 минуты
        }
    }

    private val conversationHistory = mutableListOf<ChatMessage>()

    suspend fun chat(userMessage: String): String {
        conversationHistory.add(ChatMessage(role = "user", content = userMessage))

        val request = ChatRequest(
            model = model,
            messages = conversationHistory,
            stream = false
        )

        return try {
            val response: HttpResponse = client.post("$baseUrl/api/chat") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val responseText = response.bodyAsText()
            val lines = responseText.trim().lines().filter { it.isNotBlank() }

            // Собираем весь текст из thinking из всех строк до done=true
            val fullThinking = StringBuilder()

            for (line in lines) {
                try {
                    val resp = json.decodeFromString<ChatResponse>(line)
                    // Собираем thinking до тех пор пока не встретим done=true
                    if (!resp.done) {
                        resp.message.thinking?.let { fullThinking.append(it) }
                    }
                } catch (_: Exception) {
                    // Игнорируем ошибки парсинга
                }
            }

            val actualContent = fullThinking.toString().ifBlank {
                "Нет ответа от модели"
            }

            // Сохраняем в историю
            conversationHistory.add(ChatMessage(role = "assistant", content = actualContent))

            actualContent
        } catch (e: Exception) {
            e.printStackTrace()
            conversationHistory.removeLast() // Удаляем сообщение пользователя при ошибке
            "Ошибка: ${e.message}"
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun close() {
        client.close()
    }
}

fun main() = runBlocking {
    println("╔════════════════════════════════════════════╗")
    println("║     Ollama Chat Client (qwen3:4b)          ║")
    println("╠════════════════════════════════════════════╣")
    println("║  Команды:                                  ║")
    println("║    /clear - очистить историю чата          ║")
    println("║    /exit  - выход                          ║")
    println("╚════════════════════════════════════════════╝")
    println()

    val client = OllamaClient()

    try {
        while (true) {
            print("Вы: ")
            val input = readlnOrNull()?.trim() ?: break

            when {
                input.isEmpty() -> continue
                input == "/exit" -> {
                    println("До свидания!")
                    break
                }
                input == "/clear" -> {
                    client.clearHistory()
                    println("История чата очищена.\n")
                    continue
                }
            }

            print("AI: ")
            val response = client.chat(input)
            println(response)
            println()
        }
    } finally {
        client.close()
    }
}
