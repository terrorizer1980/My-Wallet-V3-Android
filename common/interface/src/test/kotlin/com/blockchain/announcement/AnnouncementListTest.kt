package com.blockchain.announcement

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`it throws`
import org.junit.Test

class AnnouncementListTest {

    @Test
    fun `calls no announcements until subscribed`() {
        AnnouncementList<X>()
            .add(dontCheckAnnouncement())
            .add(dontShowAnnouncement())
            .showNextAnnouncement(X(), Schedulers.trampoline())
    }

    @Test
    fun `calls first announcement that says it should show`() {
        val announcement = announcement()
        val x = X()
        AnnouncementList<X>()
            .add(dontShowAnnouncement())
            .add(announcement)
            .showNextAnnouncement(x, Schedulers.trampoline())
            .test()
            .assertValue(announcement)
            .assertComplete()
            .assertNoErrors()
        verify(announcement).show(x)
    }

    @Test
    fun `nothing available`() {
        AnnouncementList<X>()
            .add(dontShowAnnouncement())
            .add(dontShowAnnouncement())
            .add(dontShowAnnouncement())
            .showNextAnnouncement(X(), Schedulers.trampoline())
            .test()
            .assertValues()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `does not check announcements beyond one that says it should show`() {
        val announcement = announcement()
        val x = X()
        AnnouncementList<X>()
            .add(announcement)
            .add(dontCheckAnnouncement())
            .showNextAnnouncement(x, Schedulers.trampoline())
            .test()
            .assertValue(announcement)
            .assertComplete()
            .assertNoErrors()
        verify(announcement).show(x)
    }

    @Test
    fun `calls first announcement that says it should show - alternative scheduler`() {
        val announcement = announcement()
        val scheduler = TestScheduler()
        val x = X()
        val test = AnnouncementList<X>()
            .add(dontShowAnnouncement())
            .add(announcement)
            .showNextAnnouncement(x, scheduler)
            .test()
            .assertValues()
            .assertNoErrors()
            .assertNotComplete()
        verify(announcement, never()).show(x)
        scheduler.triggerActions()
        verify(announcement).show(x)
        test
            .assertValue(announcement)
            .assertComplete()
            .assertNoErrors()
    }
}

private class X

private fun announcement(): Announcement<X> =
    mock {
        on { shouldShow(any()) } `it returns` Single.just(true)
    }

private fun dontShowAnnouncement(): Announcement<X> =
    mock {
        on { shouldShow(any()) } `it returns` Single.just(false)
        on { show(any()) } `it throws` RuntimeException("Not expected")
    }

private fun dontCheckAnnouncement(): Announcement<X> =
    mock {
        on { shouldShow(any()) } `it throws` RuntimeException("Not expected")
        on { show(any()) } `it throws` RuntimeException("Not expected")
    }
