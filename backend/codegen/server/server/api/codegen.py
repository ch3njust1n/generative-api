from django.contrib import admin
from django.urls import path
from django.http import HttpRequest, HttpResponse, HttpResponseBadRequest, JsonResponse


def generate_code():
    pass


def generate_tests():
    pass


def analyze():
    pass


def load():
    pass


def run():
    pass


def start_job(req: HttpRequest) -> HttpResponse:
    if req.method == "POST":
        id = req.POST.get("id")
    pass


urlpatterns = [
    path("admin/", admin.site.urls),
]
