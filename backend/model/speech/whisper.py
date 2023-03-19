"""
This script continuously listens for audio input, processes the audio in 5-second chunks, and 
transcribes the speech using the "openai/whisper" model series with the Transformers pipeline 
and chunking enabled.

https://huggingface.co/openai/whisper-tiny
"""

import os
import io
import string
import pyaudio
import numpy as np
import soundfile as sf
from scipy.io import wavfile
from typing import List
from queue import Queue
from threading import Thread
from transformers import WhisperProcessor, WhisperForConditionalGeneration


class Whisper(object):
    def __init__(self):
        self.seconds = int(os.environ.get("SECONDS", 5))
        self.sampling_rate = int(os.environ.get("SAMPLING_RATE", 16000))
        self.pretrained_model = os.environ.get("MODEL", "openai/whisper-tiny")

        self.processor = WhisperProcessor.from_pretrained(self.pretrained_model)
        self.model = WhisperForConditionalGeneration.from_pretrained(
            self.pretrained_model
        )
        self.model.config.forced_decoder_ids = None

    # Record user audio
    def record_audio(
        self,
        queue: Queue,
        chunk: int = 1024,
        channels: int = 1,
        rate: int = 16000,
        format: int = pyaudio.paInt16,
    ) -> None:
        audio = pyaudio.PyAudio()
        stream = audio.open(
            format=format,
            channels=channels,
            rate=rate,
            input=True,
            frames_per_buffer=chunk,
        )

        print("Listening...")

        while True:
            try:
                data = stream.read(chunk, exception_on_overflow=False)
                queue.put(data)
            except OSError as e:
                if e.errno == -9981:
                    print("Input overflowed. Skipping...")
                else:
                    raise e

    def convert_wav_to_flac(self, wav_data: np.ndarray, sample_rate: int) -> np.ndarray:
        flac_data = io.BytesIO()
        sf.write(flac_data, wav_data, sample_rate, format="FLAC")

        flac_data.seek(0)
        flac_data_array, _ = sf.read(flac_data)

        return flac_data_array

    def speech_to_text(self, data: np.ndarray, sampling_rate: int) -> List[str]:
        input_features = self.processor(
            data, sampling_rate=sampling_rate, return_tensors="pt"
        ).input_features
        predicted_ids = self.model.generate(input_features)
        return self.processor.batch_decode(predicted_ids, skip_special_tokens=True)
    
    def transcribe(self, file_path: str) -> str:
        sampling_rate, data = wavfile.read(file_path)
        data = self.convert_wav_to_flac(data, sampling_rate)

        return self.speech_to_text(data, sampling_rate)

    def transcribe_audio(self):
        # Initialize the recording thread and queue
        record_queue = Queue()
        record_thread = Thread(target=self.record_audio, args=(record_queue,))
        record_thread.daemon = True
        record_thread.start()

        # Set up continuous streaming
        buffer = []
        buffer_len = 0
        buffer_max_len = self.seconds * self.sampling_rate

        print("Starting transcription loop...")

        pattern = r"[^\w\s]"

        while True:
            if not record_queue.empty():
                # Retrieve the recorded data and append it to the buffer
                data = record_queue.get()
                data_np = np.frombuffer(data, dtype=np.int16)
                buffer.append(data_np)
                buffer_len += len(data_np)

                # Check if the buffer is full
                if buffer_len >= buffer_max_len:
                    # Concatenate the buffered data into a single array
                    audio_input = np.concatenate(buffer, axis=0)

                    # Save the audio input to a temporary file
                    temp_filename = "temp.wav"
                    sf.write(
                        temp_filename, audio_input, self.sampling_rate, subtype="PCM_16"
                    )

                    sampling_rate, data = wavfile.read("temp.wav")
                    data = self.convert_wav_to_flac(data, sampling_rate)

                    transcription = self.speech_to_text(data, sampling_rate)

                    # TODO: Trigger is for development purposes only. Remove later.
                    print("Transcription:", transcription)
                    trigger = (
                        transcription[0]
                        .lower()
                        .translate(str.maketrans("", "", string.punctuation))
                        .strip()
                    )

                    if trigger == "stop":
                        os.remove(temp_filename)
                        break

                    # Clear the buffer and remove the temporary file
                    buffer = []
                    buffer_len = 0
                    os.remove(temp_filename)
