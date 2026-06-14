"""
Hospital Patient Location Backend
FastAPI + sqlite3 (wbudowany w Python – zero dodatkowych zależności)

Uruchom: uvicorn main:app --host 0.0.0.0 --port 8000 --reload
Dokumentacja: http://localhost:8000/docs
"""

import asyncio
import json
from datetime import datetime
from typing import AsyncGenerator, List, Optional

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse

from database import get_conn, init_db
from models import (
    AssignTagRequest, LocationInfo, LocationUpdate,
    PatientCreate, PatientOut, PatientUpdate, TagOut,
)

app = FastAPI(title="Hospital Location API", version="1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Zdarzenia SSE
_sse_queues: list[asyncio.Queue] = []


async def _broadcast(event_data: dict):
    msg = json.dumps(event_data, default=str)
    for q in list(_sse_queues):
        await q.put(msg)


@app.on_event("startup")
def startup():
    init_db()



def _row_to_patient(row, last_loc=None) -> PatientOut:
    loc = None
    if last_loc:
        loc = LocationInfo(
            floor=last_loc["floor"],
            location_label=last_loc["location_label"],
            camera_id=last_loc["camera_id"],
            timestamp=last_loc["timestamp"],
            lat=last_loc["lat"],
            lng=last_loc["lng"],
        )
    return PatientOut(
        id=row["id"],
        name=row["name"],
        age=row["age"],
        ward=row["ward"],
        notes=row["notes"],
        tag_id=row["tag_id"],
        created_at=row["created_at"],
        location=loc,
    )


def _get_last_location(conn, patient_id: int):
    return conn.execute(
        """SELECT floor, location_label, camera_id, lat, lng, timestamp
           FROM location_events
           WHERE patient_id = ?
           ORDER BY timestamp DESC LIMIT 1""",
        (patient_id,),
    ).fetchone()



@app.get("/patients", response_model=List[PatientOut])
def list_patients():
    with get_conn() as conn:
        rows = conn.execute(
            "SELECT * FROM patients ORDER BY created_at DESC"
        ).fetchall()
        return [_row_to_patient(r, _get_last_location(conn, r["id"])) for r in rows]


@app.post("/patients", response_model=PatientOut, status_code=201)
def create_patient(data: PatientCreate):
    with get_conn() as conn:
        cur = conn.execute(
            "INSERT INTO patients (name, age, ward, notes) VALUES (?,?,?,?)",
            (data.name, data.age, data.ward, data.notes),
        )
        conn.commit()
        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (cur.lastrowid,)
        ).fetchone()
        return _row_to_patient(row)


@app.get("/patients/{patient_id}", response_model=PatientOut)
def get_patient(patient_id: int):
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        if not row:
            raise HTTPException(404, "Pacjent nie znaleziony")
        return _row_to_patient(row, _get_last_location(conn, patient_id))


@app.put("/patients/{patient_id}", response_model=PatientOut)
def update_patient(patient_id: int, data: PatientUpdate):
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        if not row:
            raise HTTPException(404, "Pacjent nie znaleziony")

        fields = data.model_dump(exclude_unset=True)
        if fields:
            sets = ", ".join(f"{k} = ?" for k in fields)
            conn.execute(
                f"UPDATE patients SET {sets} WHERE id = ?",
                (*fields.values(), patient_id),
            )
            conn.commit()

        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        return _row_to_patient(row, _get_last_location(conn, patient_id))


@app.delete("/patients/{patient_id}", status_code=204)
def delete_patient(patient_id: int):
    with get_conn() as conn:
        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        if not row:
            raise HTTPException(404, "Pacjent nie znaleziony")
        # Zwolnij tag
        if row["tag_id"]:
            conn.execute(
                "UPDATE tags SET patient_id = NULL, assigned_at = NULL WHERE id = ?",
                (row["tag_id"],),
            )
        conn.execute("DELETE FROM patients WHERE id = ?", (patient_id,))
        conn.commit()



