ARG BASE_IMAGE

FROM $BASE_IMAGE

ARG BASE_IMAGE_NAME

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y curl libcurl4-openssl-dev tzdata unzip
RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime

# clang-12 can only be installed to ubuntu and is only needed for GSAC contest
RUN if [ "$BASE_IMAGE_NAME" = "ubuntu" ]; then \
    apt-get install -y clang-12 clang-format libclang-12-dev; \
    fi

# Fixme: https://github.com/saveourtool/save-cloud/issues/352
RUN if [ "$BASE_IMAGE_NAME" = "python" ]; then \
    curl -s "https://get.sdkman.io" | bash && \
    bash -c 'source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 8.0.302-open' && \
    ln -s $(which java) /usr/bin/java; \
    fi

RUN rm -rf /var/lib/apt/lists/*

RUN groupadd --gid 1100 save-agent && \
    useradd --uid 1100 --gid 1100 --create-home --shell /bin/sh save-agent && \
    # `WORKDIR` directive creates the directory as `root` user unless the directory already exists
    mkdir /home/save-agent/save-execution && \
    chown -R 1100:1100 /home/save-agent/save-execution
USER save-agent
WORKDIR /home/save-agent/save-execution
