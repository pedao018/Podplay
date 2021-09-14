package com.raywenderlich.podplay.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.raywenderlich.podplay.model.Episode
import com.raywenderlich.podplay.model.Podcast

// 1
@Dao
interface PodcastDao {
    // 2
    /*
    loadPodcasts() loads all of the podcasts from the database and returns a LiveData object.
    The @Query annotation is defined to select all podcasts and sort them by their title in ascending order.
    Note the lack of the use of the suspend keyword for this function, while each of the other functions has the suspend keyword.
    Coroutines are built right into Room and together they are both powerful and easy to use.
    All the database calls will be happening asynchronously but your code is written in a synchronous fashion.
    However, LiveData is already using suspend, so in defining a function in your DAO that returns a LiveData, you don’t need to use the suspend keyword as it’s redundant.
    In fact, if you forget and use it by accident, your app will not compile and you will get a strange-looking, hard to decipher error message like this:
    - A failure occurred while executing org.jetbrains.kotlin.gradle.internal.KaptExecution
    - java.lang.reflect.InvocationTargetException (no error message)
    * */
    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcasts(): LiveData<List<Podcast>>

    // 3
    @Query("SELECT * FROM Episode WHERE podcastId = :podcastId ORDER BY releaseDate DESC")
    suspend fun loadEpisodes(podcastId: Long): List<Episode>

    @Query("SELECT * FROM Podcast WHERE feedUrl = :url")
    suspend fun loadPodcast(url: String): Podcast?

    // 4
    /*
    insertPodcast() inserts a single podcast into the database.
    No SQL statement is required on the @Insert annotation.
    onConflict is set to REPLACE to tell Room to replace the old record if a record with the same primary key already exists in the database.
    **/
    @Insert(onConflict = REPLACE)
    suspend fun insertPodcast(podcast: Podcast): Long

    // 5
    @Insert(onConflict = REPLACE)
    suspend fun insertEpisode(episode: Episode): Long

    @Delete
    fun deletePodcast(podcast: Podcast)

    @Query("SELECT * FROM Podcast ORDER BY FeedTitle")
    fun loadPodcastsStatic(): List<Podcast>


    @Query(
        """
            DELETE FROM Episode WHERE guid IN
            (
                SELECT * FROM (SELECT guid FROM Episode WHERE PodCastID IN (SELECT ID FROM Podcast WHERE feedUrl IN ('https://androidcentral.libsyn.com/rss')) LIMIT 5) as a
                UNION
                SELECT * FROM (SELECT guid FROM Episode WHERE PodCastID IN (SELECT ID FROM Podcast WHERE feedUrl IN ('https://www.raywenderlich.com/feed/podcast')) LIMIT 5) as b
            )
            """
    )
    suspend fun deleteEpisodeForTestNotify()


}
