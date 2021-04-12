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
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
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
            FirebaseDatabase.getInstance().getReference("Users").orderByChild("firebaseUid")
                .equalTo(user.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            if (snapshot.childrenCount > 1) error("Error in user children count!")
                            val ss = snapshot.children.first()
                            Log.d("KHJ","snapshot.key: ${snapshot.key}")
                            Log.d("KHJ","snapshot.value: ${snapshot.value}")
                            val ssUser = ss.getValue(Users::class.java) as Users
                            Log.d("KHJ",ssUser.printToString())
                            ssUser.let {
                                CurrentUser.setInstance(ssUser)
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        toast("token: ${task.result}")
                                        Log.d("KHJ",task.result.toString())
                                        startMainActivity()
                                    } else {
                                        error("Error in getting token")
                                    }
                                }
                            }
                        } else {
                            FirebaseAuth.getInstance().signOut()
                            createSignInIntent()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })
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
                        return
                    }
                }
                userRef.orderByChild("firebaseUid").equalTo(curUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (!snapshot.exists()) {
                                val newRef = userRef.push()
                                val user = Users()
                                setUser(user, curUser, newRef.key.toString())
                                CurrentUser.setInstance(user)
                                newRef.setValue(user)
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            toast("User 생성 성공")
                                            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    toast("token: ${task.result}")
                                                    Log.d("KHJ","token: "+task.result.toString())
                                                } else {
                                                    error("Error in getting token")
                                                }

                                            }
                                            startMainActivity()
                                        } else {
                                            toast("User 생성 실패")
                                            error("Error in userRef.setValue")
                                        }
                                    }
                            }
                            else {
                                Log.d("KHJ","Already User Exist!")
                                FirebaseDatabase.getInstance().getReference("Users").orderByChild("firebaseUid")
                                    .equalTo(curUser.uid).addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            if (snapshot.exists()) {
                                                val ss = snapshot.children.first()
                                                val ssUser = ss.getValue(Users::class.java) as Users
                                                Log.d("KHJ","snapshot.key: ${snapshot.key}")
                                                Log.d("KHJ","snapshot.value: ${snapshot.value}")
                                                Log.d("KHJ",ssUser.printToString())
                                                ssUser.let {
                                                    CurrentUser.setInstance(ssUser)
                                                    startMainActivity()
                                                }
                                            } else {
                                                finish()
                                                error("Error in LoginActivity!")
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                        }
                                    })
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
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

    fun setUser(user: Users, curUser: FirebaseUser, key: String) {
        user.name = curUser.displayName.let {
            if (it.isNullOrBlank()) "NoName"
            else it
        }
        user.email = curUser.email.let {
            if (it.isNullOrBlank()) "NoEmail"
            else it
        }
        val photoUrl = curUser.photoUrl ?: Uri.parse("")
        user.photoUrl = photoUrl.toString()
        user.firebaseUid = curUser.uid
        user.provider = curUser.providerData[1].providerId
        Log.d("KHJ","in setUser, provider: ${user.provider}")
        user.startTime = ServerValue.TIMESTAMP
        user.userId = key
    }
}