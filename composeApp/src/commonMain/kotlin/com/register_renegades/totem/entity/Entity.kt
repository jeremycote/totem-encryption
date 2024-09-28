package com.register_renegades.totem.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User (
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String,
) {}

data class File (
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as File

        return id == other.id
    }

    override fun hashCode(): Int {
        return id + name.hashCode()
    }
}