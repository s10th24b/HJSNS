package kr.s10th24b.app.hjsns

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.createBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserInfo
import com.trello.rxlifecycle4.components.support.RxFragment
import kr.s10th24b.app.hjsns.databinding.FragmentCardsBinding
import kr.s10th24b.app.hjsns.databinding.FragmentProfileBinding

class ProfileFragment : RxFragment() {
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
        // Inflate the layout for this fragment
        val curUser = FirebaseAuth.getInstance().currentUser
        if (curUser != null) {
            mUser = curUser
        } else {
            error("mUser error")
        }

        setView(mUser.providerData[1])




        return binding.root
    }

    fun setView(data: UserInfo) {
        binding.profileNameTextView.text = data.displayName ?: "NoName"
        binding.profileEmailTextView.text = data.email ?: "NoEmailName"
        Glide.with(this)
            .load(data.photoUrl ?: R.drawable.github_auth_icon)
            .into(binding.profilePhotoImageView)
        when (data.providerId) {
            "github.com" -> {
                Glide.with(this)
                    .load(R.drawable.github_auth_icon)
                    .centerCrop()
                    .apply{RequestOptions().transform(RoundedCorners(16))}
                    .into(binding.profileProviderImageView)
                binding.profileProviderTextView.text = "GitHub"
            }
            "google.com" -> {
                Glide.with(this)
                    .load(R.drawable.google_auth_icon)
                    .apply{RequestOptions().transform(RoundedCorners(16))}
                    .centerCrop()
                    .into(binding.profileProviderImageView)
                binding.profileProviderTextView.text = "Google"
            }
            "password" -> {
                Glide.with(this)
                    .load(android.R.drawable.ic_dialog_email)
                    .centerCrop()
                    .apply{RequestOptions().transform(RoundedCorners(16))}
                    .into(binding.profileProviderImageView)
                binding.profileProviderTextView.text = "Email"
            }
            else -> {
                error("Error in when data.providerId")
            }
        }
    }
}