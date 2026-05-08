FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl git jq openssh-client procps python3 \
    rsync sudo tmux unzip xz-utils zip \
  && rm -rf /var/lib/apt/lists/*

# Install Nix via Determinate Systems installer (Docker-optimized)
# https://github.com/DeterminateSystems/nix-installer
RUN curl --proto '=https' --tlsv1.2 -sSf -L https://install.determinate.systems/nix | \
    sh -s -- install linux --extra-conf "sandbox = false" --init none --no-confirm

RUN groupadd --gid 1000 workspace \
  && useradd --uid 1000 --gid 1000 --create-home --shell /bin/bash workspace \
  && usermod -aG sudo workspace \
  && echo 'workspace ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/workspace

# Install ttyd (browser terminal) from GitHub releases
RUN curl -fsSL https://github.com/tsl0922/ttyd/releases/download/1.7.7/ttyd.x86_64 -o /usr/local/bin/ttyd \
  && chmod +x /usr/local/bin/ttyd

# Install bun system-wide (for Pi agent)
RUN curl -fsSL https://bun.sh/install | bash \
  && cp /root/.bun/bin/bun /usr/local/bin/bun \
  && rm -rf /root/.bun

ENV NIX_CONFIG="experimental-features = nix-command flakes"
ENV PATH="/nix/var/nix/profiles/default/bin:/usr/local/bin:/usr/bin:/bin"

COPY --chown=workspace:workspace image/install.sh /home/workspace/bin/install.sh
COPY --chown=workspace:workspace image/bootstrap.sh /home/workspace/bin/bootstrap.sh
COPY --chown=workspace:workspace image/entrypoint.sh /home/workspace/bin/entrypoint.sh
RUN chmod +x /home/workspace/bin/*.sh

USER workspace
WORKDIR /workspace

ENV PATH="/nix/var/nix/profiles/default/bin:/usr/local/bin:/usr/bin:/bin"
ENV NIX_CONFIG="experimental-features = nix-command flakes"

RUN /home/workspace/bin/install.sh

EXPOSE 7681
ENTRYPOINT ["/home/workspace/bin/entrypoint.sh"]
