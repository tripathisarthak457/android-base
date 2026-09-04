#!/usr/bin/env bash
#
# Sets up the API on a fresh Ubuntu 24.04 Vultr box.
#
#   curl -fsSL https://raw.githubusercontent.com/tripathisarthak457/android-base/main/web/deploy/install.sh | sudo bash -s -- api.yourapp.duckdns.org
#
# Idempotent: running it twice is how you deploy an update. It pulls, rebuilds, restarts, and
# leaves the database and the generated .env alone.
#
# What it installs: Go (to build the binary), Python 3 (to run the generator), Postgres (for the
# admin portal), Caddy (for TLS), and a systemd unit for the API. No Docker — the whole service is
# one static binary and one Python package, and a container adds a layer to debug through for
# nothing in return.

set -euo pipefail

DOMAIN="${1:-}"
if [[ -z "$DOMAIN" ]]; then
	echo "Usage: install.sh <domain>" >&2
	echo "  e.g. install.sh api.yourapp.duckdns.org" >&2
	exit 1
fi

REPO_URL="https://github.com/tripathisarthak457/android-base.git"
APP_DIR="/opt/android-base"
SERVICE_USER="androidgen"
ENV_FILE="/etc/android-base.env"

log() { printf '\n\033[1;34m==>\033[0m %s\n' "$1"; }

if [[ $EUID -ne 0 ]]; then
	echo "Run this with sudo." >&2
	exit 1
fi

log "Installing packages"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq curl git ca-certificates gnupg python3 postgresql postgresql-contrib golang-go

# Caddy is not in the default archive.
if ! command -v caddy >/dev/null; then
	log "Installing Caddy"
	apt-get install -y -qq debian-keyring debian-archive-keyring apt-transport-https
	curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' |
		gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
	curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' |
		tee /etc/apt/sources.list.d/caddy-stable.list >/dev/null
	apt-get update -qq
	apt-get install -y -qq caddy
fi

log "Creating the service user"
id -u "$SERVICE_USER" >/dev/null 2>&1 || useradd --system --create-home --shell /usr/sbin/nologin "$SERVICE_USER"

log "Fetching the source"
if [[ -d "$APP_DIR/.git" ]]; then
	git -C "$APP_DIR" fetch --quiet origin
	git -C "$APP_DIR" reset --hard --quiet origin/main
else
	git clone --quiet "$REPO_URL" "$APP_DIR"
fi
chown -R "$SERVICE_USER:$SERVICE_USER" "$APP_DIR"

log "Setting up Postgres"
DB_NAME="androidgen"
DB_USER="androidgen"
if ! sudo -u postgres psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='$DB_USER'" | grep -q 1; then
	DB_PASSWORD="$(openssl rand -hex 24)"
	sudo -u postgres psql -qc "CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';"
	sudo -u postgres psql -qc "CREATE DATABASE $DB_NAME OWNER $DB_USER;"
	NEW_DB=1
else
	echo "    the database already exists; keeping it and its password"
	NEW_DB=0
fi

log "Writing $ENV_FILE"
# Secrets are generated once and never regenerated: rotating IP_SALT on every deploy would make
# every returning visitor look new, and rotating ADMIN_TOKEN would log you out on every update.
if [[ ! -f "$ENV_FILE" ]]; then
	if [[ "$NEW_DB" -eq 0 ]]; then
		echo "The database exists but $ENV_FILE does not — recover the password or drop the role." >&2
		exit 1
	fi
	ADMIN_TOKEN="$(openssl rand -hex 32)"
	IP_SALT="$(openssl rand -hex 32)"
	cat >"$ENV_FILE" <<EOF
ADDR=127.0.0.1:8080
DATABASE_URL=postgres://$DB_USER:$DB_PASSWORD@127.0.0.1:5432/$DB_NAME?sslmode=disable
GENERATOR_DIR=$APP_DIR/generator
PYTHON_BIN=python3
ADMIN_TOKEN=$ADMIN_TOKEN
IP_SALT=$IP_SALT
ALLOWED_ORIGINS=https://android-base.vercel.app,http://localhost:3000
GENERATE_TIMEOUT=90s
MAX_CONCURRENT_GENERATIONS=4
RATE_LIMIT_PER_HOUR=30
EOF
	chmod 600 "$ENV_FILE"
	echo
	echo "    ADMIN_TOKEN=$ADMIN_TOKEN"
	echo "    Save that now. It is the password for /admin and it is not printed again."
	echo
else
	echo "    keeping the existing $ENV_FILE"
fi

log "Building the API"
cd "$APP_DIR/web/api"
go build -o /usr/local/bin/androidgen-api ./cmd/server

log "Installing the systemd unit"
cat >/etc/systemd/system/androidgen-api.service <<EOF
[Unit]
Description=Android project generator API
After=network-online.target postgresql.service
Wants=network-online.target

[Service]
Type=simple
User=$SERVICE_USER
EnvironmentFile=$ENV_FILE
ExecStart=/usr/local/bin/androidgen-api
Restart=always
RestartSec=3

# The service reads the template and writes a zip to /tmp. It needs nothing else, so it is given
# nothing else.
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadOnlyPaths=$APP_DIR
ProtectKernelTunables=true
ProtectControlGroups=true
RestrictSUIDSGID=true
LockPersonality=true

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now androidgen-api
systemctl restart androidgen-api

log "Configuring Caddy for $DOMAIN"
sed "s/api\.yourapp\.duckdns\.org/$DOMAIN/" "$APP_DIR/web/deploy/Caddyfile" >/etc/caddy/Caddyfile
systemctl reload caddy || systemctl restart caddy

log "Checking it came up"
sleep 3
if curl -fsS http://127.0.0.1:8080/api/health >/dev/null; then
	echo "    the API is answering on loopback"
else
	echo "    the API is NOT answering. journalctl -u androidgen-api -n 50" >&2
	exit 1
fi

cat <<EOF

Done.

  API          https://$DOMAIN/api/health
  Admin        https://$DOMAIN/admin/overview  (needs the bearer token)
  Logs         journalctl -u androidgen-api -f
  Update       sudo bash $APP_DIR/web/deploy/install.sh $DOMAIN

Two things left:

  1. Point $DOMAIN at this box's IP, if you have not already. Caddy will get a
     certificate on the first request.
  2. Set NEXT_PUBLIC_API_BASE=https://$DOMAIN on the Vercel project, and add the
     Vercel URL to ALLOWED_ORIGINS in $ENV_FILE, then:
     systemctl restart androidgen-api

EOF