@app.post("/patients/{patient_id}/assign-tag", response_model=PatientOut)
def assign_tag(patient_id: int, req: AssignTagRequest):
    with get_conn() as conn:
        patient = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        if not patient:
            raise HTTPException(404, "Pacjent nie znaleziony")

        tag = conn.execute(
            "SELECT * FROM tags WHERE id = ?", (req.tag_id,)
        ).fetchone()
        if not tag:
            conn.execute(
                "INSERT INTO tags (id, patient_id, assigned_at) VALUES (?,?,?)",
                (req.tag_id, None, None)
            )
            tag = conn.execute(
                "SELECT * FROM tags WHERE id = ?", (req.tag_id,)
            ).fetchone()
        if tag["patient_id"] and tag["patient_id"] != patient_id:
            raise HTTPException(
                400, f"Tag {req.tag_id} jest już przypisany do pacjenta {tag['patient_id']}"
            )

        # Odepnij poprzedni tag pacjenta
        if patient["tag_id"] and patient["tag_id"] != req.tag_id:
            conn.execute(
                "UPDATE tags SET patient_id = NULL, assigned_at = NULL WHERE id = ?",
                (patient["tag_id"],),
            )

        now = datetime.utcnow().isoformat()
        conn.execute(
            "UPDATE tags SET patient_id = ?, assigned_at = ? WHERE id = ?",
            (patient_id, now, req.tag_id),
        )
        conn.execute(
            "UPDATE patients SET tag_id = ? WHERE id = ?",
            (req.tag_id, patient_id),
        )
        conn.commit()

        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        return _row_to_patient(row, _get_last_location(conn, patient_id))


@app.post("/patients/{patient_id}/unassign-tag", response_model=PatientOut)
def unassign_tag(patient_id: int):
    with get_conn() as conn:
        patient = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        if not patient:
            raise HTTPException(404, "Pacjent nie znaleziony")
        if patient["tag_id"]:
            conn.execute(
                "UPDATE tags SET patient_id = NULL, assigned_at = NULL WHERE id = ?",
                (patient["tag_id"],),
            )
            conn.execute(
                "UPDATE patients SET tag_id = NULL WHERE id = ?", (patient_id,)
            )
            conn.commit()
        row = conn.execute(
            "SELECT * FROM patients WHERE id = ?", (patient_id,)
        ).fetchone()
        return _row_to_patient(row)



@app.get("/tags", response_model=List[TagOut])
def list_tags():
    with get_conn() as conn:
        rows = conn.execute("SELECT * FROM tags").fetchall()
        return [
            TagOut(
                id=r["id"],
                patient_id=r["patient_id"],
                assigned_at=r["assigned_at"],
                is_free=(r["patient_id"] is None),
            )
            for r in rows
        ]


@app.get("/tags/free", response_model=List[TagOut])
def list_free_tags():
    with get_conn() as conn:
        rows = conn.execute(
            "SELECT * FROM tags WHERE patient_id IS NULL"
        ).fetchall()
        return [
            TagOut(id=r["id"], patient_id=None, assigned_at=None, is_free=True)
            for r in rows
        ]



@app.post("/location/update")
async def update_location(data: LocationUpdate):
    with get_conn() as conn:
        tag = conn.execute(
            "SELECT * FROM tags WHERE id = ?", (data.tag_id,)
        ).fetchone()
        if not tag:
            conn.execute(
                "INSERT INTO tags (id, patient_id, assigned_at) VALUES (?,?,?)",
                (data.tag_id, None, None)
            )
            tag = conn.execute(
                "SELECT * FROM tags WHERE id = ?", (data.tag_id,)
            ).fetchone()

        patient_id = tag["patient_id"]
        now = datetime.utcnow().isoformat()

        conn.execute(
            """INSERT INTO location_events
               (patient_id, tag_id, floor, location_label, camera_id, lat, lng, timestamp)
               VALUES (?,?,?,?,?,?,?,?)""",
            (patient_id, data.tag_id, data.floor,
             data.location_label, data.camera_id,
             data.lat, data.lng, now),
        )
        conn.commit()

    await _broadcast({
        "event":          "location_update",
        "tag_id":         data.tag_id,
        "patient_id":     patient_id,
        "floor":          data.floor,
        "location_label": data.location_label,
        "camera_id":      data.camera_id,
        "lat":            data.lat,
        "lng":            data.lng,
        "timestamp":      now,
    })

    return {"ok": True, "patient_id": patient_id}



@app.get("/events")
async def sse_stream():
    queue: asyncio.Queue = asyncio.Queue()
    _sse_queues.append(queue)

    async def generator() -> AsyncGenerator[str, None]:
        try:
            yield 'data: {"event":"connected"}\n\n'
            while True:
                try:
                    msg = await asyncio.wait_for(queue.get(), timeout=25.0)
                    yield f"data: {msg}\n\n"
                except asyncio.TimeoutError:
                    yield ": keepalive\n\n"
        finally:
            if queue in _sse_queues:
                _sse_queues.remove(queue)

    return StreamingResponse(
        generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )



@app.get("/")
def root():
    return {
        "status":           "ok",
        "service":          "Hospital Location API",
        "docs":             "/docs",
        "connected_clients": len(_sse_queues),
    }
