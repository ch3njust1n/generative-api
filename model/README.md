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



```
python3.10 -m pip install --upgrade pip
```

```
pip install -r requirements.txt
```