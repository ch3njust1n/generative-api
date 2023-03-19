import pytest
import numpy as np
from scipy.io import wavfile
from speech.whisper import Whisper


@pytest.fixture
def whisper():
    return Whisper()


def test_convert_wav_to_flac(whisper):
    # Create a simple sine wave
    sample_rate = 16000
    frequency = 440
    duration = 1
    time = np.linspace(0, duration, sample_rate * duration, endpoint=False)
    wav_data = np.sin(2 * np.pi * frequency * time) * (2**15 - 1)

    flac_data = whisper.convert_wav_to_flac(wav_data, sample_rate)

    assert isinstance(flac_data, np.ndarray)
    assert len(flac_data) == len(wav_data)

def test_whisper(whisper):
    sampling_rate, data = wavfile.read("test.wav")
    data = whisper.convert_wav_to_flac(data, sampling_rate)
    transcription = whisper.speech_to_text(data, sampling_rate)
    assert transcription[0].strip() == 'Mr. Quilter is the apostle of the middle classes and we are glad to welcome his gospel.'
