package ru.netology.coroutines.dto

data class PostWithCommentsAndAuthor(
    val post: Post,
    val author: Author,
    val comments: List<CommentWithAuthor>,
)
