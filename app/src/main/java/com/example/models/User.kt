package com.example.models

import com.google.firebase.database.IgnoreExtraProperties
import java.io.File

@IgnoreExtraProperties
data class User(
    val userID: String? = null, val username: String? = null, val email: String? = null,
    val role: String? = null, var blocked: Boolean? = null, val screenShot: File? = null,
    var imagePath : String? = null
) {
    // Null default values create a no-argument default constructor, which is needed
    // for deserialization from a DataSnapshot.
}