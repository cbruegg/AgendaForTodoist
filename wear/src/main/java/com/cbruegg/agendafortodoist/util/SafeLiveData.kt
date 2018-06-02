package com.cbruegg.agendafortodoist.util

import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer

open class LiveData<T>(initialValue: T) : LiveData<T>() {

    open val data: T
        get() = value

    init {
        value = initialValue
    }

    override fun getValue(): T = super.getValue()!!
}

open class MutableLiveData<T>(initialValue: T) : com.cbruegg.agendafortodoist.util.LiveData<T>(initialValue) {
    override var data: T
        get() = value
        set(x) {
            value = x
        }
}

inline fun <reified T> observer(crossinline f: ((T) -> Unit)): Observer<T> = Observer<T> {
    f(it as T)
}

inline fun <reified T> LiveData<T>.observe(lifecycleOwner: LifecycleOwner, crossinline f: (T) -> Unit): Observer<T> =
        observer<T> { f(it) }.also {
            observe(lifecycleOwner, it)
        }