package knf.kuma.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import knf.kuma.R
import knf.kuma.animeinfo.ActivityAnime
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.bind
import knf.kuma.commons.load

class SearchAdapter internal constructor(private val fragment: Fragment) :
    PagingDataAdapter<SearchObject, SearchAdapter.ItemHolder>(DIFF_CALLBACK) {

    private val layType = PrefsUtil.layType

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
        return ItemHolder(
            LayoutInflater.from(parent.context).inflate(
                if (layType == "0")
                    R.layout.item_dir
                else
                    R.layout.item_dir_grid, parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ItemHolder, position: Int) {
        if (fragment.context == null) return
        val animeObject = getItem(position)
        if (animeObject != null) {
            holder.imageView.load(PatternUtil.getCover(animeObject.aid))
            holder.progressView.visibility = View.GONE
            holder.textView.text = animeObject.name
            holder.cardView.setOnClickListener { ActivityAnime.open(fragment, animeObject, holder.imageView, false, true) }
        } else {
            holder.progressView.visibility = View.VISIBLE
            holder.textView.text = null
        }
    }

    inner class ItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView by itemView.bind(R.id.card)
        val imageView: ImageView by itemView.bind(R.id.img)
        val progressView: ProgressBar by itemView.bind(R.id.progress)
        val textView: TextView by itemView.bind(R.id.title)
    }

    companion object {

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SearchObject>() {
            override fun areItemsTheSame(oldItem: SearchObject, newItem: SearchObject): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(oldItem: SearchObject, newItem: SearchObject): Boolean {
                return oldItem.name == newItem.name && oldItem.aid == newItem.aid
            }
        }
    }
}
