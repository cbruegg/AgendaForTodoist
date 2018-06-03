package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock

internal interface Persister<T> {
    val lockedProperty: LockProtectedVar<T>
}

internal class LockProtectedVar<T>(private val getter: () -> T, private val setter: (T) -> Unit) {

    private val lock = Mutex()

    suspend fun inTransaction(f: suspend (T) -> T): Unit = inTransactionWithReturn {
        f(it) to Unit
    }

    suspend fun <R> inTransactionWithReturn(f: suspend (T) -> Pair<T, R>): R {
        return lock.withLock {
            val (newT, r) = f(getter())
            setter(newT)
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