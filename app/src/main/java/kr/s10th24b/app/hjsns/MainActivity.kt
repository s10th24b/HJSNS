package kr.s10th24b.app.hjsns

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TimeUtils
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.haha.perflib.Main
import com.squareup.leakcanary.LeakCanary
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kr.s10th24b.app.hjsns.databinding.ActivityMainBinding
import splitties.intents.receiverSpec
import splitties.toast.toast
import java.util.concurrent.TimeUnit

class MainActivity : RxAppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    companion object {
        const val RC_SIGN_IN = 0
    }

    private val cardsFragment by lazy { CardsFragment() }
    private val profileFragment by lazy { ProfileFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainFloatingActionButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("mode", "post")
            startActivity(intent)
        }
        binding.mainBottomBavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.cardPage -> {
//                    var badge = binding.mainBottomNavigationView.getOrCreateBadge(item.itemId)
//                    badge.isVisible = false
//                    badge.isVisible = true
//                    badge.number = 99
                    binding.mainFloatingActionButton.visibility = View.VISIBLE
                    changeFragment(cardsFragment)
                    true
                }
                R.id.profilePage -> {
//                    var badge = binding.mainBottomNavigationView.getOrCreateBadge(item.itemId)
//                    badge.isVisible = false
//                    changeFragment(profileFragment)
                    binding.mainFloatingActionButton.visibility = View.GONE
                    changeFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
        binding.mainBottomBavigationView.selectedItemId = R.id.cardPage
    }

    override fun onDestroy() {
        if (isFinishing) {
        } else {

        }
        super.onDestroy()
    }

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFrame, fragment)
            .commit()
    }
}