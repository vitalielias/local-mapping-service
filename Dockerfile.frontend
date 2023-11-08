# Use a base image that has the necessary tools installed
FROM debian:buster AS base
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y ca-certificates curl git && \
    rm -rf /var/lib/apt/lists/*

# Clone the entire frontend-collection repository
FROM base AS frontend-build
RUN git clone https://github.com/kit-data-manager/frontend-collection.git && \
    # Rename mapping-service-ui.html to index.html
    mv /frontend-collection/mapping-service-ui.html /frontend-collection/index.html

# Copy your custom settings file into the cloned repository
# Assuming the custom settings file is in the same directory as the Dockerfile
COPY mapping-service.settings.js /frontend-collection/js/mapping-service.settings.js

# Use Nginx to serve the content
FROM nginx:stable-alpine AS frontend
# Copy the entire frontend-collection repository to the HTML directory of Nginx
COPY --from=frontend-build /frontend-collection /usr/share/nginx/html
