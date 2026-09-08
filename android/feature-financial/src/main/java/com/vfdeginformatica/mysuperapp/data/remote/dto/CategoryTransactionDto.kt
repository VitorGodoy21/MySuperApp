package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId

data class CategoryTransactionDto(
    @DocumentId
    var id: String = "",
    var title: String = "",
    var description: String = ""
)
