FROM paketobuildpacks/builder:base

USER root
RUN apt update && apt install -y software-properties-common && \
    add-apt-repository ppa:deadsnakes/ppa && apt update && \
    apt install -y gcc python3.10-dev
USER cnb
