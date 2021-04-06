package kr.s10th24b.app.hjsns

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import kr.s10th24b.app.hjsns.databinding.CardPostRecyclerBinding
import kr.s10th24b.app.hjsns.databinding.FragmentCardsBinding
import splitties.systemservices.layoutInflater
import splitties.toast.toast
import java.io.Serializable
import java.text.SimpleDateFormat

class CardsFragment : Fragment() {
    lateinit var binding: FragmentCardsBinding
    lateinit var recyclerViewAdapter: CardRecyclerViewAdapter
    lateinit var layoutManager: LinearLayoutManager
    var clickCardSubject = PublishSubject.create<Post>()
    var clickLikeSubject = PublishSubject.create<Post>()
    val mCompositeDisposable = CompositeDisposable()

    //    lateinit var mState: Bundle
    var alreadyCreated = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        toast("onCreate")
        if (savedInstanceState == null) {
            Log.d("KHJ", "savedInstanceState is null!")
        } else {
            Log.d("KHJ", "savedInstanceState is not null!")
        }
        binding = FragmentCardsBinding.inflate(layoutInflater)
        arguments?.let {
        }

        if (!alreadyCreated) {
            setFirebaseDatabaseListener()
        }
        setClickCardSubject()
    }

    private fun setClickCardSubject() {
//        toast("setClickCardSubject!")
        mCompositeDisposable.add(clickCardSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                val intent = Intent(context, DetailActivity::class.java)
                intent.putExtra("post", it)
                startActivity(intent)
            }
        )
        mCompositeDisposable.add(clickLikeSubject
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                FirebaseDatabase.getInstance().getReference("Like/${it.postId}")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {

                        }

                        override fun onCancelled(error: DatabaseError) {
                        }
                    })
                val like = Like()
                val newRef =
                    FirebaseDatabase.getInstance().getReference("Like/${it.postId}").push()
                like.likeId = newRef.key.toString()
                like.likerId = getMyId()
                like.postId = it.postId
                like.likeTime = ServerValue.TIMESTAMP
            }
        )
    }

    private fun setFirebaseDatabaseListener() {
        recyclerViewAdapter = CardRecyclerViewAdapter()
        // FireBase Data pulling and save it to posts variable
        FirebaseDatabase.getInstance().getReference("/Posts")
            .orderByChild("writeTime").addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    toast("onChildAdded()")
                    snapshot.let { ss ->
                        val post = ss.getValue(Post::class.java)
                        post?.let { newPost ->
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
//                                    recyclerViewAdapter.cardList.add(prevIndex + 1, newPost)
                                recyclerViewAdapter.cardList.add(newPost)
                                recyclerViewAdapter.notifyItemInserted(recyclerViewAdapter.cardList.lastIndex)
                                val firstCompVisPos =
                                    layoutManager.findFirstCompletelyVisibleItemPosition()
//                                val visibleItemCount = binding.cardsFragmentRecyclerView.childCount
                                val visibleItemCount = layoutManager.childCount
                                val totalItemCount = layoutManager.itemCount
//                                toast("firstCompVis: $firstCompVisPos, totalItemCount: $totalItemCount visibleItemCount: $visibleItemCount")
                                if (firstCompVisPos + visibleItemCount >= totalItemCount) {// If scroll top
                                    binding.cardsFragmentRecyclerView.scrollToPosition(
                                        recyclerViewAdapter.cardList.lastIndex
                                    )
                                }
                            }
                        }
                    }
                    for ((idx, card) in recyclerViewAdapter.cardList.withIndex()) {
                        Log.d("KHJ", "$idx: ${card.message}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    toast("onChildChanged()")
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
                    toast("onChildRemoved()")
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
                    toast("onChildMoved()")
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

    private fun saveInstanceState() {
    }

    override fun onAttach(context: Context) {
//        toast("onAttach()")
        super.onAttach(context)
    }

    override fun onStart() {
//        toast("onStart!")
        super.onStart()
    }

    override fun onResume() {
//        toast("onResume!")
        // Refresh View Data
        super.onResume()
        alreadyCreated = true
    }

    override fun onPause() {
//        toast("onPause()!")
        super.onPause()
    }

    override fun onStop() {
//        toast("onStop()!")
        saveInstanceState()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
//        toast("onSaveInstanceState()")
        Log.d("KHJ", "onSaveInstanceState()!")
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
//        toast("onDestroyView!")
        super.onDestroyView()
    }

    override fun onDestroy() {
//        toast("onDestroy!")
        mCompositeDisposable.clear()
        super.onDestroy()
    }

    inner class CardRecyclerViewAdapter :
        RecyclerView.Adapter<CardRecyclerViewHolder>() {
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
        fun bind(card: Post) {
            Glide.with(itemView.context)
                .load(Uri.parse(card.bgUri))
                .centerCrop()
                .into(binding.cardImageView)
            binding.contentTextView.text = card.message
            binding.commentCountTextView.text = card.commentCount.toString()
            binding.likeCountTextView.text = card.likeCount.toString()
            binding.timeTextView.text = formatTimeString(card.writeTime as Long)
            binding.cardImageView.setOnClickListener {
                clickCardSubject.onNext(card)
            }
            binding.likeImageView.setOnClickListener {
                clickLikeSubject.onNext(card)
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

    @SuppressLint("HardwareIds")
    fun getMyId(): String { // Return Device ID
        return Settings.Secure.getString(activity?.contentResolver, Settings.Secure.ANDROID_ID)
    }
}