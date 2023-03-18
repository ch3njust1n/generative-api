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

from stt.whisper import Whisper


def main():
    model = Whisper()
    model.transcribe_audio()


if __name__ == "__main__":
    main()
