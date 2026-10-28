package com.vfdeginformatica.mysuperapp.domain.model

/** Result of an image upload: public download [url] and Storage [path] used to delete it later. */
data class UploadedImage(
    val url: String,
    val path: String
)
