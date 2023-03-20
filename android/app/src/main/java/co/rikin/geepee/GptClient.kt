package co.rikin.geepee

import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

object GptClient {

  private val client = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor)
    .readTimeout(60, TimeUnit.SECONDS)
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

const val BASE_URL = "https://api.openai.com/v1/"