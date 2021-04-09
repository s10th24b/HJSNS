package kr.s10th24b.app.hjsns

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.ktx.Firebase
import splitties.toast.toast

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var user = FirebaseAuth.getInstance().currentUser
        FirebaseAuth.getInstance().signOut()
        user = null
        if (user == null) {
            createSignInIntent()
        } else {
//            Log.d("KHJ", user.providerId)
//            Log.d("KHJ", user.providerData[0].providerId)
//            Log.d("KHJ", user.email)
//            val intent = Intent(this, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            //
        }
    }

    private fun createSignInIntent() {
        val providers = listOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.GitHubBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.authentication)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(true)
                .build(),
            MainActivity.RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MainActivity.RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            Log.d("KHJ", response.toString())
//            toast(response.toString())

            if (resultCode == Activity.RESULT_OK) {
                // sign-in success
                val user = FirebaseAuth.getInstance().currentUser
                logUserProviderData(user!!)
                val providerId = user.providerData[1].providerId
                if (providerId == "password") {
                    if (!user.isEmailVerified) {
                        user.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    toast("인증 메일 발송 성공")
                                } else {
                                    toast("인증 메일 발송 실패")
                                }
                            }
                        finish()
                    }
                }
                startMainActivity()
            } else {
                // sign in failed
                toast("Sing-in Failed")
                finish()
            }
        }
    }

    fun logUserProviderData(user: FirebaseUser) {
        for (data in user.providerData) {
            Log.d(
                "KHJ", "id: ${data.providerId}, email: ${data.email}, " +
                        "photo: ${data.photoUrl} displayName: ${data.displayName}"
            )
        }
    }

    fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}