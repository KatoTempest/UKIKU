package knf.kuma.animeinfo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import knf.kuma.R
import knf.kuma.commons.PatternUtil
import knf.kuma.commons.PicassoSingle
import knf.kuma.pojos.AnimeObject
import kotlinx.android.synthetic.main.item_related.view.*

internal class AnimeRelatedAdapter(private val fragment: Fragment, private val list: MutableList<AnimeObject.WebInfo.AnimeRelated>) : RecyclerView.Adapter<AnimeRelatedAdapter.RelatedHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RelatedHolder {
        return RelatedHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_related, parent, false))
    }

    override fun onBindViewHolder(holder: RelatedHolder, position: Int) {
        val related = list[position]
        holder.textView.text = related.name
        holder.relation.text = related.relation
        if (related.aid != "null") {
            holder.imageView.visibility = View.VISIBLE
            PicassoSingle.get().load(PatternUtil.getCover(related.aid)).into(holder.imageView)
            holder.cardView.setOnClickListener { ActivityAnime.open(fragment, related, holder.imageView) }
        } else {
            holder.imageView.visibility = View.GONE
            holder.cardView.setOnClickListener { ActivityAnime.open(fragment, related) }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    internal class RelatedHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: LinearLayout = itemView.card
        val imageView: ImageView = itemView.img
        val textView: TextView = itemView.title
        val relation: TextView = itemView.relation
    }
}
