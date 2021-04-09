package kr.s10th24b.app.hjsns

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import com.google.firebase.ktx.Firebase
import com.jakewharton.rxbinding4.view.clicks
import com.trello.rxlifecycle4.android.FragmentEvent
import com.trello.rxlifecycle4.components.support.RxDialogFragment
import com.trello.rxlifecycle4.components.support.RxFragment
import com.trello.rxlifecycle4.kotlin.bindUntilEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kr.s10th24b.app.hjsns.databinding.FragmentProfileBinding
import splitties.toast.toast
import splitties.snackbar.snack
import splitties.systemservices.appWidgetManager
import java.util.concurrent.TimeUnit
import kotlin.math.log

class ProfileFragment : RxFragment(), MyAlertDialogFragment.MyAlertDialogListener {
    lateinit var binding: FragmentProfileBinding
    lateinit var mUser: FirebaseUser
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(layoutInflater)
        applyUserInfo()
        setView()
        setButtonClick()




        return binding.root
    }

    fun applyUserInfo() {
        val curUser = FirebaseAuth.getInstance().currentUser
        if (curUser != null) {
            mUser = curUser
        } else {
            error("mUser error")
        }
        Log.d("KHJ", mUser.toString())
    }

    fun setView() {
        val data = mUser.providerData[1]
        binding.profileNameTextView.text = data.displayName.let {
            if (it.isNullOrBlank()) "NoName" else it
        }
        binding.profileEmailTextView.text = data.email ?: "NoEmailName"
        Glide.with(this)
            .load(data.photoUrl ?: R.drawable.github_auth_icon)
            .into(binding.profilePhotoImageView)
        when (data.providerId) {
            "github.com" -> {
                Glide.with(this)
                    .load(R.drawable.github_auth_icon)
                    .centerCrop()
                    .apply { RequestOptions().transform(RoundedCorners(16)) }
                    .into(binding.profileProviderImageView)
                binding.profileProviderTextView.text = "GitHub Account"
            }
            "google.com" -> {
                Glide.with(this)
                    .load(R.drawable.google_auth_icon)
                    .apply { RequestOptions().transform(RoundedCorners(16)) }
                    .centerCrop()
                    .into(binding.profileProviderImageView)
                binding.profileProviderTextView.text = "Google Account"
            }
            "password" -> {
                Glide.with(this)
                    .load(android.R.drawable.ic_dialog_email)
                    .centerCrop()
                    .apply { RequestOptions().transform(RoundedCorners(16)) }
                    .into(binding.profileProviderImageView)
                binding.profileProviderTextView.text = "Email Account"
            }
            else -> {
                error("Error in when data.providerId")
            }
        }
    }

    fun setButtonClick() {
        binding.profileMyCardsButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {

            }

        binding.profileMyCommentsButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {

            }

        binding.profileMyLikesButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {

            }

        binding.profileLogoutButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {
                val logoutDialog = MyAlertDialogFragment("Fragment","로그아웃하시겠습니까?")
                logoutDialog.show(childFragmentManager, "LogoutDialog")
//                logoutDialog.show(parentFragmentManager, "LogoutDialog")

            }
    }

    override fun onPositiveClick(dialog: RxDialogFragment): Boolean {
//        FirebaseAuth.getInstance().signOut()
        AuthUI.getInstance().signOut(requireContext())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
//                            Snackbar.make(requireView(),"Successfully Signed-Out",Snackbar.LENGTH_SHORT).show()
                    toast("Successfully Signed-Out")
//                            requireActivity().finish()
                    startLoginActivity()
                } else {
                    Snackbar.make(
                        requireView(),
                        "Error Occurred in Signing-out",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
        return true
    }

    override fun onNegativeClick(dialog: RxDialogFragment): Boolean {
        // do nothing
        return false
    }

    fun startLoginActivity() {
        val intent = Intent(requireContext().applicationContext, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}