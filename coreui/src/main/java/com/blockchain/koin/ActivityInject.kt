package com.blockchain.koin

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.koin.android.ext.android.inject

const val ACTIVITY_PARAMETER = "_param_activity"

inline fun <reified T> AppCompatActivity.injectActivity(): Lazy<T> =
    inject(parameters = { toInjectionParameters() })

inline fun <reified T> Fragment.injectActivity(): Lazy<T> =
    inject(parameters = { this.activity!!.toInjectionParameters() })

fun AppCompatActivity.toInjectionParameters() = mapOf(ACTIVITY_PARAMETER to this)
fun FragmentActivity.toInjectionParameters() = mapOf(ACTIVITY_PARAMETER to this)
