@echo off
title SmartRental AI Service
echo ===================================================
echo     Starting SmartRental AI Service (FastAPI)
echo     Port: 8000
echo ===================================================
venv\Scripts\python.exe -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
pause
