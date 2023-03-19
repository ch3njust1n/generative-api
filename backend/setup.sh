#!/bin/bash

if [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "Detected Linux operating system"
    echo "Installing ffmpeg..."
    sudo apt update
    sudo apt install -y ffmpeg
    echo "ffmpeg installation complete."

elif [[ "$OSTYPE" == "darwin"* ]]; then
    echo "Detected macOS operating system"
    echo "Checking if Homebrew is installed..."
    if ! command -v brew &> /dev/null; then
        echo "Homebrew not found. Installing Homebrew..."
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    fi
    echo "Installing ffmpeg..."
    brew install ffmpeg
    echo "ffmpeg installation complete."

elif [[ "$OSTYPE" == "cygwin" || "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    echo "Detected Windows operating system"
    echo "Please follow the manual installation instructions for Windows. https://www.gyan.dev/ffmpeg/builds/"

else
    echo "Operating system not supported. Please follow the manual installation instructions for your OS."
fi

# Install required packages
pip install -r model/requirements.txt