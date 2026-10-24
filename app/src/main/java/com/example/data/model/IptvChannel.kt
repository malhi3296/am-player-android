package com.example.data.model

data class IptvChannel(
    val id: String,
    val name: String,
    val category: String,
    val streamUrl: String,
    val logoUrl: String = "",
    val country: String = "Global",
    val isCustom: Boolean = false
)
