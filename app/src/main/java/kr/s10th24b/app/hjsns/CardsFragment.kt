package kr.s10th24b.app.hjsns

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kr.s10th24b.app.hjsns.databinding.CardPostRecyclerBinding
import kr.s10th24b.app.hjsns.databinding.FragmentCardsBinding
import splitties.systemservices.layoutInflater
import splitties.toast.toast
import java.text.SimpleDateFormat

class CardsFragment : Fragment() {
    lateinit var binding: FragmentCardsBinding
    lateinit var recyclerViewAdapter: CardRecyclerViewAdapter
    lateinit var layoutManager: LinearLayoutManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FragmentCardsBinding.inflate(layoutInflater)
        arguments?.let {
        }
        recyclerViewAdapter = CardRecyclerViewAdapter()

        // FireBase Data pulling and save it to posts variable
        FirebaseDatabase.getInstance().getReference("/Posts")
            .orderByChild("writeTime").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                    toast("onChildAdded()")
                    snapshot.let { ss ->
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            // 새 글이 마지막 부분에 추가된 경우
//                            toast("previousChildName: $previousChildName")
                            if (previousChildName == null) {
//                                toast("새 글이 마지막부분에 추가")
                                recyclerViewAdapter.cardList.add(newPost as Post)
                                recyclerViewAdapter.notifyItemInserted(0)
//                                recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.cardList.lastIndex)
//                                recyclerViewAdapter.notifyDataSetChanged()
                            } else { // 글이 중간에 삽입된 경우... 근데 이건 이 앱에선 상관없음. 무조건 마지막에 추가되도록 했으니.
//                                toast("새 글이 중간에 추가")
//                                val prevIndex = recyclerViewAdapter.cardList
//                                    .indexOfFirst { it.postId == previousChildName }
                                if (newPost != null) {
//                                    recyclerViewAdapter.cardList.add(prevIndex + 1, newPost)
                                    recyclerViewAdapter.cardList.add(newPost)
//                                    recyclerViewAdapter.notifyItemInserted(0)
                                    recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.cardList.lastIndex)
//                                    if (layoutManager.findFirstVisibleItemPosition())
                                    val firstCompVisPos =
                                        layoutManager.findFirstCompletelyVisibleItemPosition()
                                    val visibleItemCount =
                                        binding.cardsFragmentRecyclerView.childCount
                                    val totalItemCount = layoutManager.itemCount
//                                    toast("firstCompVis: $firstCompVisPos, totalItemCount: $totalItemCount visibleItemCount: $visibleItemCount")
                                    if (firstCompVisPos + visibleItemCount == totalItemCount) {// If scroll top
                                        binding.cardsFragmentRecyclerView.scrollToPosition(
                                            recyclerViewAdapter.cardList.lastIndex
                                        )
                                    }
                                } else {
                                    error("Error in onChildAdded!")
                                }
                            }
                        }
                    }
                    for (card in recyclerViewAdapter.cardList) {
                        Log.d("KHJ", "${card.message}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
//                    toast("onChildChanged()")
                    snapshot.let { ss ->
                        //snapshot의 데이터를 Post의 객체로
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            val prevIndex = recyclerViewAdapter.cardList
                                .indexOfFirst { it.postId == previousChildName }
                            if (newPost != null) {
                                recyclerViewAdapter.cardList[prevIndex + 1] = newPost
                                recyclerViewAdapter.notifyItemChanged(prevIndex + 1)
                            } else {
                                error("Error in onChildChanged!")
                            }
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
//                    toast("onChildRemoved()")
                    snapshot.let { ss ->
                        //snapshot의 데이터를 Post의 객체로 가져옴
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            val existIndex = recyclerViewAdapter.cardList
                                .indexOfFirst { it.postId == newPost?.postId }
                            if (existIndex != -1) {
                                recyclerViewAdapter.cardList.removeAt(existIndex)
                                recyclerViewAdapter.notifyItemRemoved(existIndex)
                            } else {
                                recyclerViewAdapter.notifyDataSetChanged()
                            }
                        }
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
//                    toast("onChildMoved()")
                    snapshot.let { ss ->
                        //snapshot의 데이터를 Post의 객체로
                        val post = ss.getValue(Post::class.java)
                        post.let { newPost ->
                            val existIndex = recyclerViewAdapter.cardList
                                .indexOfFirst { it.postId == newPost?.postId }
                            recyclerViewAdapter.cardList.removeAt(existIndex)
                            recyclerViewAdapter.notifyItemRemoved(existIndex)
                            // prevChildName 이 없는 경우 맨 마지막으로 이동된 것
                            if (previousChildName == null) {
                                recyclerViewAdapter.cardList.add(newPost!!)
                                recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.cardList.lastIndex)
                            } else {
                                // prevChildName 다음 글로 추가
                                val prevIndex =
                                    recyclerViewAdapter.cardList.indexOfFirst { it.postId == previousChildName }
                                if (prevIndex != -1) {
                                    recyclerViewAdapter.cardList.add(prevIndex + 1, newPost!!)
                                    recyclerViewAdapter.notifyItemInserted(prevIndex + 1)
                                } else {
                                    error("Error in onChildMoved!")

                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    toast("onCancelled()")
                    error.toException().printStackTrace()
                    error(error.toString())
                }
            })

    }

    override fun onStart() {
//        toast("onStart!")
        super.onStart()
    }

    override fun onDestroy() {
//        toast("onDestroy!")
        super.onDestroy()
    }

    override fun onResume() {
//        toast("onResume!")
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        layoutManager = LinearLayoutManager(context)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        binding = FragmentCardsBinding.inflate(layoutInflater)
        binding.cardsFragmentRecyclerView.layoutManager = layoutManager
        binding.cardsFragmentRecyclerView.adapter = recyclerViewAdapter
        return binding.root
    }

    inner class CardRecyclerViewAdapter : RecyclerView.Adapter<CardRecyclerViewHolder>() {
        val cardList = mutableListOf<Post>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardRecyclerViewHolder {
            return CardRecyclerViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.card_post_recycler, parent, false)
            )
        }

        override fun onBindViewHolder(holder: CardRecyclerViewHolder, position: Int) {
            val card = cardList[position]
            holder.bind(card)
        }

        override fun getItemCount() = cardList.size
    }

    inner class CardRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var binding = CardPostRecyclerBinding.bind(itemView)
        val cardImageView = binding.cardImageView
        val contentTextView = binding.contentTextView
        val commentCountTextView = binding.commentCountTextView
        val likeCountTextView = binding.likeCountTextView
        val timeTextView = binding.timeTextView
        fun bind(card: Post) {
            Glide.with(itemView.context)
                .load(Uri.parse(card.bgUri))
                .centerCrop()
                .into(cardImageView)
            contentTextView.text = card.message
            commentCountTextView.text = card.commentCount.toString()
            likeCountTextView.text = card.likeCount.toString()
            timeTextView.text = formatTimeString(card.writeTime as Long)
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