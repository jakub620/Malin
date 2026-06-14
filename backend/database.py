"""
Warstwa bazy danych – czysty sqlite3 (wbudowany w Python, zero instalacji)
"""
import sqlite3
import os
from datetime import datetime

DB_PATH = os.path.join(os.path.dirname(__file__), "hospital.db")


def get_conn() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row   # dostęp do kolumn po nazwie
    conn.execute("PRAGMA journal_mode=WAL")
    return conn


def init_db():
    with get_conn() as conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS patients (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                name        TEXT    NOT NULL,
                age         INTEGER,
                ward        TEXT,
                notes       TEXT,
                tag_id      TEXT,
                created_at  TEXT    DEFAULT (datetime('now'))
            );

            CREATE TABLE IF NOT EXISTS tags (
                id           TEXT PRIMARY KEY,
                patient_id   INTEGER,
                assigned_at  TEXT
            );

            CREATE TABLE IF NOT EXISTS location_events (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                patient_id     INTEGER,
                tag_id         TEXT NOT NULL,
                floor          INTEGER,
                location_label TEXT,
                camera_id      TEXT,
                lat            REAL,
                lng            REAL,
                timestamp      TEXT DEFAULT (datetime('now'))
            );
        """)

        # Dodaj kolumny lat/lng jeśli brak (migracja starego DB)
        for col in ("lat", "lng"):
            try:
                conn.execute(f"ALTER TABLE location_events ADD COLUMN {col} REAL")
            except Exception:
                pass  # kolumna już istnieje

        # Inicjalizuj 5 tagów jeśli jeszcze nie ma
        for i in range(1, 6):
            tag_id = f"TAG_{i:03d}"
            exists = conn.execute(
                "SELECT 1 FROM tags WHERE id = ?", (tag_id,)
            ).fetchone()
            if not exists:
                conn.execute(
                    "INSERT INTO tags (id, patient_id, assigned_at) VALUES (?,?,?)",
                    (tag_id, None, None)
                )
        conn.commit()
