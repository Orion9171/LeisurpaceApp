package com.example.leisurepace

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.leisurepace.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize binding and Firebase Auth
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (firebaseAuth.currentUser != null) {
            Toast.makeText(this, "Already logged in as ${firebaseAuth.currentUser?.email}", Toast.LENGTH_SHORT).show()
            binding.Logout.isEnabled = true
        } else {
            binding.Logout.isEnabled = false
        }

        // Sign-up logic
        binding.Signup.setOnClickListener {
            val email = binding.Email.text.toString()
            val pass = binding.password.text.toString()
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Sign up successful!", Toast.LENGTH_SHORT).show()
                        navigateToMainActivity()
                    } else {
                        handleSignupError(task.exception)
                    }
                }
            } else {
                Toast.makeText(this, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
            }
        }

        // Login logic
        binding.Login.setOnClickListener {
            val email = binding.Email.text.toString()
            val pass = binding.password.text.toString()

            Log.d("LoginActivity", "Attempting login with Email: $email")
            if (email.isNotEmpty() && pass.isNotEmpty()) {
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        if (firebaseAuth.currentUser != null) {
                            Log.d("LoginActivity", "User authenticated: ${firebaseAuth.currentUser?.email}")
                            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                            navigateToMainActivity()
                        } else {
                            Log.d("LoginActivity", "Authentication failed: User is null")
                            Toast.makeText(this, "Authentication failed!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        handleLoginError(task.exception)
                    }
                }
            } else {
                Toast.makeText(this, "Empty fields are not allowed", Toast.LENGTH_SHORT).show()
            }
        }

        // Logout logic
        binding.Logout.setOnClickListener {
            firebaseAuth.signOut()
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

            binding.Logout.isEnabled = false
            binding.Email.text.clear()
            binding.password.text.clear()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun handleSignupError(exception: Exception?) {
        try {
            throw exception ?: Exception("Sign up failed")
        } catch (e: FirebaseAuthUserCollisionException) {
            Toast.makeText(this, "Email already in use.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("SignupError", "Error during sign-up", e)
        }
    }

    private fun handleLoginError(exception: Exception?) {
        try {
            throw exception ?: Exception("Login failed")
        } catch (e: FirebaseAuthInvalidUserException) {
            Toast.makeText(this, "No account with this email.", Toast.LENGTH_SHORT).show()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("LoginError", "Error during login", e)
        }
    }
}
