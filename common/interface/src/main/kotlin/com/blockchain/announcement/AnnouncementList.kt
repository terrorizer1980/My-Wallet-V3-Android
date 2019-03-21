package com.blockchain.announcement

import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler

class AnnouncementList<Context> {

    private val list = mutableListOf<Announcement<Context>>()

    fun add(announcement: Announcement<Context>): AnnouncementList<Context> {
        list.add(announcement)
        return this
    }

    fun showNextAnnouncement(context: Context, scheduler: Scheduler): Maybe<Announcement<Context>> =
        getNextAnnouncement(context)
            .observeOn(scheduler)
            .doOnSuccess { it.show(context) }

    private fun getNextAnnouncement(context: Context): Maybe<Announcement<Context>> =
        Observable.concat(
            list.map { a ->
                Observable.defer {
                    a.shouldShow(context)
                        .filter { it }
                        .map { a }
                        .toObservable()
                }
            }
        ).firstElement()
}
