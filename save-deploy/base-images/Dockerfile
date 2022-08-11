ARG BASE_IMAGE_NAME
ARG BASE_IMAGE_TAG
FROM $BASE_IMAGE_NAME:$BASE_IMAGE_TAG

RUN if command -v apt-get &> /dev/null; \
    then apt-get update && env DEBIAN_FRONTEND="noninteractive" apt-get install -y libcurl4-openssl-dev tzdata && rm -rf /var/lib/apt/lists/*; \
    else apk add curl-dev tzdata; \
    fi;
RUN ln -fs /usr/share/zoneinfo/UTC /etc/localtime

# Fixme: https://github.com/saveourtool/save-cloud/issues/352
RUN if [ "$BASE_IMAGE_NAME" = "python" ]; \
  then curl -s "https://get.sdkman.io" | bash && \
    bash -c 'source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 8.0.302-open' && \
    ln -s $(which java) /usr/bin/java; \
  fi

RUN groupadd --gid 1100 save-agent && useradd --uid 1100 --gid 1100 --create-home --shell /bin/sh save-agent
WORKDIR /home/save-agent/save-execution