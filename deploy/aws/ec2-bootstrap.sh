#!/usr/bin/env bash
set -euo pipefail

# Preparación mínima de una instancia Ubuntu para el despliegue académico EFT.
# Ejecutar con un usuario con privilegios sudo.

sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"

echo "Docker instalado. Cierra sesión y vuelve a entrar para usar Docker sin sudo."
echo "Después: clona el repositorio, crea .env, configura DNS/issuer y ejecuta docker compose up -d --build."
