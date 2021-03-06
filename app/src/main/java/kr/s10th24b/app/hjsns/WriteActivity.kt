package kr.s10th24b.app.hjsns

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.jakewharton.rxbinding4.view.clicks
import com.trello.rxlifecycle4.android.ActivityEvent
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import com.trello.rxlifecycle4.kotlin.bindUntilEvent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import kr.s10th24b.app.hjsns.databinding.ActivityWriteBinding
import kr.s10th24b.app.hjsns.databinding.CardBackgroundBinding
import splitties.toast.toast
import java.util.concurrent.TimeUnit

class WriteActivity : RxAppCompatActivity() {
    lateinit var binding: ActivityWriteBinding
    var intentPostId = ""
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
//        toast("onCreate")
        binding = ActivityWriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val mode = intent.getStringExtra("mode") ?: ""
        intentPostId = intent.getStringExtra("postId") ?: ""
        val tempModifyCard = intent.getSerializableExtra("post") ?: Post()
        val modifyCard = tempModifyCard as Post

        val tempModifyComment = intent.getSerializableExtra("comment") ?: Comment()
        val modifyComment = tempModifyComment as Comment

        if (mode == "postModify") {
//            toast("postModify")
            Glide.with(this)
                .load(Uri.parse(modifyCard.bgUri))
                .centerCrop()
                .into(binding.writeImageView)
            binding.writeEditText.setText(modifyCard.message)
            currentBgPosition = bgList.indexOfFirst { it == modifyCard.bgUri }
            if (currentBgPosition == -1) currentBgPosition = 0
        } else if (mode == "commentModify") {
//            toast("commentModify")
            Glide.with(this)
                .load(Uri.parse(modifyComment.bgUri))
                .centerCrop()
                .into(binding.writeImageView)
            binding.writeEditText.setText(modifyComment.message)
            currentBgPosition = bgList.indexOfFirst { it == modifyComment.bgUri }
            if (currentBgPosition == -1) currentBgPosition = 0
        } else {

        }

        val recyclerViewAdapter = CardBackgroundRecyclerViewAdapter()
        recyclerViewAdapter.cardBackgroundList = bgList
        binding.writeRecyclerView.adapter = recyclerViewAdapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        binding.writeRecyclerView.layoutManager = layoutManager

        binding.writeShareButton.clicks()
            .debounce(200L, TimeUnit.MILLISECONDS)
            .bindUntilEvent(this,ActivityEvent.DESTROY)
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
                            post.writerId = CurrentUser.getInstance().userId
                            post.postId = newRef.key.toString()
                            newRef.setValue(post)
                                .addOnSuccessListener(this) { toast("?????? ?????? ??????") }
                                .addOnCanceledListener(this) { toast("?????? ?????? ??????") }
                                .addOnFailureListener(this) { toast("?????? ?????? ??????") }
                            Log.d("KHJ", "Adding Post ${post.message}")
//                                toast("?????? ?????? ??????")
                            finish()
                        }
                        "comment" -> {
                            // ?????? ???????????? ????????? ?????? ??????????????? ???????????????.
                            FirebaseDatabase.getInstance().getReference("Posts")
                                .orderByChild("postId").equalTo(intentPostId)
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
//                                                toast("post exist")
                                            Log.d("KHJ", "post exist. adding comment.")
                                            val comment = Comment()
                                            val newRef =
                                                FirebaseDatabase.getInstance()
                                                    .getReference("Comments/$intentPostId")
                                                    .push()
                                            comment.writeTime = ServerValue.TIMESTAMP
                                            comment.bgUri = bgList[currentBgPosition]
                                            comment.message =
                                                binding.writeEditText.text.toString()
                                            comment.writerId = CurrentUser.getInstance().userId
                                            comment.postId = intentPostId
                                            comment.commentId = newRef.key.toString()
                                            newRef.setValue(comment)
                                            Log.d("KHJ", "Adding Comment ${comment.message}")
                                            //// You should write below code in writing part, not viewing activity
                                            // here, right.
                                            val postRef =
                                                FirebaseDatabase.getInstance()
                                                    .getReference("Posts/$intentPostId")
                                            postRef.runTransaction(object :
                                                Transaction.Handler {
                                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                                    val p =
                                                        currentData.getValue(Post::class.java)
                                                            ?: return Transaction.success(
                                                                currentData
                                                            )
                                                    p.commentCount++
                                                    currentData.value = p
//                                        Toast.makeText(this@WriteActivity,"????????? ?????????????????????",Toast.LENGTH_SHORT).show()
                                                    Log.d("KHJ", "????????? ?????????????????????")
                                                    return Transaction.success(currentData)
                                                }

                                                override fun onComplete(
                                                    error: DatabaseError?,
                                                    committed: Boolean,
                                                    currentData: DataSnapshot?
                                                ) {
                                                    Log.d(
                                                        "KHJ",
                                                        "postModifyTransaction:onComplete(), $error"
                                                    )
                                                }
                                            })
