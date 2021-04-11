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
        setView()
        setButtonClick()

        return binding.root
    }


    fun setView() {
        binding.profileNameTextView.text = CurrentUser.getInstance().name
        binding.profileEmailTextView.text = CurrentUser.getInstance().email
        Glide.with(this)
            .load(CurrentUser.getInstance().photoUrl)
            .into(binding.profilePhotoImageView)
        Log.d("KHJ","provider: ${CurrentUser.getInstance().provider}")
        when (CurrentUser.getInstance().provider) {
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

    private fun setButtonClick() {
        binding.profileMyCardsButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {
                val intent = Intent(requireActivity(),UserCardsActivity::class.java)
                intent.putExtra("type","myCard")
                startActivity(intent)
            }

        binding.profileMyCommentsButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {
                val intent = Intent(requireActivity(),UserCardsActivity::class.java)
                intent.putExtra("type","myCommentCard")
                startActivity(intent)

            }

        binding.profileMyLikesButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {
                val intent = Intent(requireActivity(),UserCardsActivity::class.java)
                intent.putExtra("type","myLikeCard")
                startActivity(intent)

            }

        binding.profileLogoutButton.clicks()
            .observeOn(AndroidSchedulers.mainThread())
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this, FragmentEvent.DESTROY_VIEW)
            .subscribe {
                val logoutDialog = MyAlertDialogFragment("Fragment","로그아웃하시겠습니까?")
                logoutDialog.show(childFragmentManager, "LogoutDialog")

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