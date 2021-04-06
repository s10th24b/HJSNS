package kr.s10th24b.app.hjsns

import android.annotation.SuppressLint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.jakewharton.rxbinding4.view.clicks
import com.squareup.haha.perflib.Snapshot
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kr.s10th24b.app.hjsns.databinding.ActivityWriteBinding
import kr.s10th24b.app.hjsns.databinding.CardBackgroundBinding
import splitties.toast.toast
import java.util.concurrent.TimeUnit

class WriteActivity : AppCompatActivity() {
    lateinit var binding: ActivityWriteBinding
    var commentPostId = ""
    var mCompositeDisposable = CompositeDisposable()
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
        val mode = intent.getStringExtra("mode")
        commentPostId = intent.getStringExtra("postId") ?: ""

        val recyclerViewAdapter = CardBackgroundRecyclerViewAdapter()
        recyclerViewAdapter.cardBackgroundList = bgList
        binding.writeRecyclerView.adapter = recyclerViewAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.writeRecyclerView.layoutManager = layoutManager

        mCompositeDisposable.add(
            binding.writeShareButton.clicks()
                .debounce(200L, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (binding.writeEditText.text.isNotBlank()) {
                        when (mode) {
                            "post" -> {
                                val post = Post()
                                val newRef =
                                    FirebaseDatabase.getInstance().getReference("Posts").push()
                                post.writeTime = ServerValue.TIMESTAMP
                                post.bgUri = bgList[currentBgPosition]
                                post.message = binding.writeEditText.text.toString()
                                post.writerId = getMyId()
                                post.postId = newRef.key.toString()
                                newRef.setValue(post)
                                Log.d("KHJ", "Adding Post ${post.message}")
//                                toast("카드 작성 성공")
                                finish()
                            }
                            "comment" -> {
                                val comment = Comment()
                                val newRef =
                                    FirebaseDatabase.getInstance()
                                        .getReference("Comments/$commentPostId").push()
                                comment.writeTime = ServerValue.TIMESTAMP
                                comment.bgUri = bgList[currentBgPosition]
                                comment.message = binding.writeEditText.text.toString()
                                comment.writerId = getMyId()
                                comment.postId = commentPostId
                                comment.commentId = newRef.key.toString()
                                newRef.setValue(comment)
                                Log.d("KHJ", "Adding Comment ${comment.message}")
                                //// You should write below code in writing part, not viewing activity
                                // here, right.
                                val postCommentCountRef =
                                    FirebaseDatabase.getInstance()
                                        .getReference("Posts/$commentPostId")
                                        .child("commentCount")
                                postCommentCountRef.get().addOnSuccessListener(this) {
                                    postCommentCountRef.setValue(it.value.toString().toInt() + 1)
                                }.addOnCanceledListener(this) {
                                    Log.d("KHJ", "Error getting data from $commentPostId")
                                }
//                                toast("댓글 작성 성공")
                                finish()
                            }
                            else -> {
                                error("error in writeShareButton")
                            }
                        }
                    } else {
                        toast("내용을 작성해주세요")
                    }
                }, {
                    Log.d("KHJ", "${it.toString()}")
                    it.printStackTrace()

                })
        )
    }

    @SuppressLint("HardwareIds")
    fun getMyId(): String { // Return Device ID
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onDestroy() {
        mCompositeDisposable.dispose()
        super.onDestroy()
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