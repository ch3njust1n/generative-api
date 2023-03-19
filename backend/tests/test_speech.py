import os
import pytest
from scipy.io import wavfile


def test_transcribe():
    pretrained_model = os.environ.get("MODEL", "openai/whisper-tiny")

    processor = WhisperProcessor.from_pretrained(pretrained_model)
    model = WhisperForConditionalGeneration.from_pretrained(pretrained_model)
    model.config.forced_decoder_ids = None

    sampling_rate, data = wavfile.read("test.wav")
    data = whisper.convert_wav_to_flac(data, sampling_rate)

    input_features = processor(
        data, sampling_rate=sampling_rate, return_tensors="pt"
    ).input_features
    predicted_ids = model.generate(input_features)
    transcription = processor.batch_decode(predicted_ids, skip_special_tokens=True)
    assert (
        transcription[0].strip()
        == "Mr. Quilter is the apostle of the middle classes and we are glad to welcome his gospel."
    )
