package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.isAccessible

internal interface Persister<T> {
    val lockedProperty: LockProtectedVar<T>
}

internal class LockProtectedVar<T>(private var t: KMutableProperty<T>) {

    private val lock = Mutex()

    init {
        t.getter.isAccessible = true
        t.setter.isAccessible = true
    }

    suspend fun inTransaction(f: suspend (T) -> T): Unit = inTransactionWithReturn {
        f(it) to Unit
    }

    suspend fun <R> inTransactionWithReturn(f: suspend (T) -> Pair<T, R>): R {
        return lock.withLock {
            val (newT, r) = f(t.call())
            t.setter.call(newT)
            r
        }
    }

    suspend fun <R> readOnly(f: suspend (T) -> R): R {
        return inTransactionWithReturn {
            val r = f(it)
            it to r
        }
    }
}