package kr.s10th24b.app.hjsns

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.s10th24b.app.hjsns.databinding.FragmentCardsBinding

class CardsFragment : Fragment() {
    lateinit var binding: FragmentCardsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCardsBinding.inflate(layoutInflater)
        return binding.root
    }

    inner class CardRecyclerViewAdapter : RecyclerView.Adapter<CardRecyclerViewHolder>() {
        val cardList = mutableListOf<String>()
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
        fun bind(card: String) {

        }
    }
}