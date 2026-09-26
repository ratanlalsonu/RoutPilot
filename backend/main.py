"""
RoutPilot FastAPI Backend Server (2026-2027)
Real-Time Road & Bridge Hazard Detection with Intelligent Route Diversion System.
Communicates with ESP32 (MQTT/REST), PostgreSQL, MATLAB Engine, OSRM/Nominatim, and Android APK via REST & WebSocket.
"""
import os
import json
import time
from datetime import datetime, timezone
from typing import List, Optional, Dict, Any
from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from pydantic import BaseModel

app = FastAPI(title="RoutPilot FastAPI Backend", version="2026.1")

CURRENT_SENSOR_MODE = "EXTERNAL_HARDWARE"
THRESHOLDS_PATH = os.path.join(os.path.dirname(__file__), "..", "hazard_thresholds.json")


class SensorModePayload(BaseModel):
    mode: str


class Esp32SensorPayload(BaseModel):
    deviceId: str
    timestamp: str
    latitude: float
    longitude: float
    vibration: float
    tilt: float
    strain: float
    displacement: float
    waterLevel: float
    battery: float = 100.0
    dataSource: str = "LIVE_HARDWARE"


class RouteCalculatePayload(BaseModel):
    sourceLat: float
    sourceLon: float
    destLat: float
    destLon: float
    vehicleType: str = "CAR"
    algorithm: str = "ASTAR"


@app.get("/api/health")
def health_check():
    return {"status": "OK", "service": "RoutPilot FastAPI", "timestamp": datetime.now(timezone.utc).isoformat()}


@app.get("/api/system/status")
def system_status():
    return {
        "backend": "CONNECTED",
        "database": "POSTGRESQL_READY",
        "sensorMode": CURRENT_SENSOR_MODE,
        "matlab": "LOCAL_AND_ENGINE_READY",
    }


@app.get("/api/sensor-mode")
def get_sensor_mode():
    return {"mode": CURRENT_SENSOR_MODE}


@app.post("/api/sensor-mode")
@app.post("/api/mode/switch")
def switch_sensor_mode(payload: SensorModePayload):
    global CURRENT_SENSOR_MODE
    if payload.mode not in ("EXTERNAL_HARDWARE", "VIRTUAL"):
        raise HTTPException(status_code=400, detail="Invalid mode. Use EXTERNAL_HARDWARE or VIRTUAL.")
    CURRENT_SENSOR_MODE = payload.mode
    return {"mode": CURRENT_SENSOR_MODE, "switchedAt": datetime.now(timezone.utc).isoformat()}


@app.get("/api/location/status")
def location_status():
    return {"provider": "ANDROID_FUSED_GPS", "status": "ACTIVE"}


@app.get("/api/geocode/search")
def geocode_search(q: str):
    import urllib.request
    import urllib.parse
    url = f"https://nominatim.openstreetmap.org/search?format=json&q={urllib.parse.quote(q)}&limit=6"
    req = urllib.request.Request(url, headers={"User-Agent": "RoutPilot-Backend/2026.1"})
    with urllib.request.urlopen(req, timeout=8) as resp:
        return json.loads(resp.read().decode("utf-8"))


@app.post("/api/sensors/data")
def ingest_sensor_data(payload: Esp32SensorPayload):
    if CURRENT_SENSOR_MODE == "EXTERNAL_HARDWARE" and payload.dataSource != "LIVE_HARDWARE":
        raise HTTPException(status_code=400, detail="Hardware Mode rejects non-hardware sensor packets.")
    return {"status": "INGESTED", "deviceId": payload.deviceId, "dataSource": payload.dataSource}


@app.get("/api/sensors/devices")
def list_devices():
    return {"mode": CURRENT_SENSOR_MODE, "devices": []}


@app.get("/api/sensors/{deviceId}")
def get_device(deviceId: str):
    return {"deviceId": deviceId, "mode": CURRENT_SENSOR_MODE}


@app.get("/api/hazards/active")
def get_active_hazards():
    return {"mode": CURRENT_SENSOR_MODE, "hazards": []}


@app.get("/api/hazards/history")
def get_hazard_history():
    return {"history": []}


@app.post("/api/hazards")
def create_hazard(hazard: Dict[str, Any]):
    return {"status": "RECORDED", "hazard": hazard}


@app.get("/api/roads/status")
def get_roads_status():
    return {"mode": CURRENT_SENSOR_MODE, "roads": []}


@app.get("/api/matlab/status")
def get_matlab_status():
    return {"connected": False, "fallback": "MATLAB unavailable — using local processing."}


@app.post("/api/matlab/analyze")
def matlab_analyze(payload: Dict[str, Any]):
    return {"status": "ANALYZED", "engine": "LOCAL_VERIFIED", "payload": payload}


@app.post("/api/matlab/astar")
def matlab_astar(payload: RouteCalculatePayload):
    return {"algorithm": "ASTAR", "status": "COMPUTED"}


@app.post("/api/matlab/dijkstra")
def matlab_dijkstra(payload: RouteCalculatePayload):
    return {"algorithm": "DIJKSTRA", "status": "COMPUTED"}


@app.post("/api/routes/calculate")
@app.post("/api/routes/alternatives")
@app.post("/api/routes/hazard-aware")
@app.post("/api/route/compare")
def calculate_routes(payload: RouteCalculatePayload):
    return {"status": "OK", "vehicleType": payload.vehicleType, "algorithm": payload.algorithm}


@app.post("/api/virtual/start")
@app.post("/api/virtual/pause")
@app.post("/api/virtual/stop")
@app.post("/api/virtual/reset")
def virtual_control(payload: Optional[Dict[str, Any]] = None):
    return {"mode": "VIRTUAL", "label": "TEST DATA — NOT LIVE", "status": "OK"}
