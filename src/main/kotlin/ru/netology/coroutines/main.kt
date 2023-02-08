package ru.netology.coroutines

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.coroutines.dto.*
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private val BASE_URL = "127.0.0.1:9999/api/slow "
private val gson = Gson()
private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
    .build()

private suspend fun getPosts(): List<Post> = parseResponse("$BASE_URL/posts", object : TypeToken<List<Post>>() {})
private suspend fun getAuthor(authorId: Long): Author =
    parseResponse("$BASE_URL/authors/$authorId", object : TypeToken<Author>() {})

private suspend fun getComment(postId: Long): List<Comment> =
    parseResponse("$BASE_URL/posts/$postId/comments", object : TypeToken<List<Comment>>() {})

private suspend fun <T> parseResponse(url: String, typeToken: TypeToken<T>): T {
    return withContext(Dispatchers.IO) {
        gson.fromJson(requireNotNull(makeRequest(url).body).string(), typeToken.type)
    }
}

private suspend fun makeRequest(url: String): Response {
    return suspendCoroutine { continuation ->
        Request.Builder()
            .url(url)
            .build()
            .let { request ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response)
                    }

                })
            }
    }
}

fun main() {
    CoroutineScope(EmptyCoroutineContext).launch {
        getPosts().map { post ->
            async {
                PostWithCommentsAndAuthor(post, getAuthor(post.authorId), getComment(post.id).map { comment ->
                    CommentWithAuthor(comment, getAuthor(comment.authorId))
                })
            }
        }.awaitAll()
    }

    Thread.sleep(1000L)
}