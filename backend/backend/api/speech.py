import os
import uuid
from pydub import AudioSegment
from django.urls import path
from django.http import JsonResponse
from django.views.decorators.csrf import csrf_exempt

from model.speech.whisper import Whisper

ALLOWED_EXTENSIONS = {"mp3", "wav", "ogg", "m4a", "flac", "webm"}


def allowed_file(filename: str) -> bool:
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS


@csrf_exempt
def transcribe_audio(request):
    model = Whisper()

    if request.method == "POST":
        if "file" not in request.FILES:
            return JsonResponse({"error": "No file provided."}, status=400)

        file = request.FILES["file"]
        if not allowed_file(file.name):
            return JsonResponse(
                {"error": "Invalid file type. Please upload an audio file."}, status=400
            )
        
        os.makedirs("media", exist_ok=True)

        # Save the received file
        filename = f"{uuid.uuid4()}.{file.name.rsplit('.', 1)[1].lower()}"
        filepath = os.path.join("media", filename)

        with open(filepath, "wb") as f:
            for chunk in file.chunks():
                f.write(chunk)

        # Convert the file to .wav format
        input_format = file.name.rsplit(".", 1)[1].lower()
        if input_format != "wav":
            wav_filename = f"{uuid.uuid4()}.wav"
            wav_filepath = os.path.join("media", wav_filename)
            sound = AudioSegment.from_file(filepath, format=input_format)
            sound.export(wav_filepath, format="wav")
            os.remove(filepath)  # Remove the original file
        else:
            wav_filepath = filepath

        # Call the transcription function
        # transcription = transcribe(wav_filepath)
        transcription = model.transcribe(wav_filepath)

        # Remove the .wav file
        os.remove(wav_filepath)

        return JsonResponse({"transcription": transcription})
    else:
        return JsonResponse({"error": "Invalid request method. Use POST."}, status=405)


def get_urlpatterns():
    return [
        path("transcribe/", transcribe_audio, name="transcribe"),
    ]
