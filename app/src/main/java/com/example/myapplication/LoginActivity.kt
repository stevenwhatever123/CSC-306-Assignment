package com.example.myapplication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * A Login class for user login and signup
 */
class LoginActivity : AppCompatActivity() {

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Firebase database reference
    private val database = Firebase.database.reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Get the current user
        val user = mAuth.currentUser

        val contextView = findViewById<View>(R.id.login_view)
        val emailText = findViewById<AppCompatEditText>(R.id.input_email)
        val passText = findViewById<AppCompatEditText>(R.id.input_password)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // If there is a user current, we sign in the user automatically and start the MainActivity
        if(user != null){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Handler for login button
        btnLogin.setOnClickListener{view->
            view.hideKeyboard()
            mAuth.signInWithEmailAndPassword(
                    emailText.text.toString(),
                    passText.text.toString()
            )
                    .addOnCompleteListener(this){task->
                        /*
                        If the user successfully login, update the UI and change to MainActivity
                        Otherwise, we show a snackbar notice the user that login failed
                         */
                        if(task.isSuccessful){
                            emailText.text = null
                            passText.text = null
                            val user = mAuth.currentUser
                            updateUI(user)

                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Snackbar.make(contextView, "Login Failed", Snackbar.LENGTH_SHORT)
                                    .setAction("UNDO"){
                                        Log.e("TAG", "Done");
                                    }
                                    .show()
                            updateUI(null)
                        }
                    }
        }

        // Handler for sign up button
        btnSignUp.setOnClickListener{view->
            view.hideKeyboard()
            mAuth.createUserWithEmailAndPassword(
                    emailText.text.toString(),
                    passText.text.toString()
            )
                    .addOnCompleteListener(this){task->
                        /*
                        If the user successfully signed up
                        Update the UI and create a new user in the firebase
                        Otherwise, we show a snackbar noticing the user whether
                        the account has already been created or failed
                         */
                        if(task.isSuccessful){
                            //textView.text = "createUserWithEmail: Success"

                            val user = mAuth.currentUser

                            var nameList : List<String> = listOf()

                            database.child("users").child(user!!.uid).child("Follow_list").setValue(nameList)

                            updateUI(user)
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Snackbar.make(contextView, "Account Already Created", Snackbar.LENGTH_SHORT)
                                    .setAction("UNDO"){
                                        Log.e("TAG", "Done");
                                    }
                                    .show()
                            updateUI(null)
                        }
                    }
        }

        btnLogout.setOnClickListener{view->
            mAuth.signOut()
            //textView.text = "Signed Out"
            updateUI(null)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    /**
     * This method updates the UI according if there is a user
     * @param user a firebase user
     */
    private fun updateUI(user: FirebaseUser?){
        if(user != null){

        } else {
            mAuth.signOut()
        }
    }

    /**
     * This method hided the keyboard
     */
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }
}