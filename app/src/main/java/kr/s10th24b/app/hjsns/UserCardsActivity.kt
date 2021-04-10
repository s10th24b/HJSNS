package kr.s10th24b.app.hjsns

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.Fragment
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import kr.s10th24b.app.hjsns.databinding.ActivityUserCardsBinding

class UserCardsActivity : RxAppCompatActivity() {
    lateinit var binding: ActivityUserCardsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserCardsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val type = intent.getStringExtra("type")
        when (type) {
            "myCard" -> changeFragment(CardsFragment(type))
            "myCommentCard" -> changeFragment(UserCardFragment(type))
            "myLikeCard" -> changeFragment(UserCardFragment(type))
            else -> {}
        }
    }

    fun changeFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.userCardsFrame, fragment)
            .commit()
    }
}