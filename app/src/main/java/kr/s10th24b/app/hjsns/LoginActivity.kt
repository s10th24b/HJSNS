package kr.s10th24b.app.hjsns

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import splitties.toast.toast

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val user = FirebaseAuth.getInstance().currentUser
//            FirebaseAuth.getInstance().signOut()
//            user.delete()
        if (user == null) {
            createSignInIntent()
        } else {
            Log.d("KHJ", user.providerId)
            Log.d("KHJ", user.email)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            //
        }
    }

    private fun createSignInIntent() {
        val providers = listOf(
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.GitHubBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.authentication)
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .build(),
            MainActivity.RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MainActivity.RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            Log.d("KHJ", response.toString())
            toast(response.toString())

            if (resultCode == Activity.RESULT_OK) {
                // sign-in success
                val user = FirebaseAuth.getInstance().currentUser
                Log.d("KHJ", user.providerId)
                Log.d("KHJ", user.email)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else {
                // sign in failed
                finish()
            }
        }
    }
}