from whisper import Whisper


def main():
    model = Whisper()
    model.transcribe_audio()


if __name__ == "__main__":
    main()
