package com.equinos.profile

import android.net.Uri

enum class Role {
    USER, ADVANCED_USER;

    override fun toString(): String {
        return this.name
    }
}

data class User(
    val userId: Long,
    val username: String,
    val email: String,
    var image: Uri?,
    val role: Role,
    val accessToken: String
)