//                                toast("?????? ?????? ??????")
                                        } else {
                                            toast("????????? ?????? ?????????????????????")
                                            Log.d(
                                                "KHJ",
                                                "post not exist. abort adding comment."
                                            )
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        Log.d("KHJ", "onCancelled in adding comment")
                                        error("onCancelled in adding comment")
                                    }
                                })
                            finish()
                        }
                        "postModify" -> {
                            // ??????????????? ??????. ???????????? ????????? ???????????????... ??? ????????? ?????? ???????????? ??????..?
                            // ?????? ???????????? ??????????
                            // ??????. ???????????? Firebase ??? Update ????????? ????????????.
                            // Update ?????????.. ???????????? ??? ?????? ????????? ??????, ????????? ?????? ??????.
                            // ?????? Update ???????????? ????????? ?????? ??????????????????.
                            // Exist ???????????? ?????? ??????????????????
                            // Transaction ??? ????????????.
                            // ?????? ???????????? ?????? ?????? ???????????? ?????? ????????? ??? ?????? ???????????? ????????? ?????? ???????????? ????????? ????????? ??? ????????????.
                            val newRef =
                                FirebaseDatabase.getInstance()
                                    .getReference("/Posts/${modifyCard.postId}")
//                                    FirebaseDatabase.getInstance().getReference("/Posts")

                            // Update ????????? ??????. Transaction ?????? ??????
//                                modifyCard.bgUri = bgList[currentBgPosition]
//                                modifyCard.message = binding.writeEditText.text.toString()
//                                val modifyValues = modifyCard.toMap()
//                                val childUpdates = hashMapOf<String, Any?>(
//                                    modifyCard.postId to modifyValues
//                                )
//                                newRef.updateChildren(childUpdates)
//                                    .addOnSuccessListener(this) { toast("?????? ?????? ??????") }
//                                    .addOnCanceledListener(this) { toast("?????? ?????? ??????") }
//                                    .addOnFailureListener(this) { toast("?????? ?????? ??????") }

                            newRef.runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    val p = currentData.getValue(Post::class.java)
                                        ?: return Transaction.success(currentData)

                                    p.bgUri = bgList[currentBgPosition]
                                    p.message = binding.writeEditText.text.toString()
                                    currentData.value = p
//                                        Toast.makeText(this@WriteActivity,"????????? ?????????????????????",Toast.LENGTH_SHORT).show()
                                    Log.d("KHJ", "????????? ?????????????????????")
                                    return Transaction.success(currentData)
                                }

                                override fun onComplete(
                                    error: DatabaseError?,
                                    committed: Boolean,
                                    currentData: DataSnapshot?
                                ) {
                                    Log.d("KHJ", "postModifyTransaction:onComplete(), $error")
                                    Log.d(
                                        "KHJ",
                                        "postModifyTransaction:onComplete() committed, $committed"
                                    )
                                }
                            })
                            Log.d("KHJ", "Modifying Post ${modifyCard.message}")
                            finish()
                        }
                        "commentModify" -> {
                            val newRef =
                                FirebaseDatabase.getInstance()
                                    .getReference("/Comments/${modifyComment.postId}/${modifyComment.commentId}")
                            newRef.runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    val p = currentData.getValue(Comment::class.java)
                                        ?: return Transaction.success(currentData)

                                    p.bgUri = bgList[currentBgPosition]
                                    p.message = binding.writeEditText.text.toString()
                                    currentData.value = p
//                                        Toast.makeText(this@WriteActivity,"????????? ?????????????????????",Toast.LENGTH_SHORT).show()
                                    Log.d("KHJ", "????????? ?????????????????????")
                                    return Transaction.success(currentData)
                                }

                                override fun onComplete(
                                    error: DatabaseError?,
                                    committed: Boolean,
                                    currentData: DataSnapshot?
                                ) {
                                    Log.d(
                                        "KHJ",
                                        "commentModifyTransaction:onComplete(), $error"
                                    )
                                    Log.d(
                                        "KHJ",
                                        "commentModifyTransaction:onComplete() committed, $committed"
                                    )
                                }
                            })
                            Log.d("KHJ", "Modifying Comment ${modifyComment.message}")
                            finish()

                        }
                        else -> {
                            error("error in writeShareButton")
                        }
                    }
                } else {
                    toast("????????? ??????????????????")
                }
            }, {
                Log.d("KHJ", "${it.toString()}")
                it.printStackTrace()

            })
    }

    @SuppressLint("HardwareIds")
    fun getDeviceId(): String { // Return Device ID
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }

    override fun onDestroy() {
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
            Glide.with(this@WriteActivity)
                .load(Uri.parse(cardBackground))
                .centerCrop()
                .into(holder.imageView)
            holder.itemView.setOnClickListener {
                Glide.with(this@WriteActivity)
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