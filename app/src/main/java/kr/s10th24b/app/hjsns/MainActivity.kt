package kr.s10th24b.app.hjsns

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.leakcanary.LeakCanary
import kr.s10th24b.app.hjsns.databinding.ActivityMainBinding
import splitties.toast.toast

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    private val cardsFragment by lazy { CardsFragment() }
    private val profileFragment by lazy { ProfileFragment() }
    val ref = FirebaseDatabase.getInstance().getReference("test")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.mainFloatingActionButton.setOnClickListener {
            val intent = Intent(this,WriteActivity::class.java)
            startActivity(intent)
        }

        ref.addValueEventListener(object:ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val message = snapshot.value.toString()
                Log.d("KHJ",message)
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
        binding.mainBottomBavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.cardPage -> {
                    var badge = binding.mainBottomBavigationView.getOrCreateBadge(item.itemId)
//                    badge.isVisible = true
//                    badge.number = 99
                    changeFragment(cardsFragment)
                    true
                }
                R.id.profilePage -> {
                    var badge = binding.mainBottomBavigationView.getOrCreateBadge(item.itemId)
//                    badge.isVisible = true
//                    changeFragment(profileFragment)
                    true
                }
                else -> false
            }
        }
    }
    fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFrame, fragment)
            .commit()
    }
}