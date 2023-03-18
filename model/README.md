### STT for 4 LUI
Implements OpenAI's Whisper STT model that integrates with GPT-4

### Setup

Install dependencies and set environment variables to tell PyAudio install process where to find PortAudio header and libraries:
```
brew install portaudio
pip install wheel

export LDFLAGS="-L/usr/local/lib"
export CFLAGS="-I/usr/local/include"

pip install pyaudio
```

Upgrade pip and install requirements.txt
```
python3 -m pip install --upgrade pip
pip install -r requirements.txt
```

### Model weights

`onnx` directory will automatically be created when stt.whisper model is instantiated.


### Setting .env

For latest ONNX opset table, see [here](https://github.com/onnx/onnx/blob/main/docs/Versioning.md).