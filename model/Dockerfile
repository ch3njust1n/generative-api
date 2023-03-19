FROM python:3.10.8-alpine

WORKDIR /app

COPY . .

RUN pip install --upgrade pip
RUN pip install -r requirements.txt
RUN pip install pytest black

CMD ["python", "manage.py", "runserver", "0.0.0.0:8000"]
