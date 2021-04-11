package kr.s10th24b.app.hjsns

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import splitties.toast.toast
import kotlin.math.log

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var user = FirebaseAuth.getInstance().currentUser
//        FirebaseAuth.getInstance().signOut()
//        user = null
        if (user == null || !user.isEmailVerified) {
            createSignInIntent()
        } else {
            logUserProviderData(user)
            startMainActivity()
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
                .setLogo(R.drawable.app_logo)
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
//            toast(response.toString())

            if (resultCode == Activity.RESULT_OK) {
                // sign-in success
                val curUser = FirebaseAuth.getInstance().currentUser
                val userRef = FirebaseDatabase.getInstance().getReference("Users")
                logUserProviderData(curUser!!)
                val providerId = curUser.providerData[1].providerId
                if (providerId == "password") {
                    if (!curUser.isEmailVerified) {
                        curUser.sendEmailVerification()
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
                userRef.orderByChild("firebaseUid").equalTo(curUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                val newKey = userRef.push()
                                val user = Users()
                                user.name = curUser.displayName ?: "NoName"
                                user.email = curUser.email ?: "NoEmail"
                                val photoUrl = curUser.photoUrl ?: Uri.parse("")
                                user.photoUrl = photoUrl.toString()
                                user.firebaseUid = curUser.uid ?: "NoName"
                                user.provider = providerId
                                user.startTime = ServerValue.TIMESTAMP
                                user.userId = newKey.key.toString()
                                newKey.setValue(user)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            toast("User 생성 성공")
                                        } else {
                                            toast("User 생성 실패")
                                            error("Error in userRef.setValue")
                                        }
                                    }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
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