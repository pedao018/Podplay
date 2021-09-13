package com.raywenderlich.podplay.repository

import com.raywenderlich.podplay.service.ItunesService

// 1
/*
You define the primary constructor for ItunesRepo to require an existing instance of the ItunesService interface.
This is an example of the Dependency Injection principle.
By passing an ItunesService to ItunesRepo, it makes it possible for the calling code to pass a different implementation for ItunesService.
ItuneRepo doesn’t care about the implementation, as long as it conforms to the ItunesService interface.
* */
class ItunesRepo(private val itunesService: ItunesService) {
    // 2
    suspend fun searchByTerm(term: String) = itunesService.searchPodcastByTerm(term) // 3
}
