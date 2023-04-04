FROM python:3.10-slim

ENV LDFLAGS="-L/usr/local/lib"
ENV CFLAGS="-I/usr/local/include"

WORKDIR /app

RUN apt-get update && apt-get clean

COPY requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN pip install --upgrade pip
RUN pip install -r requirements.txt
RUN pip install pytest black

EXPOSE 8000

CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]
