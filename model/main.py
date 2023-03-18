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
import pyaudio
import torch
import numpy as np
import soundfile as sf
from queue import Queue
from threading import Thread
from transformers import pipeline
from datasets import load_dataset


# Record user audio
def record_audio(queue, chunk=1024, channels=1, rate=16000, format=pyaudio.paInt16):
    audio = pyaudio.PyAudio()
    stream = audio.open(
        format=format, channels=channels, rate=rate, input=True, frames_per_buffer=chunk
    )

    print("Listening...")

    while True:
        try:
            data = stream.read(chunk)
            queue.put(data)
        except OSError as e:
            if e.errno == -9981:  # Input overflowed
                print("Input overflowed. Skipping...")
            else:
                raise e


def transcribe_audio():
    # Initialize the recording thread and queue
    record_queue = Queue()
    record_thread = Thread(target=record_audio, args=(record_queue,))
    record_thread.daemon = True
    record_thread.start()

    # Set up the ASR pipeline
    device = "cuda:0" if torch.cuda.is_available() else "cpu"
    asr_pipeline = pipeline(
        "automatic-speech-recognition",
        model="openai/whisper-tiny",
        chunk_length_s=30,
        device=device,
    )

    # Set up continuous streaming
    buffer = []
    buffer_len = 0
    buffer_max_len = 5 * 16000  # 5 seconds buffer

    print("Starting transcription loop...")

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
                temp_filename = "temp_audio.wav"
                sf.write(temp_filename, audio_input, 16000, subtype="PCM_16")

                # Load the audio from the temporary file
                ds = load_dataset(
                    "hf-internal-testing/librispeech_asr_dummy", "clean", split="validation"
                )
                sample = ds[0]["audio"]
                sample["array"], _ = sf.read(temp_filename, dtype="int16")

                # Transcribe the audio using the ASR pipeline
                prediction = asr_pipeline(sample.copy())["text"]

                # Print the text
                print("Transcription:", prediction)

                # Uncomment the following lines if you want to return timestamps for the predictions
                # prediction = asr_pipeline(sample, return_timestamps=True)["chunks"]
                # print("Transcription with timestamps:", prediction)

                # Clear the buffer and remove the temporary file
                buffer = []
                buffer_len = 0
                os.remove(temp_filename)

def main():
    transcribe_audio()

if __name__ == "__main__":
    main()