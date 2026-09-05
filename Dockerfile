# The generator API, as one container.
#
# Two stages, and the second one is the interesting half: the service is a Go binary that shells
# out to `generate_headless.py`, so the runtime image needs a Python interpreter and the whole
# generator — `genkit/` and the `template/` tree it copies from — sitting next to the binary. That
# is the entire reason this is a container rather than a plain Go build: Vercel's Go runtime image
# has no Python in it, and reimplementing the generator in Go to avoid the dependency is how the
# terminal and the website start producing different projects.
#
# It lives at the repository root rather than beside the Go code because the build context is the
# directory the Dockerfile is in, and `generator/` and `template/` are what the image is mostly
# made of — hence the `web/api/` prefixes on the COPY lines below. `.dockerignore` keeps the 2.7GB
# of Gradle build output out of the context; what actually goes in is about 1.5MB.
#
# The runtime stage also carries a headless JRE, purely for `keytool`. See the note beside it.

FROM golang:1.25-alpine AS build
WORKDIR /src

# Dependencies first, so editing a handler does not re-download the module cache.
COPY web/api/go.mod web/api/go.sum ./
RUN go mod download

COPY web/api/ ./
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags '-s -w' -o /out/server ./cmd/server


FROM python:3.12-alpine
WORKDIR /app

# A headless JRE, for one binary out of it: `keytool`.
#
# Signing keys are the only thing the generator shells out to a JDK for, and without one here the
# catalogue reports `keystoresAvailable: false` and the site hides the offer rather than taking
# passwords and returning a zip with no keys in it. That is the correct behaviour, but it means
# forgetting this line does not break the build — it silently removes a feature.
#
# `-jre-headless` rather than a full JDK: no compiler, no AWT, about 55MB.
RUN apk add --no-cache openjdk21-jre-headless

# No pip install: the generator is standard library only, deliberately, and this is where that
# decision pays for itself.
COPY --from=build /out/server /app/server
COPY generator/ /app/generator/
COPY template/ /app/template/

# generate_headless.py resolves the template as `Path(__file__).parent.parent / "template"`, so
# the two have to stay siblings here exactly as they are in the repository.
#
# JAVA_HOME is set as well as PATH because Alpine's JRE packages do not reliably symlink keytool
# into /usr/bin, and `find_keytool` checks JAVA_HOME/bin when PATH comes up empty.
ENV GENERATOR_DIR=/app/generator \
    PYTHON_BIN=python3 \
    PORT=8080 \
    JAVA_HOME=/usr/lib/jvm/java-21-openjdk \
    PATH="/usr/lib/jvm/java-21-openjdk/bin:$PATH"

# Runs as nobody: the only thing this process should be able to write is a temp directory.
USER nobody
EXPOSE 8080
CMD ["/app/server"]
