package co.rikin.geepee

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.POST


interface GptService {
  @POST("chat/completions")
  suspend fun chat(@Body body: ChatRequest): ChatResponse
}

data class ChatRequest(
  @Json(name="model")
  val model: String = "gpt-3.5-turbo",
  @Json(name="messages")
  val messages: List<ChatMessage>
)

data class ChatMessage(
  @field:Json(name="role")
  val role: String,
  @field:Json(name="content")
  val content: String
)

data class ChatResponse(
  @field:Json(name="id")
  val id: String,
  @field:Json(name="model")
  val model: String,
  @field:Json(name="choices")
  val choices: List<Choice>
)

data class Choice(
  @field:Json(name="message")
  val message: ChatMessage,
  @field:Json(name="finish_reason")
  val finishReason: String,
  @field:Json(name="index")
  val index: Int
)