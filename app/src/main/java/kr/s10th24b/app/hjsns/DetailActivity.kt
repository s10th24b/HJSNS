package kr.s10th24b.app.hjsns

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.renderscript.Sampler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kr.s10th24b.app.hjsns.databinding.ActivityDetailBinding
import kr.s10th24b.app.hjsns.databinding.CardCommentBinding
import kr.s10th24b.app.hjsns.databinding.CardPostRecyclerBinding
import java.text.SimpleDateFormat

class DetailActivity : AppCompatActivity() {
    lateinit var binding: ActivityDetailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intentPost = intent.getSerializableExtra("post") as Post
        val intentPostId = intentPost.postId

        val layoutManager = LinearLayoutManager(this)
        val recyclerViewAdapter = DetailRecyclerViewAdapter()
        binding.detailRecyclerView.adapter = recyclerViewAdapter
        binding.detailRecyclerView.layoutManager = layoutManager

            FirebaseDatabase.getInstance().getReference("/Posts/$intentPostId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.let { ss ->
                            val post = ss.getValue(Post::class.java)
                            post.let {
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                    }
                })


        binding.detailFloatingActionButton.setOnClickListener {

        }
    }

    inner class DetailRecyclerViewAdapter :
        RecyclerView.Adapter<DetailRecyclerViewAdapter.DetailRecyclerViewHolder>() {
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

        inner class DetailRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var binding = CardCommentBinding.bind(itemView)
            private val commentImageView = binding.cardCommentImageView
            private val commentTextView = binding.cardCommentTextView
            fun bind(comment: Comment) {
                Glide.with(itemView.context)
                    .load(Uri.parse(comment.bgUri))
                    .centerCrop()
                    .into(commentImageView)
                commentTextView.text = comment.message
                binding.root.setOnClickListener {
//                    clickCardSubject.onNext(card)
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
    }
}