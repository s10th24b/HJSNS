package kr.s10th24b.app.hjsns

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kr.s10th24b.app.hjsns.databinding.ActivityWriteBinding
import kr.s10th24b.app.hjsns.databinding.CardBackgroundBinding
import splitties.toast.toast

class WriteActivity : AppCompatActivity() {
    lateinit var binding: ActivityWriteBinding
    var currentBgPosition = 0
    val bgList = mutableListOf(
        "android.resource://kr.s10th24b.app.hjsns/drawable/default_bg",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg2",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg3",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg4",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg5",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg6",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg7",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg8",
        "android.resource://kr.s10th24b.app.hjsns/drawable/bg9"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recyclerViewAdapter = CardBackgroundRecyclerViewAdapter()
        recyclerViewAdapter.cardBackgroundList = bgList
        binding.writeRecyclerView.adapter = recyclerViewAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.writeRecyclerView.layoutManager = layoutManager

        binding.writeShareButton.setOnClickListener {
            if (binding.writeEditText.text.isNotBlank()) {
                val post = Post()
                val newRef = FirebaseDatabase.getInstance().getReference("Posts").push()
                post.writeTime = ServerValue.TIMESTAMP
                post.bgUri = bgList[currentBgPosition]
                post.message = binding.writeEditText.text.toString()
                post.writerId = getMyId()
                post.postId = newRef.key.toString()
                newRef.setValue(post)
                toast("공유되었습니다.")
                finish()
            } else {
                toast("내용을 작성해주세요!")
                return@setOnClickListener
            }
        }
    }

    fun getMyId(): String{ // Return Device ID
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    inner class CardBackgroundRecyclerViewAdapter :
        RecyclerView.Adapter<CardBackgroundRecyclerViewHolder>() {
        var cardBackgroundList = mutableListOf<String>()
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CardBackgroundRecyclerViewHolder {
            return CardBackgroundRecyclerViewHolder(
                LayoutInflater.from(this@WriteActivity)
                    .inflate(R.layout.card_background, parent, false)
            )
        }

        override fun onBindViewHolder(holder: CardBackgroundRecyclerViewHolder, position: Int) {
            var cardBackground = cardBackgroundList[position]
            Glide.with(applicationContext)
                .load(Uri.parse(cardBackground))
                .centerCrop()
                .into(holder.imageView)
            holder.itemView.setOnClickListener {
                Glide.with(applicationContext)
                    .load(Uri.parse(cardBackground))
                    .centerCrop()
                    .into(binding.writeImageView)
                currentBgPosition = position
            }
        }

        override fun getItemCount() = cardBackgroundList.size
    }

    inner class CardBackgroundRecyclerViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var binding = CardBackgroundBinding.bind(itemView)
        var imageView = binding.imageView
    }
}