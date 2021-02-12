package knf.kuma.download

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.text.format.Formatter
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import knf.kuma.commons.toast
import knf.kuma.pojos.AnimeObject
import knf.kuma.videoservers.FileActions

object MultipleDownloadManager {
    private const val CHAPTER_SIZE = 160000000L
    private var index = 0
    private var chaptersList: List<AnimeObject.WebInfo.AnimeChapter> = listOf()
    var isLoading = false
    var langSelected = -1

    fun startDownload(fragment: Fragment, view: View, list: List<AnimeObject.WebInfo.AnimeChapter>, addQueue: Boolean) {
        if (list.isEmpty()) return
        if (!addQueue && !isSpaceAvailable(list.size)) {
            "Se requieren mínimo ${minSpaceString(fragment.requireContext(), list.size)} libres!".toast()
            return
        }
        clear(list)
        isLoading = true
        downloadNext(fragment, view, addQueue)
    }

    private fun downloadNext(fragment: Fragment, view: View, addQueue: Boolean) {
        if (index >= chaptersList.size || !fragment.isAdded || fragment.context == null) {
            isLoading = false
            langSelected = -1
            return
        }
        val current = chaptersList[index]
        val callback: (FileActions.CallbackState, Any?) -> Unit = { state, _ ->
            when (state) {
                FileActions.CallbackState.USER_CANCELLED,
                FileActions.CallbackState.MISSING_PERMISSION,
                FileActions.CallbackState.LOW_STORAGE,
                FileActions.CallbackState.LIFECYCLE_EXPIRED -> {
                    Log.e("MultiDownload", "Cancel processing")
                    clear(emptyList())
                }
                FileActions.CallbackState.OPERATION_RUNNING -> {
                    Log.e("MultiDownload", "Running")
                }
                else -> {
                    index++
                    downloadNext(fragment, view, addQueue)
                    Log.e("MultiDownload", "on Next")
                }
            }
        }
        if (!addQueue)
            FileActions.download(fragment.requireContext(), fragment.viewLifecycleOwner, current, view, callback)
        else
            FileActions.queuedStream(fragment.requireContext(), fragment.viewLifecycleOwner, current, view, callback)
    }

    private fun clear(list: List<AnimeObject.WebInfo.AnimeChapter>) {
        index = 0
        chaptersList = list
        isLoading = false
        langSelected = -1
    }

    private fun minSpaceString(context: Context, size: Int): String {
        return Formatter.formatFileSize(context, size * CHAPTER_SIZE)
    }

    fun isSpaceAvailable(size: Int): Boolean {
        return try {
            getAvailable() > size * CHAPTER_SIZE
        } catch (e: Exception) {
            true
        } || Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }

    private fun getAvailable(): Long {
        val stat = StatFs(FileAccessHelper.rootFile.path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }
}