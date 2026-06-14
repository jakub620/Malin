# System lokalizacji pacjentów (projekt "MALIN")

Projekt systemu monitorowania i lokalizacji pacjentów wewnątrz budynków szpitalnych na podstawie kodów QR. Aplikacja mobilna (Android) integruje się z serwerem mapowym CENAGIS (nakładka pięter na OpenStreetMap) oraz lokalnym serwerem backendowym (FastAPI), który odbiera dane z kamer skanujących kody QR.

## Struktura projektu

* **app/** – projekt aplikacji mobilnej w Android Studio (Kotlin, Jetpack Compose, Retrofit, OSMDroid, Proj4J).
* **backend/** – serwer lokalny napisany w Pythonie (FastAPI + SQLite).
  * `main.py` – API serwera obsługujące rejestrację lokalizacji i pacjentów.
  * `database.py` – obsługa lokalnej bazy danych SQLite.
  * `camera_watcher.py` – skrypt symulujący kamerę (skanuje kody QR z kamery komputera i wysyła pozycję na serwer).

---

## Uruchomienie projektu

### 1. Uruchomienie Backend (Python)

Backend wymaga Pythona 3.8+.
1. Wejdź do katalogu `backend/`.
2. Zainstaluj wymagane biblioteki:
   ```bash
   pip install -r requirements.txt
   ```
3. Uruchom serwer API za pomocą uvicorn:
   ```bash
   uvicorn main:app --host 0.0.0.0 --port 8001 --reload
   ```

### 2. Uruchomienie Kamery (Symulator)

Aby uruchomić skrypt kamery skanującej kody QR:
1. Podłącz kamerkę internetową do komputera.
2. W pliku `backend/camera_watcher.py` możesz skonfigurować lokalizację kamery (`FLOOR` i `LOCATION_LABEL`).
3. Uruchom skrypt:
   ```bash
   python camera_watcher.py
   ```

### 3. Konfiguracja i uruchomienie aplikacji Android

1. Otwórz katalog główny projektu w **Android Studio**.
2. **Kluczowa konfiguracja IP:**
   Aplikacja łączy się z backendem uruchomionym na komputerze. Adres IP serwera jest zdefiniowany w pliku:
   `app/src/main/java/com/example/szpital/data/api/HospitalRetrofitClient.kt`
   
   * Domyślnie ustawiony jest IP `192.168.137.1` (dla telefonu podpiętego pod Hotspot Windows).
   * Jeśli uruchamiasz aplikację na **emulatorze w Android Studio**, musisz zmienić ten adres na `"http://10.0.2.2:8001/"`.
   * Jeśli telefon i komputer są w tej samej sieci Wi-Fi, zmień adres na lokalne IP komputera (np. `192.168.1.X`).
3. Skompiluj i uruchom aplikację na telefonie lub emulatorze.

---

## Główne funkcjonalności

1. **Wyszukiwanie lokalizacji online i offline:** Aplikacja odpytuje serwer CENAGIS o współrzędne kodu QR (w układzie EPSG:2180, które w runtime konwertuje do WGS84 przy użyciu biblioteki Proj4J). W razie braku połączenia korzysta z wbudowanej bazy offline (215 kodów Gmachu Głównego).
2. **Podkład mapowy:** Wyświetlanie planu pięter bezpośrednio z serwera ArcGIS CENAGIS (`SION2_Topo_MV/sion2_topo_indoor_all/MapServer/export`). Podkład jest filtrowany po aktualnie wybranym piętrze.
3. **Tryb kamery (odczyt zdarzeń):** Integracja z backendem przez protokół SSE (Server-Sent Events) – zmiana lokalizacji pacjenta (np. przejście obok kamery) natychmiast odświeża jego pozycję na mapie w aplikacji.
4. **Zarządzanie pacjentami:** Dodawanie nowych pacjentów oraz przypisywanie im wolnych tagów QR za pomocą wbudowanego skanera.
