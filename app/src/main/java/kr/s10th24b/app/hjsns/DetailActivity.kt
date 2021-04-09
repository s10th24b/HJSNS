package kr.s10th24b.app.hjsns

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.renderscript.Sampler
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import kr.s10th24b.app.hjsns.databinding.ActivityDetailBinding
import kr.s10th24b.app.hjsns.databinding.CardCommentBinding
import splitties.toast.toast
import java.text.SimpleDateFormat
import java.util.*

class DetailActivity : RxAppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    lateinit var layoutManager: LinearLayoutManager
    var recyclerViewAdapter = DetailRecyclerViewAdapter()
    var alreadyCreated = false
    private var intentPostId = ""
    lateinit var postValueEventListener: ValueEventListener
    lateinit var commentChildEventListener: ChildEventListener
    lateinit var menuCard: Post
    lateinit var menuComment: Comment
    lateinit var menuIn: Any
    override fun onCreate(savedInstanceState: Bundle?) {
//        toast("onCreate")
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

        binding.detailImageView.setOnCreateContextMenuListener { menu, v, menuInfo ->
            val post = intentPost
            menuCard = post
            menuIn = menuCard
            val inflater = MenuInflater(this@DetailActivity)
            inflater.inflate(R.menu.card_floating_menu, menu)
            //                    menu.setHeaderTitle("메뉴")
            if (post.writerId == getMyId()) {
                menu.removeItem(R.id.menu_item_report)
            } else {
                menu.removeItem(R.id.menu_item_remove)
                menu.removeItem(R.id.menu_item_modify)
            }
        }

        FirebaseDatabase.getInstance().getReference("Likes/$intentPostId")
            .orderByChild("likerId").equalTo(getMyId())
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
//                            toast("like already exist")
                        Log.d("KHJ", "like already exist")
                        Glide.with(this@DetailActivity)
                            .load(R.drawable.lb_ic_thumb_up)
                            .into(binding.detailLikeFloatingButton)
                    } else {
//                            toast("like not exist")
                        Log.d("KHJ", "like not exist")
                        Glide.with(this@DetailActivity)
                            .load(R.drawable.lb_ic_thumb_up_outline)
                            .into(binding.detailLikeFloatingButton)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("KHJ", "onCacelled in likerId")
                    error("onCacelled in likerId")
                }
            })
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
//        val info = item.menuInfo as AdapterView.AdapterContextMenuInfo
        return when (item.itemId) {
            R.id.menu_item_modify -> {
                val intent = Intent(this, WriteActivity::class.java)
                if (menuIn is Post) {
                    intent.putExtra("mode", "postModify")
                    intent.putExtra("post", menuCard)
                } else if (menuIn is Comment) {
                    intent.putExtra("mode", "commentModify")
                    intent.putExtra("comment", menuComment)
                }
                startActivity(intent)
                true
            }
            R.id.menu_item_remove -> {
                val dbRef = FirebaseDatabase.getInstance().getReference("")
                if (menuIn is Post) {
                    val postPath = "/Posts/${menuCard.postId}"
                    val commentPath = "/Comments/${menuCard.postId}"
                    val likePath = "/Likes/${menuCard.postId}"
                    val childUpdates = hashMapOf<String, Any?>(
                        postPath to null,
                        commentPath to null,
                        likePath to null
                    )
                    dbRef.updateChildren(childUpdates)
                        .addOnSuccessListener(this) { toast("카드 삭제 성공") }
                        .addOnCanceledListener(this) { toast("카드 삭제 취소") }
                        .addOnFailureListener(this) { toast("카드 삭제 실패") }
                    finish()
                } else if (menuIn is Comment) {
                    val commentPath = "/Comments/${menuComment.postId}/${menuComment.commentId}"
                    val childUpdates = hashMapOf<String, Any?>(
                        commentPath to null,
                    )
                    dbRef.updateChildren(childUpdates)
                        .addOnSuccessListener(this) { toast("댓글 삭제 성공") }
                        .addOnCanceledListener(this) { toast("댓글 삭제 취소") }
                        .addOnFailureListener(this) { toast("댓글 삭제 실패") }

                    dbRef.child("/Posts/${menuComment.postId}").runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val p = currentData.getValue(Post::class.java)
                                ?: return Transaction.success(currentData)

                            p.commentCount = p.commentCount-1
                            currentData.value = p
//                                        Toast.makeText(this@WriteActivity,"카드가 수정되었습니다",Toast.LENGTH_SHORT).show()
                            Log.d("KHJ","댓글을 수정했습니다")
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(
                            error: DatabaseError?,
                            committed: Boolean,
                            currentData: DataSnapshot?
                        ) {
                            Log.d("KHJ", "postModifyTransaction:onComplete(), $error")
                            Log.d("KHJ", "postModifyTransaction:onComplete() committed, $committed")
                        }
                    })
                }
                true
            }
            R.id.menu_item_report -> {
                true
            }
            else -> {
                super.onContextItemSelected(item)
            }
        }
    }


    override fun onStop() {
//        toast("onStop")
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
        FirebaseDatabase.getInstance().getReference("/Posts/$intentPostId")
            .removeEventListener(postValueEventListener)
        FirebaseDatabase.getInstance().getReference("/Comments/$intentPostId")
            .removeEventListener(commentChildEventListener)
        unregisterForContextMenu(binding.detailRecyclerView)
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
                            recyclerViewAdapter.notifyDataSetChanged()
                        } else {
                            val prevIndex =
                                recyclerViewAdapter.commentList.indexOfFirst { it.commentId == previousChildName }
                            recyclerViewAdapter.commentList.add(prevIndex + 1, cmm)
                            recyclerViewAdapter.notifyItemInserted(prevIndex + 1)
                            recyclerViewAdapter.notifyDataSetChanged()
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
                            recyclerViewAdapter.notifyDataSetChanged()
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
                        recyclerViewAdapter.notifyDataSetChanged()
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    val comment = snapshot.getValue(Comment::class.java)
                    comment?.let { cmm ->
                        val existIndex =
                            recyclerViewAdapter.commentList.indexOfFirst { cmm.commentId == it.commentId }
                        recyclerViewAdapter.commentList.removeAt(existIndex)
                        recyclerViewAdapter.notifyItemRemoved(existIndex)
                        recyclerViewAdapter.notifyDataSetChanged()

                        val prevIndex =
                            recyclerViewAdapter.commentList.indexOfFirst { cmm.commentId == previousChildName }
                        recyclerViewAdapter.commentList.add(prevIndex + 1, cmm)
                        recyclerViewAdapter.notifyItemInserted(prevIndex)
                        recyclerViewAdapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    error.toException().printStackTrace()
                    Log.d("KHJ", "Error in eventListener!")
                    error(error)
                }
            })

    }

    inner class DetailRecyclerViewAdapter : RecyclerView.Adapter<DetailRecyclerViewHolder>() {
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
            Log.d("KHJ","comment: ${comment.message}, position: $position")
            holder.bind(comment, position)
        }

        override fun getItemCount() = commentList.size
    }

    inner class DetailRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var binding = CardCommentBinding.bind(itemView)
        private val commentImageView = binding.cardCommentImageView
        private val commentTextView = binding.cardCommentTextView
        fun bind(comment: Comment, position: Int) {
            Glide.with(this@DetailActivity)
                .load(Uri.parse(comment.bgUri))
                .centerCrop()
                .into(commentImageView)
            commentTextView.text = comment.message
            binding.root.setOnClickListener {
//                    clickCardSubject.onNext(card)
            }
            binding.root.setOnCreateContextMenuListener { menu, v, menuInfo ->
                val comm = recyclerViewAdapter.commentList[position]
                menuComment = comm
                menuIn = menuComment
//                Toast.makeText(this@DetailActivity,"menuIn is initialized!",Toast.LENGTH_SHORT).show()
                val inflater = MenuInflater(this@DetailActivity)
                inflater.inflate(R.menu.card_floating_menu, menu)
                //                    menu.setHeaderTitle("메뉴")
                if (comm.writerId == getMyId()) {
                    menu.removeItem(R.id.menu_item_report)
                } else {
                    menu.removeItem(R.id.menu_item_remove)
                    menu.removeItem(R.id.menu_item_modify)
                }
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

    @SuppressLint("HardwareIds")
    fun getMyId(): String { // Return Device ID
        return Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
    }
}