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
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kr.s10th24b.app.hjsns.databinding.ActivityWriteBinding
import kr.s10th24b.app.hjsns.databinding.CardBackgroundBinding
import splitties.toast.toast
import java.util.concurrent.TimeUnit

class WriteActivity : RxAppCompatActivity() {
    lateinit var binding: ActivityWriteBinding
    var intentPostId = ""
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
        val mode = intent.getStringExtra("mode") ?: ""
        val tempModifyCard = intent.getSerializableExtra("post") ?: Post()
        val modifyCard = tempModifyCard as Post
        intentPostId = intent.getStringExtra("postId") ?: ""

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
                                    .addOnSuccessListener(this) { toast("카드 작성 성공") }
                                    .addOnCanceledListener(this) { toast("카드 작성 취소") }
                                    .addOnFailureListener(this) { toast("카드 작성 실패") }
                                Log.d("KHJ", "Adding Post ${post.message}")
//                                toast("카드 작성 성공")
                                finish()
                            }
                            "comment" -> {
                                val comment = Comment()
                                val newRef =
                                    FirebaseDatabase.getInstance()
                                        .getReference("Comments/$intentPostId").push()
                                comment.writeTime = ServerValue.TIMESTAMP
                                comment.bgUri = bgList[currentBgPosition]
                                comment.message = binding.writeEditText.text.toString()
                                comment.writerId = getMyId()
                                comment.postId = intentPostId
                                comment.commentId = newRef.key.toString()
                                newRef.setValue(comment)
                                Log.d("KHJ", "Adding Comment ${comment.message}")
                                //// You should write below code in writing part, not viewing activity
                                // here, right.
                                val postCommentCountRef =
                                    FirebaseDatabase.getInstance()
                                        .getReference("Posts/$intentPostId")
                                        .child("commentCount")
                                postCommentCountRef.get().addOnSuccessListener(this) {
                                    postCommentCountRef.setValue(it.value.toString().toInt() + 1)
                                }
                                    .addOnSuccessListener(this) { toast("댓글 작성 성공") }
                                    .addOnCanceledListener(this) { toast("댓글 작성 취소") }
                                    .addOnFailureListener(this) { toast("댓글 작성 실패") }
//                                toast("댓글 작성 성공")
                                finish()
                            }
                            "postModify" -> {
                                // 존재하는지 확인. 수정하는 도중에 지워질수도... 는 관리자 생각 안하니까 없나..?
                                // 혹시 모르니까 넣을까?
                                // 아니. 안전하게 Firebase 의 Update 기능을 이용한다.
                                // Update 기능은.. 수정하는 것 뿐만 아니라 입력, 삭제도 모두 포함.
                                // 만약 Update 시점에서 없으면 그냥 추가해버린다.
                                // Exist 하는지를 직접 구현해야할듯
                                val newRef =
                                    FirebaseDatabase.getInstance().getReference("/Posts")
                                modifyCard.bgUri = bgList[currentBgPosition]
                                modifyCard.message = binding.writeEditText.text.toString()
                                val modifyValues = modifyCard.toMap()
                                val childUpdates = hashMapOf<String, Any?>(
                                    modifyCard.postId to modifyValues
                                )
                                newRef.updateChildren(childUpdates)
                                    .addOnSuccessListener(this) { toast("카드 수정 성공") }
                                    .addOnCanceledListener(this) { toast("카드 수정 취소") }
                                    .addOnFailureListener(this) { toast("카드 수정 실패") }
                                Log.d("KHJ", "Modifying Post ${modifyCard.message}")
                                finish()
                            }
                            "commentModify" -> {
                                val newRef =
                                    FirebaseDatabase.getInstance().getReference("/Comments")
                                modifyCard.bgUri = bgList[currentBgPosition]
                                modifyCard.message = binding.writeEditText.text.toString()
                                val modifyValues = modifyCard.toMap()
                                val childUpdates = hashMapOf<String, Any?>(
                                    modifyCard.postId to modifyValues
                                )
                                newRef.updateChildren(childUpdates)
                                    .addOnSuccessListener(this) { toast("카드 수정 성공") }
                                    .addOnCanceledListener(this) { toast("카드 수정 취소") }
                                    .addOnFailureListener(this) { toast("카드 수정 실패") }
                                Log.d("KHJ", "Modifying Post ${modifyCard.message}")
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