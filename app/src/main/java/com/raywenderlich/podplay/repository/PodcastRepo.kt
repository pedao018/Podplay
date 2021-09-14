package com.raywenderlich.podplay.repository

import androidx.lifecycle.LiveData
import com.raywenderlich.podplay.db.PodcastDao
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast
import com.raywenderlich.podplay.service.RssFeedResponse
import com.raywenderlich.podplay.service.RssFeedService

import com.raywenderlich.podplay.util.DateUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PodcastRepo(
    private var feedService: RssFeedService,
    private var podcastDao: PodcastDao
) {
    suspend fun getPodcast(feedUrl: String): Podcast? {
        val podcastLocal = podcastDao.loadPodcast(feedUrl)
        if (podcastLocal != null) {
            podcastLocal.id?.let {
                podcastLocal.episodes = podcastDao.loadEpisodes(it)
                return podcastLocal
            }
        }

        var podcast: Podcast? = null
        val feedResponse = feedService.getFeed(feedUrl)
        if (feedResponse != null) {
            podcast = rssResponseToPodcast(feedUrl, "", feedResponse)
        }
        return podcast
    }

    private fun rssResponseToPodcast(
        feedUrl: String, imageUrl: String, rssResponse: RssFeedResponse
    ): Podcast? {
        // 1
        val items = rssResponse.episodes ?: return null
        // 2
        val description = if (rssResponse.description == "")
            rssResponse.summary else rssResponse.description
        // 3
        return Podcast(
            null, feedUrl, rssResponse.title, description, imageUrl,
            rssResponse.lastUpdated, episodes = rssItemsToEpisodes(items)
        )
    }


    private fun rssItemsToEpisodes(
        episodeResponses: List<RssFeedResponse.EpisodeResponse>
    ): List<Episode> {
        return episodeResponses.map {
            Episode(
                it.guid ?: "",
                null,
                it.title ?: "",
                it.description ?: "",
                it.url ?: "",
                it.type ?: "",
                DateUtils.xmlDateToDate(it.pubDate),
                it.duration ?: ""
            )
        }
    }

    fun save(podcast: Podcast) {
        GlobalScope.launch {
            // 1
            val podcastId = podcastDao.insertPodcast(podcast)
            // 2
            for (episode in podcast.episodes) {
                // 3
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    fun delete(podcast: Podcast) {
        GlobalScope.launch {
            podcastDao.deletePodcast(podcast)
        }
    }

    fun deleteEpisodeForTestNotify() {
        GlobalScope.launch {
            podcastDao.deleteEpisodeForTestNotify()
        }
    }


    fun getAll(): LiveData<List<Podcast>> {
        return podcastDao.loadPodcasts()
    }

    private suspend fun getNewEpisodes(localPodcast: Podcast): List<Episode> {
        // 1
        val response = feedService.getFeed(localPodcast.feedUrl)
        if (response != null) {
            // 2
            val remotePodcast =
                rssResponseToPodcast(localPodcast.feedUrl, localPodcast.imageUrl, response)
            remotePodcast?.let {
                // 3
                val localEpisodes = podcastDao.loadEpisodes(localPodcast.id!!)
                // 4
                return remotePodcast.episodes.filter { episode ->
                    localEpisodes.find { episode.guid == it.guid } == null
                }
            }
        }
        // 5
        return listOf()
    }

    suspend fun updatePodcastEpisodes(): MutableList<PodcastUpdateInfo> {
        // 1
        val updatedPodcasts: MutableList<PodcastUpdateInfo> = mutableListOf()
        // 2
        val podcasts = podcastDao.loadPodcastsStatic()
        // 3
        for (podcast in podcasts) {
            // 4
            val newEpisodes = getNewEpisodes(podcast)
            // 5
            if (newEpisodes.count() > 0) {
                podcast.id?.let {
                    saveNewEpisodes(it, newEpisodes)
                    updatedPodcasts.add(
                        PodcastUpdateInfo(
                            podcast.feedUrl, podcast.feedTitle, newEpisodes.count()
                        )
                    )
                }
            }
        }
        // 6
        return updatedPodcasts
    }

    private fun saveNewEpisodes(podcastId: Long, episodes: List<Episode>) {
        GlobalScope.launch {
            for (episode in episodes) {
                episode.podcastId = podcastId
                podcastDao.insertEpisode(episode)
            }
        }
    }

    class PodcastUpdateInfo(
        val feedUrl: String,
        val name: String,
        val newCount: Int
    )

}
