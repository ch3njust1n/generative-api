import multiprocessing as mp

from django.contrib import admin
from django.urls import path
from django.http import HttpRequest, HttpResponse, HttpResponseBadRequest, JsonResponse


def generate_code():
    pass


def generate_tests():
    pass


def format(data: dict) -> dict:
    pass


def load(id: str) -> dict:
    pass


def run(id: str) -> None:
    data = load(id)
    formatted = format(data)
    pipe = []


'''
Calls run with the given id
'''
def start_job(req: HttpRequest) -> HttpResponse:
    if req.method == "POST":
        run(req.POST.get("id"))
        


urlpatterns = [
    path("admin/", admin.site.urls),
]
