package com.example.cloudsecurity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.models.User
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class SplashActivity : AppCompatActivity() {

    private var mDatabase: DatabaseReference? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        FirebaseApp.initializeApp(this)
        //Check current user
        val firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuth.addAuthStateListener(authStateListener)
        mDatabase = FirebaseDatabase.getInstance()
            .getReferenceFromUrl("https://cloud-security-siem-solution-default-rtdb.firebaseio.com/")
    }

    var authStateListener =
        AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            if (firebaseUser == null) {
                val intent = Intent(this@SplashActivity, SignUpActivity::class.java)
                startActivity(intent)
                finish()
            }
            if (firebaseUser != null) {
                mDatabase?.child("users")?.child(firebaseUser.uid)?.get()
                    ?.addOnSuccessListener { it ->
                        val mapper = ObjectMapper() // jackson's objectmapper
                        val pojo = mapper.convertValue(it.value, User::class.java)
                        if (pojo.blocked == true) {
                            val intent =
                                Intent(this@SplashActivity, TemporaryBlockedActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val intent =
                                Intent(this@SplashActivity, HomeActivityForUser::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }?.addOnFailureListener { it ->
                    Toast.makeText(this, "Unable to Fetch Value", Toast.LENGTH_SHORT).show()
                }
            }
        }
}