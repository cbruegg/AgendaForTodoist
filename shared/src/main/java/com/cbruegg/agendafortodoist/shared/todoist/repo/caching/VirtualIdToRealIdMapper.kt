package com.cbruegg.agendafortodoist.shared.todoist.repo.caching

import android.content.Context
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

internal typealias VirtualIdToRealIdPersister = Persister<Map<Long, Long>>

private const val MAP_FILE = "virtual_ids_to_real_ids"
private const val CREATION_DATE_FILE = "virtual_ids_to_real_ids_creation_dates"
private val EXPIRY_MS = TimeUnit.DAYS.toMillis(2)

@Singleton
internal class SharedPreferencesVirtualIdToReadIdPersister
@Inject
constructor(context: Context) : VirtualIdToRealIdPersister {

    private val map = context.getSharedPreferences(MAP_FILE, Context.MODE_PRIVATE)
    private val creationDates = context.getSharedPreferences(CREATION_DATE_FILE, Context.MODE_PRIVATE)

    private var virtualIdsToRealIds: Map<Long, Long>
        get() {
            val time = System.currentTimeMillis()
            val mapEditor = map.edit()
            val creationDateEditor = creationDates.edit()
            creationDates.all.filterValues { (it as Long) + EXPIRY_MS < time }.forEach { virtualId, _ ->
                mapEditor.remove(virtualId)
                creationDateEditor.remove(virtualId)
            }
            mapEditor.apply()
            creationDateEditor.apply()

            return map.all.map { (key, value) -> (key.toLong()) to (value as Long) }.toMap()
        }
        set(value) {
            val mapEditor = map.edit()
            val creationDateEditor = creationDates.edit()
            for ((virtualId, realId) in value) {
                val virtualIdStr = virtualId.toString()
                val updated = virtualIdStr !in map || map.getLong(virtualIdStr, realId + 1) != realId
                if (updated) {
                    mapEditor.putLong(virtualIdStr, realId)
                    creationDateEditor.putLong(virtualIdStr, System.currentTimeMillis())
                }
            }
            mapEditor.apply()
            creationDateEditor.apply()
        }

    override val lockedProperty = LockProtectedVar({ virtualIdsToRealIds }, { virtualIdsToRealIds = it })
}