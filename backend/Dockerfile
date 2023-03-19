FROM python:3.10.8-alpine

WORKDIR /app

RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN pip install --upgrade pip
RUN pip install -r requirements.txt
RUN pip install pytest black

EXPOSE 8000

CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]
