package com.kgg.android.delivers.loginActivity

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kgg.android.delivers.MainFragment
import java.util.concurrent.TimeUnit

import com.kgg.android.delivers.R
import kotlinx.android.synthetic.main.certification.*

class loginActivity : AppCompatActivity() {
    // [START declare_auth]
    private lateinit var auth: FirebaseAuth
    // [END declare_auth]

    private var storedVerificationId: String? = ""
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        setContentView(R.layout.certification)


        auth = Firebase.auth


        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)

                if (e is FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                } else if (e is FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                storedVerificationId = verificationId
                resendToken = token

                cer_text.setText("인증번호가 전송되었습니다. 인증 코드를 입력해주세요.")
            }
        }



        regist_submit.setOnClickListener {
            if (editTextPhone.length() < 11) {
                Toast.makeText(
                    this, "휴대폰 번호를 정확하게 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val phoneNum = "+82" + editTextPhone.text.toString().substring(1)
                startPhoneNumberVerification( phoneNum)
                Toast.makeText(
                    this, "인증번호가 전송되었습니다.0" + phoneNum,
                    Toast.LENGTH_SHORT
                ).show()
                send.setVisibility(View.INVISIBLE)
                resend.setVisibility(View.VISIBLE)
                regist_submit.setVisibility(View.INVISIBLE)
                regist_resubmit.setVisibility(View.VISIBLE)

            }
        }

        regist_resubmit.setOnClickListener{
            resendVerificationCode("+82" + editTextPhone.text.toString().substring(1), resendToken)
            Toast.makeText(
                this, "인증번호가 재전송되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }

        cer_next_btn.setOnClickListener {
            if (editTextPhone.length() < 11) {
                Toast.makeText(
                    this, "휴대폰 번호를 정확하게 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (editcernum.length() < 6) {
                Toast.makeText(
                    this, "인증번호를 정확하게 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val phoneCredential =
                    PhoneAuthProvider.getCredential(
                        storedVerificationId.toString(),
                        editcernum.text.toString()
                    )
                Log.e(TAG, "storedVerificationId:${storedVerificationId.toString()}")
                Log.e(TAG, "certificationNum:${ editcernum.text.toString()}")
                verifyPhoneNumberWithCode(phoneCredential)
            }
        }


    }

    private fun startPhoneNumberVerification(phoneNumber: String) {
        // [START start_phone_auth]
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        // [END start_phone_auth]
    }

    private fun verifyPhoneNumberWithCode(phoneAuthCredential: PhoneAuthCredential) {

        Firebase.auth.signInWithCredential(phoneAuthCredential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful)  {
                    Log.d(TAG, "signInWithCredential:success")
                    var name = intent.getStringExtra("name")
                    var regNum_front = intent.getStringExtra("regNum_front")
                    var regNum_back = intent.getStringExtra("regNum_back")
                    val intent = Intent(this, MainFragment::class.java)
                    intent.putExtra("name", name.toString())
                    intent.putExtra("regNum_front", regNum_front.toString())
                    intent.putExtra("regNum_back", regNum_back.toString())
                    intent.putExtra("phoneNum", editTextPhone.text.toString())

                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)}

                else {
                    Toast.makeText(this, "올바르지 않은 인증번호입니다.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithCredential:success")

                    val user = task.result?.user
                } else {
                    // Sign in failed, display a message and update the UI
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    if (task.exception is FirebaseAuthInvalidCredentialsException) {
                        // The verification code entered was invalid
                    }
                    // Update UI
                }
            }
    }
    // [END sign_in_with_phone]

    private fun resendVerificationCode(
        phoneNumber: String,
        token: PhoneAuthProvider.ForceResendingToken?
    ) {
        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(90L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this)                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
        if (token != null) {
            optionsBuilder.setForceResendingToken(token) // callback's ForceResendingToken
        }
        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private var lastTimeBackPressed : Long = 0

    override fun onBackPressed() {
        if(System.currentTimeMillis() - lastTimeBackPressed >= 1500) {
            lastTimeBackPressed = System.currentTimeMillis()
            Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
        } else{
            finishAffinity()
        }
    }
}
