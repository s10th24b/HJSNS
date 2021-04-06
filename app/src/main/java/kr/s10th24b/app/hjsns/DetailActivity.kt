package kr.s10th24b.app.hjsns

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Sampler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import kr.s10th24b.app.hjsns.databinding.ActivityDetailBinding
import kr.s10th24b.app.hjsns.databinding.CardCommentBinding
import splitties.toast.toast
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    lateinit var layoutManager: LinearLayoutManager
    var recyclerViewAdapter = DetailRecyclerViewAdapter()
    var alreadyCreated = false
    private var intentPostId = ""
    lateinit var postValueEventListener: ValueEventListener
    lateinit var commentChildEventListener: ChildEventListener
    override fun onCreate(savedInstanceState: Bundle?) {
        toast("onCreate")
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intentPost = intent.getSerializableExtra("post") as Post
        intentPostId = intentPost.postId

        binding.detailRecyclerView.adapter = recyclerViewAdapter
        layoutManager =
            LinearLayoutManager(this).apply { orientation = LinearLayoutManager.HORIZONTAL }
        binding.detailRecyclerView.layoutManager = layoutManager

        setFirebaseDatabasePostListener(intentPostId)
        setFirebaseDatabaseCommentListener(intentPostId)

        binding.detailFloatingActionButton.setOnClickListener {
            val intent = Intent(this, WriteActivity::class.java)
            intent.putExtra("mode", "comment")
            intent.putExtra("postId", intentPostId)
            startActivity(intent)
        }
    }

    override fun onStop() {
        toast("onStop")
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        alreadyCreated = true
    }

    fun setFirebaseDatabasePostListener(postId: String) {
        Log.d("KHJ", "setFirebaseDatabasePostListener")
        postValueEventListener = FirebaseDatabase.getInstance().getReference("/Posts/$postId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val post = snapshot.getValue(Post::class.java)
                    post?.let {
                        Glide.with(applicationContext).load(Uri.parse(it.bgUri))
                            .into(binding.detailImageView)
                        binding.detailTextView.text = it.message
                    }
//                    toast("onDataChange PostListener")
                    Log.d("KHJ", "onDataChange PostListener")
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }

    override fun onDestroy() {
        FirebaseDatabase.getInstance().getReference("/Posts/$intentPostId") .removeEventListener(postValueEventListener)
        FirebaseDatabase.getInstance().getReference("/Comments/$intentPostId") .removeEventListener(commentChildEventListener)
//        FirebaseDatabase.getInstance().getReference("/Comments/$") .removeEventListener(valueEventListener)
        super.onDestroy()
    }

    fun setFirebaseDatabaseCommentListener(postId: String) {
        commentChildEventListener = FirebaseDatabase.getInstance().getReference("/Comments/$postId")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let { cmm ->
                        if (previousChildName == null) {
                            Log.d("KHJ", "previousChildName: null")
                            recyclerViewAdapter.commentList.add(cmm)
                            Log.d("KHJ", "commentList: ${recyclerViewAdapter.commentList}")
                            recyclerViewAdapter.notifyItemInserted(0)
                        } else {
                            val prevIndex =
                                recyclerViewAdapter.commentList.indexOfFirst { it.commentId == previousChildName }
                            recyclerViewAdapter.commentList.add(prevIndex + 1, cmm)
                            recyclerViewAdapter.notifyItemInserted(prevIndex + 1)
                            val firstCompVisPos =
                                layoutManager.findFirstCompletelyVisibleItemPosition()
                            val visibleItemCount = binding.detailRecyclerView.childCount
                            val totalItemCount = layoutManager.itemCount
//                            toast("firstCompVis: $firstCompVisPos, totalItemCount: $totalItemCount visibleItemCount: $visibleItemCount")
                            if (firstCompVisPos + visibleItemCount >= totalItemCount) {// If scroll top
                                binding.detailRecyclerView.scrollToPosition(
                                    recyclerViewAdapter.commentList.lastIndex
                                )
                            }
                        }
                    }
                    Log.d("KHJ", "commentList: ${recyclerViewAdapter.commentList}")
                    for ((idx, com) in recyclerViewAdapter.commentList.withIndex()) {
                        Log.d("KHJ", "$idx, ${com.message}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    snapshot.let { ss ->
                        val comment = ss.getValue(Comment::class.java)
                        comment?.let { cmm ->
                            val prevIndex =
                                recyclerViewAdapter.commentList.indexOfFirst { cmm.commentId == it.commentId }
                            recyclerViewAdapter.commentList[prevIndex] = cmm
                            recyclerViewAdapter.notifyItemChanged(prevIndex)
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let { cmm ->
                        val existIndex =
                            recyclerViewAdapter.commentList.indexOfFirst { cmm.commentId == it.commentId }
                        recyclerViewAdapter.commentList.removeAt(existIndex)
                        recyclerViewAdapter.notifyItemRemoved(existIndex)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let { cmm ->
                        val existIndex =
                            recyclerViewAdapter.commentList.indexOfFirst { cmm.commentId == it.commentId }
                        recyclerViewAdapter.commentList.removeAt(existIndex)
                        recyclerViewAdapter.notifyItemRemoved(existIndex)

                        val prevIndex =
                            recyclerViewAdapter.commentList.indexOfFirst { cmm.commentId == previousChildName }
                        recyclerViewAdapter.commentList.add(prevIndex + 1, cmm)
                        recyclerViewAdapter.notifyItemInserted(prevIndex)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                    Log.d("KHJ", "Error in eventListener!")
                    error(error)
                }
            })

    }

    fun removeComment() {
        // 내가 배운 것.
        // FirebaseDatabase CRUD Operation 안에는, 또다른 CRUD Operation이 있으면 안된다.
        // multiple clients 가 존재할 때, duplicated 되기 때문.
        val postId = ""
        //// You should write below code in removing function, not viewing activity
        val postCommentCountRef =
            FirebaseDatabase.getInstance().getReference("Posts/$postId")
                .child("commentCount")
        postCommentCountRef.get().addOnSuccessListener(this@DetailActivity) {
            postCommentCountRef.setValue(it.value.toString().toInt() - 1)
        }.addOnCanceledListener(this@DetailActivity) { Log.d("KHJ", "Error getting data from $postId") }
        ///
    }

    inner class DetailRecyclerViewAdapter :
        RecyclerView.Adapter<DetailRecyclerViewHolder>() {
        val commentList = mutableListOf<Comment>()
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): DetailRecyclerViewHolder {
            return DetailRecyclerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_comment, parent, false)
            )
        }

        override fun onBindViewHolder(holder: DetailRecyclerViewHolder, position: Int) {
            val comment = commentList[position]
            holder.bind(comment)
        }

        override fun getItemCount() = commentList.size
    }

    inner class DetailRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = CardCommentBinding.bind(itemView)
        private val commentImageView = binding.cardCommentImageView
        private val commentTextView = binding.cardCommentTextView
        fun bind(comment: Comment) {
            Glide.with(this@DetailActivity)
                .load(Uri.parse(comment.bgUri))
                .centerCrop()
                .into(commentImageView)
            commentTextView.text = comment.message
            binding.root.setOnClickListener {
//                    clickCardSubject.onNext(card)
            }
        }
    }

    fun formatTimeString(regTime: Long): String? {
        val SEC = 60
        val MIN = 60
        val HOUR = 24
        val DAY = 30
        val MONTH = 12
        val curTime = System.currentTimeMillis()
        var diffTime = (curTime - regTime) / 1000
        var msg: String? = null
        if (diffTime < SEC) {
            msg = "방금 전"
        } else if (SEC.let { diffTime /= it; diffTime } < MIN) {
            msg = diffTime.toString() + "분 전"
        } else if (MIN.let { diffTime /= it; diffTime } < HOUR) {
            msg = diffTime.toString() + "시간 전"
        } else if (HOUR.let { diffTime /= it; diffTime } < DAY) {
            msg = diffTime.toString() + "일 전"
        } else {
            val sdf = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm")
            msg = sdf.format(regTime)
        }
        return msg
    }
}