package com.example.cloudsecurity

import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import androidx.appcompat.app.AppCompatActivity
import com.example.Utils.GenericUtils.Companion.OUTSIDE_OFFICE_PREMISES
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_temporary_blocked.*
import kotlinx.android.synthetic.main.logout_common_view.*

class TemporaryBlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temporary_blocked)

        val intent = intent
        val data = intent.getStringExtra("TAG")
        if (data.equals(OUTSIDE_OFFICE_PREMISES)) {
            blocked.text = resources.getText(R.string.blocked_when_user_of_location)
            btn_log_out.visibility = GONE
            separator.visibility = GONE
            return
        }

        init()
        separator.setBackgroundColor(resources.getColor(R.color.white))
        btn_log_out.setTextColor(resources.getColor(R.color.white))


    }

    private fun init() {
        btn_log_out.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}