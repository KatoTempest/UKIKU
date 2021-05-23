package knf.kuma.news

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration
import knf.kuma.R
import knf.kuma.ads.AdsType
import knf.kuma.ads.implBanner
import knf.kuma.commons.EAHelper
import knf.kuma.commons.PrefsUtil
import knf.kuma.commons.asPx
import knf.kuma.custom.GenericActivity
import kotlinx.android.synthetic.main.activity_news.*

class NewsActivity : GenericActivity(), SwipeRefreshLayout.OnRefreshListener {
    val adapter: NewsAdapter by lazy { NewsAdapter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(EAHelper.getTheme())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news)
        toolbar.title = "Noticias"
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        refresh.setColorSchemeResources(EAHelper.getThemeColor(), EAHelper.getThemeColorLight(), R.color.colorPrimary)
        refresh.setOnRefreshListener(this)
        refresh.isRefreshing = true
        recycler.adapter = adapter
        recycler.addItemDecoration(SpacingItemDecoration(0, 10.asPx))
        NewsCreator.createNews().observe(this, {
            if (it == null || it.isEmpty())
                error.visibility = View.VISIBLE
            else {
                error.visibility = View.GONE

                adapter.update(it)
                recycler.scheduleLayoutAnimation()
            }
            refresh.isRefreshing = false
        })
        if (!PrefsUtil.isNativeAdsEnabled)
            adContainer.implBanner(AdsType.NEWS_BANNER)
    }

    override fun onRefresh() {
        refresh.isRefreshing = true
        NewsCreator.reload()
    }

    override fun onDestroy() {
        super.onDestroy()
        NewsCreator.destroy()
    }

    companion object {
        fun open(context: Context) {
            context.startActivity(Intent(context, NewsActivity::class.java))
        }
    }
}