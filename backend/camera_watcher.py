"""
camera_watcher.py – System kamery QR
Uruchom na komputerze/telefonie z kamerą.
Ciągle skanuje obraz i wysyła lokalizację do backendu gdy wykryje tag QR.

Wymagania: pip install opencv-python pyzbar requests

Konfiguracja: zmień zmienne poniżej przed uruchomieniem.
"""

import time
import requests
import cv2
from pyzbar import pyzbar

# Konfiguracja serwera i kamery

SERVER_URL      = "http://127.0.0.1:8001"        # Adres serwera
CAMERA_INDEX    = 0                              # Indeks kamery
CAMERA_ID       = "kamera_glowna"               # ID kamery
FLOOR           = 1                             # Pietro
LOCATION_LABEL  = "Wejście główne"              # Nazwa lokalizacji

COOLDOWN_SECONDS = 8   # Cooldown


def send_location(tag_id: str):
    """Wyślij lokalizację do backendu."""
    try:
        resp = requests.post(
            f"{SERVER_URL}/location/update",
            json={
                "tag_id":         tag_id,
                "floor":          FLOOR,
                "location_label": LOCATION_LABEL,
                "camera_id":      CAMERA_ID,
            },
            timeout=3,
        )
        if resp.ok:
            data = resp.json()
            patient_id = data.get("patient_id")
            if patient_id:
                print(f"[OK] Tag {tag_id} → Pacjent ID {patient_id} | {LOCATION_LABEL}, piętro {FLOOR}")
            else:
                print(f"[OK] Tag {tag_id} → (brak przypisanego pacjenta) | {LOCATION_LABEL}")
        else:
            print(f"[BŁĄD] Serwer odpowiedział: {resp.status_code}")
    except requests.exceptions.ConnectionError:
        print(f"[BŁĄD] Brak połączenia z serwerem {SERVER_URL}")
    except Exception as e:
        print(f"[BŁĄD] {e}")


def main():
    print(f"Uruchamianie kamery #{CAMERA_INDEX}...")
    print(f"Serwer: {SERVER_URL}")
    print(f"Kamera: {CAMERA_ID} | Piętro: {FLOOR} | Lokalizacja: {LOCATION_LABEL}")
    print("Naciśnij 'q' żeby zatrzymać.\n")

    cap = cv2.VideoCapture(CAMERA_INDEX)
    if not cap.isOpened():
        print(f"Nie można otworzyć kamery #{CAMERA_INDEX}!")
        return

    # Cache wyslanych tagow
    last_sent: dict[str, float] = {}

    while True:
        ret, frame = cap.read()
        if not ret:
            print("Brak klatki z kamery.")
            time.sleep(0.5)
            continue

        # Wykrywanie kodow QR
        codes = pyzbar.decode(frame)

        for code in codes:
            raw_text = code.data.decode("utf-8").strip()

            # Pobranie ID z kodu QR
            tag_id = raw_text.split("/")[-1] if "/" in raw_text else raw_text

            # Sprawdzenie cooldownu
            now = time.time()
            if now - last_sent.get(tag_id, 0) < COOLDOWN_SECONDS:
                continue

            last_sent[tag_id] = now
            send_location(tag_id)

            # Rysowanie ramki na obrazie
            pts = code.polygon
            if len(pts) == 4:
                for i in range(4):
                    cv2.line(frame, pts[i], pts[(i+1) % 4], (0, 255, 0), 2)
            cv2.putText(frame, tag_id, (pts[0].x, pts[0].y - 10),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

        # Podglad z kamery
        cv2.imshow("Kamera QR – naciśnij q żeby wyjść", frame)
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()


if __name__ == "__main__":
    main()
