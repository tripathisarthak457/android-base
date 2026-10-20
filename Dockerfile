# The generator API, as one container.
#
# The service is a Go binary that shells out to `generate_headless.py`, so the runtime image needs
# a Python interpreter and the whole generator — `genkit/`, the `templates/` it copies from and the
# `tokens/` it emits from — next to the binary. That is the reason this is a container rather than
# a plain Go build: reimplementing the generator in Go is how the terminal and the website start
# producing different projects.
#
# It lives at the repository root because the build context is the directory the Dockerfile is in,
# and the generator is what the image is mostly made of. `.dockerignore` admits only the four
# directories the image needs.

FROM golang:1.27-alpine AS build
WORKDIR /src

# Dependencies first, so editing a handler does not re-download the module cache.
COPY web/api/go.mod web/api/go.sum ./
RUN go mod download

COPY web/api/ ./
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags '-s -w' -o /out/server ./cmd/server


FROM python:3.12-alpine
WORKDIR /app

# A headless JRE, for one binary out of it: `keytool`. Signing keys are the only thing the generator
# runs a JDK for, and without one the generator reports the keys as skipped rather than failing, so
# forgetting this line does not break the build: it silently removes a feature.
RUN apk add --no-cache openjdk21-jre-headless

# No pip install: the generator is standard library only.
COPY --from=build /out/server /app/server
COPY generator/ /app/generator/
COPY templates/ /app/templates/
COPY tokens/ /app/tokens/

# The base image deletes the standard library's bytecode and the server runs as nobody, who cannot
# write it back, so without this every generation would recompile the stdlib and genkit before
# doing any work. The catalogue is computed here once rather than by Python at every cold start.
RUN python -m compileall -q -j 0 /usr/local/lib/python3.12 /app/generator \
 && cd /app/generator \
 && python -m genkit.core.catalogue --json > /app/catalogue.json

# The generator resolves `templates/` and `tokens/` relative to itself, so the layout under /app
# mirrors the repository. JAVA_HOME is set because Alpine's JRE package does not reliably link
# keytool into /usr/bin. TRUSTED_PROXY=vercel because Vercel is where this image runs: its edge
# overwrites X-Forwarded-For and adds the geo headers. Anywhere else, set it to match the proxy.
# ENABLE_TARGETS is set here so the image, not a setting remembered on the host, decides what the
# public API generates: Android only, until another target's generated projects are built in CI.
ENV GENERATOR_DIR=/app/generator \
    CATALOGUE_FILE=/app/catalogue.json \
    ENABLE_TARGETS=android \
    PYTHON_BIN=python3 \
    PORT=8080 \
    TRUSTED_PROXY=vercel \
    JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
    PATH="/usr/lib/jvm/java-21-openjdk/bin:$PATH"

# Runs as nobody: the only thing this process writes is its temporary work directory.
USER nobody
EXPOSE 8080
CMD ["/app/server"]
