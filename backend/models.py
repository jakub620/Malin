from pydantic import BaseModel
from typing import Optional
from datetime import datetime



class PatientCreate(BaseModel):
    name:  str
    age:   Optional[int]   = None
    ward:  Optional[str]   = None
    notes: Optional[str]   = None

class PatientUpdate(BaseModel):
    name:  Optional[str]   = None
    age:   Optional[int]   = None
    ward:  Optional[str]   = None
    notes: Optional[str]   = None

class LocationInfo(BaseModel):
    floor:          Optional[int]   = None
    location_label: Optional[str]  = None
    camera_id:      Optional[str]  = None
    timestamp:      Optional[datetime] = None
    lat:            Optional[float] = None
    lng:            Optional[float] = None

class PatientOut(BaseModel):
    id:         int
    name:       str
    age:        Optional[int]
    ward:       Optional[str]
    notes:      Optional[str]
    tag_id:     Optional[str]
    created_at: datetime
    location:   Optional[LocationInfo] = None  # ostatnia znana lokalizacja

    class Config:
        from_attributes = True



class TagOut(BaseModel):
    id:          str
    patient_id:  Optional[int]
    assigned_at: Optional[datetime]
    is_free:     bool

    class Config:
        from_attributes = True



class LocationUpdate(BaseModel):
    tag_id:         str            # np. "TAG_001"
    floor:          Optional[int]  = None
    location_label: Optional[str] = None
    camera_id:      Optional[str] = None
    lat:            Optional[float] = None
    lng:            Optional[float] = None



class AssignTagRequest(BaseModel):
    tag_id: str
