from django.contrib import admin
from django.urls import path
from django.http import HttpRequest, HttpResponse, HttpResponseBadRequest, JsonResponse

"""
POST configurations to:
1. Update connected APIs
2. Modify refresh interval
"""


def configure(req: HttpRequest) -> HttpResponse:
    if req.method == "POST":
        configuration = req.POST.get("config")


def get_urls():
    return [
        path("admin/", admin.site.urls),
        path("api/v1/configure", configure, name="configure"),
    ]
