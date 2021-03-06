package com.soilmoisturesensor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

/**
 * Main activity when the app opens
 * @author Ehsan kabir
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance()

        signin.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this, SignIn::class.java))
        })

        signup.setOnClickListener(View.OnClickListener {
            register()
        })


    }


    private fun register() {
        startActivity(Intent(this, SignUp::class.java))
    }
}
