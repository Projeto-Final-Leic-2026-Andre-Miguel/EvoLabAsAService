rootProject.name = "EvoLab"

include(
    "evolab-frontend",
    "evolab-backend",
    "evolab-backend:db",
    "evolab-backend:domain",
    "evolab-backend:repo",
    "evolab-backend:service",
    "evolab-backend:http",
    "evolab-backend:app"
)
