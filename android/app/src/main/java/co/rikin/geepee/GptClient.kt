package co.rikin.geepee

import android.util.Log
import com.squareup.moshi.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

object GptClient {

  private val client = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor)
    .readTimeout(2, TimeUnit.MINUTES)
    .build()

  private val retrofit = Retrofit.Builder()
    .baseUrl(BASE_URL)
    .client(client)
    .addConverterFactory(MoshiConverterFactory.create())
    .build()

  val service = retrofit.create<GptService>()
}

object AuthInterceptor : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    val originalRequest = chain.request()
    val newRequest = originalRequest.newBuilder()
      .header("Content-Type", "application/json")
      .header("Authorization", "Bearer ${BuildConfig.API_KEY}")
      .build()
    Log.i("OkHttpRequest", newRequest.toString())
    return chain.proceed(newRequest)
  }
}

interface GptService {
  @POST("chat/completions")
  suspend fun chat(@Body body: ChatRequest): ChatResponse
}

data class ChatRequest(
  @Json(name="model")
  val model: String = "gpt-4",
  @Json(name="temperature")
  val temperature: Float = 0.7f,
  @Json(name="messages")
  val messages: List<ChatMessage>
)

data class PromptData(
  @Json(name="command")
  val command: String
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

private const val BASE_URL = "https://api.openai.com/v1/"