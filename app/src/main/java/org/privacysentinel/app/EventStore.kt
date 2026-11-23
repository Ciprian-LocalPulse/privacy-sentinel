package org.privacysentinel.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object SensorKind {
    const val CAMERA = "camera"
    const val MIC = "mic"
}

object EventState {
    const val START = "start"
    const val STOP = "stop"
}

data class SensorEvent(
    val id: Long,
    val timestamp: Long,
    val sensor: String,
    val state: String,
    val app: String?,
    val screenOn: Boolean,
    val suspicious: Boolean
)

/** Local SQLite log of camera / microphone events. Never leaves the device. */
class EventStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "events.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ts INTEGER NOT NULL,
                sensor TEXT NOT NULL,
                state TEXT NOT NULL,
                app TEXT,
                screen_on INTEGER NOT NULL,
                suspicious INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Nothing to migrate yet.
    }

    @Synchronized
    fun insert(
        sensor: String,
        state: String,
        app: String?,
        screenOn: Boolean,
        suspicious: Boolean
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("ts", System.currentTimeMillis())
            put("sensor", sensor)
            put("state", state)
            put("app", app)
            put("screen_on", if (screenOn) 1 else 0)
            put("suspicious", if (suspicious) 1 else 0)
        }
        db.insert("events", null, values)
        db.execSQL(
            "DELETE FROM events WHERE id NOT IN " +
                "(SELECT id FROM events ORDER BY id DESC LIMIT $MAX_ROWS)"
        )
        changes.update { it + 1 }
    }

    @Synchronized
    fun recent(limit: Int = 500): List<SensorEvent> {
        val out = ArrayList<SensorEvent>()
        readableDatabase.query(
            "events", null, null, null, null, null, "id DESC", limit.toString()
        ).use { c ->
            val iId = c.getColumnIndexOrThrow("id")
            val iTs = c.getColumnIndexOrThrow("ts")
            val iSensor = c.getColumnIndexOrThrow("sensor")
            val iState = c.getColumnIndexOrThrow("state")
            val iApp = c.getColumnIndexOrThrow("app")
            val iScreen = c.getColumnIndexOrThrow("screen_on")
            val iSusp = c.getColumnIndexOrThrow("suspicious")
            while (c.moveToNext()) {
                out.add(
                    SensorEvent(
                        id = c.getLong(iId),
                        timestamp = c.getLong(iTs),
                        sensor = c.getString(iSensor),
                        state = c.getString(iState),
                        app = if (c.isNull(iApp)) null else c.getString(iApp),
                        screenOn = c.getInt(iScreen) == 1,
                        suspicious = c.getInt(iSusp) == 1
                    )
                )
            }
        }
        return out
    }

    @Synchronized
    fun clear() {
        writableDatabase.execSQL("DELETE FROM events")
        changes.update { it + 1 }
    }

    companion object {
        private const val MAX_ROWS = 5000

        /** Incremented on every change so the UI can refresh. */
        val changes = MutableStateFlow(0)

        @Volatile
        private var instance: EventStore? = null

        fun get(context: Context): EventStore =
            instance ?: synchronized(this) {
                instance ?: EventStore(context).also { instance = it }
            }
    }
}
