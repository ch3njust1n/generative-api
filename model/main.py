"""
This script continuously listens for audio input, processes the audio in 5-second chunks, and 
transcribes the speech using the "openai/whisper-tiny" model with the Transformers pipeline 
and chunking enabled. If you want to return timestamps for the predictions, uncomment the 
corresponding lines in the script.

Keep in mind that this implementation may have performance issues for longer durations of 
time or on slower hardware. For a more robust solution, you may want to use an ASR streaming 
service with an API that supports real-time transcription, such as Google Cloud Speech-to-Text 
or Amazon Transcribe.
"""

import os
import io
import re
import pyaudio
import torch
import numpy as np
import soundfile as sf
from scipy.io import wavfile
from queue import Queue
from threading import Thread
from typing import Tuple, Dict
from transformers import pipeline, WhisperProcessor, WhisperForConditionalGeneration


# Record user audio
def record_audio(queue, chunk=1024, channels=1, rate=16000, format=pyaudio.paInt16):
    audio = pyaudio.PyAudio()
    stream = audio.open(
        format=format, channels=channels, rate=rate, input=True, frames_per_buffer=chunk
    )

    print("Listening...")

    while True:
        try:
            data = stream.read(chunk, exception_on_overflow=False)
            queue.put(data)
        except OSError as e:
            if e.errno == -9981:  # Input overflowed
                print("Input overflowed. Skipping...")
            else:
                raise e


def convert_wav_to_flac(wav_data: np.ndarray, sample_rate: int) -> np.ndarray:
    """
    Converts a NumPy array containing WAV audio data to FLAC format and returns the resulting audio as a NumPy array.

    Args:
        wav_data (np.ndarray): A NumPy array containing the WAV audio data to convert.
        sample_rate (int): The sample rate of the WAV audio data.

    Returns:
        A NumPy array representing the audio data from the resulting FLAC file.
    """
    flac_data = io.BytesIO()
    sf.write(flac_data, wav_data, sample_rate, format="FLAC")

    flac_data.seek(0)
    flac_data_array, flac_sample_rate = sf.read(flac_data)

    return flac_data_array


def transcribe_audio():
    # Initialize the recording thread and queue
    record_queue = Queue()
    record_thread = Thread(target=record_audio, args=(record_queue,))
    record_thread.daemon = True
    record_thread.start()

    # Set up continuous streaming
    buffer = []
    buffer_len = 0
    buffer_max_len = 5 * 16000  # 5 seconds buffer

    print("Starting transcription loop...")
    processor = WhisperProcessor.from_pretrained("openai/whisper-medium")
    model = WhisperForConditionalGeneration.from_pretrained("openai/whisper-medium")
    model.config.forced_decoder_ids = None

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
                sf.write(temp_filename, audio_input, 16000, subtype="PCM_16")

                sampling_rate, data = wavfile.read("temp.wav")
                data = convert_wav_to_flac(data, sampling_rate)

                processor = WhisperProcessor.from_pretrained("openai/whisper-tiny")
                model = WhisperForConditionalGeneration.from_pretrained(
                    "openai/whisper-tiny"
                )
                model.config.forced_decoder_ids = None

                input_features = processor(
                    data, sampling_rate=sampling_rate, return_tensors="pt"
                ).input_features
                predicted_ids = model.generate(input_features)
                transcription = processor.batch_decode(
                    predicted_ids, skip_special_tokens=True
                )
                print("Transcription:", transcription)

                if re.sub(pattern, "", transcription[0].lower().strip()) == "stop":
                    break

                # Clear the buffer and remove the temporary file
                buffer = []
                buffer_len = 0
                os.remove(temp_filename)


def main():
    transcribe_audio()


if __name__ == "__main__":
    main()
