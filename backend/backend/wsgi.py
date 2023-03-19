"""
WSGI config for backend project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/4.1/howto/deployment/wsgi/
"""

import os
import shutil
import atexit
from django.core.wsgi import get_wsgi_application

os.environ.setdefault("DJANGO_SETTINGS_MODULE", "backend.settings")

application = get_wsgi_application()

def shutdown_handler():
    print("Executing tasks before shutting down the server...")
    print(os.getcwd())
    shutil.rmtree("media/")

atexit.register(shutdown_handler)