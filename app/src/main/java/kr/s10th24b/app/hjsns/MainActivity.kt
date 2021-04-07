package kr.s10th24b.app.hjsns

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TimeUtils
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.leakcanary.LeakCanary
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.operators.observable.ObservableInterval
import io.reactivex.rxjava3.subjects.PublishSubject
import kr.s10th24b.app.hjsns.databinding.ActivityMainBinding
import splitties.toast.toast
import java.util.concurrent.TimeUnit

class MainActivity : RxAppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private val mCompositeDisposable = CompositeDisposable()
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
//                    var badge = binding.mainBottomBavigationView.getOrCreateBadge(item.itemId)
//                    badge.isVisible = false
//                    badge.isVisible = true
//                    badge.number = 99
                    changeFragment(cardsFragment)
                    mCompositeDisposable.add(cardsFragment.mCompositeDisposable)
                    true
                }
                R.id.profilePage -> {
//                    var badge = binding.mainBottomBavigationView.getOrCreateBadge(item.itemId)
//                    badge.isVisible = false
//                    changeFragment(profileFragment)
                    changeFragment(profileFragment)
                    mCompositeDisposable.add(cardsFragment.mCompositeDisposable)
                    true
                }
                else -> false
            }
        }
        binding.mainBottomBavigationView.selectedItemId = R.id.cardPage
    }

    override fun onDestroy() {
        if (isFinishing) {
            mCompositeDisposable.dispose()
            Log.d("KHJ", "mCompositeDisposable disposed!")
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

