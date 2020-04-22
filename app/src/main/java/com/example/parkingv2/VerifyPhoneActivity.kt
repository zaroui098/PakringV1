package com.example.parkingv2

import android.content.Intent
import android.os.Bundle


import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.TaskExecutors
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.activity_verify_phone.*


class VerifyPhoneActivity  : AppCompatActivity()  {
    //These are the objects needed
    //It is the verification id that will be sent to the user
    private var mVerificationId: String? = null

    //The edittext to input the code
    private var editTextCode: EditText? = null

    //firebase auth object
    private var mAuth: FirebaseAuth? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_phone)

        //initializing objects
        mAuth = FirebaseAuth.getInstance()
        editTextCode = findViewById(R.id.edCode)


        //getting mobile number from the previous activity
        //and sending the verification code to the number
        val intent = intent
        val mobile = intent.getStringExtra("mobile")
        sendVerificationCode(mobile)


        //if the automatic sms detection did not work, user can also enter the code manually
        //so adding a click listener to the button
        findViewById<View>(R.id.buttonSignIn).setOnClickListener(View.OnClickListener {
            val code = edCode.getText().toString().trim { it <= ' ' }
            if (code.isEmpty() || code.length < 6) {
                edCode.setError("Enter valid code")
                edCode.requestFocus()
                return@OnClickListener
            }

            //verifying the code entered manually
            verifyVerificationCode(code)
        })
    }

    //the method is sending verification code
    //the country id is concatenated
    //you can take the country id as user input as well
    private fun sendVerificationCode(mobile: String) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            "+216$mobile",
            60,
            TimeUnit.SECONDS,
            TaskExecutors.MAIN_THREAD,
            mCallbacks
        )
    }

    //the callback to detect the verification status
    private val mCallbacks: OnVerificationStateChangedCallbacks =
        object : OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(phoneAuthCredential: PhoneAuthCredential) {

                //Getting the code sent by SMS
                val code = phoneAuthCredential.smsCode

                //sometime the code is not detected automatically
                //in this case the code will be null
                //so user has to manually enter the code
                if (code != null) {
                    editTextCode!!.setText(code)
                    //verifying the code
                    verifyVerificationCode(code)
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@VerifyPhoneActivity, e.message, Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(
                s: String,
                forceResendingToken: ForceResendingToken
            ) {
                super.onCodeSent(s, forceResendingToken)

                //storing the verification id that is sent to the user
                mVerificationId = s
            }
        }

    private fun verifyVerificationCode(code: String) {
        //creating the credential
        val credential = PhoneAuthProvider.getCredential(mVerificationId!!, code)

        //signing the user
        signInWithPhoneAuthCredential(credential)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        mAuth!!.signInWithCredential(credential)
            .addOnCompleteListener(
                this@VerifyPhoneActivity
            ) { task ->
                if (task.isSuccessful) {
                    //verification successful we will start the profile activity
                    val intent =
                        Intent(this@VerifyPhoneActivity, ReservationActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {

                    //verification unsuccessful.. display an error message
                    var message =
                        "Somthing is wrong, we will fix it soon..."
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        message = "Invalid code entered..."
                    }
                    val snackbar: Snackbar = Snackbar.make(
                        findViewById<View>(R.id.parent),
                        message,
                        Snackbar.LENGTH_LONG
                    )
                    snackbar.setAction(
                        "Dismiss",
                        View.OnClickListener { })
                    snackbar.show()
                }
            }
    }
}