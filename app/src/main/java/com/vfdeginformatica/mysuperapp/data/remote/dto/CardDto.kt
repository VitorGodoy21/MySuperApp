package com.vfdeginformatica.mysuperapp.data.remote.dto

import com.google.firebase.firestore.DocumentId

data class CardDto(
    @DocumentId
    var id: String = "",
    var bank: String = "",
    var finalCode: String = "",
    var paymentMethod: String = "",
    var invoiceDueDate: Int = 0,
)