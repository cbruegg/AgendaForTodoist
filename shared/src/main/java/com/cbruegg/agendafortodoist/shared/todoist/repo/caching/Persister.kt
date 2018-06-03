package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

internal interface Persister<T> {
    suspend fun inTransaction(f: suspend (T) -> T): Unit = inTransactionWithReturn {
        f(it) to Unit
    }

    suspend fun <R> inTransactionWithReturn(f: suspend (T) -> Pair<T, R>): R

    suspend fun <R> readOnly(f: suspend (T) -> R): R {
        return inTransactionWithReturn {
            val r = f(it)
            it to r
        }
    }
}