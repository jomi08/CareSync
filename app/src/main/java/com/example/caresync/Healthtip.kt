package com.example.caresync

// Wikipedia API response structure
data class WikiResponse(
    val query: WikiQuery?
)

data class WikiQuery(
    val pages: Map<String, WikiPage>?
)

data class WikiPage(
    val title: String,
    val extract: String?,
    val fullurl: String?
)

// our own clean data class for displaying
data class HealthItem(
    val Title: String,
    val MyHFDescription: String,
    val AccessibleVersion: String
)