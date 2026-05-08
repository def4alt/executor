FROM nixos/nix:latest

# Enable flakes and nix-command, then install extra packages
# (bash and git-minimal are already in the base image)
RUN mkdir -p /etc/nix && echo 'experimental-features = nix-command flakes' > /etc/nix/nix.conf
RUN nix profile install nixpkgs#cacert nixpkgs#curl \
    nixpkgs#jq nixpkgs#openssh nixpkgs#procps nixpkgs#python3 \
    nixpkgs#rsync nixpkgs#sudo nixpkgs#tmux nixpkgs#unzip nixpkgs#zip \
    nixpkgs#xz nixpkgs#bun \
  && nix store gc

RUN groupadd --gid 1000 workspace \
  && useradd --uid 1000 --gid 1000 --create-home --shell /bin/bash workspace \
  && usermod -aG sudo workspace \
  && echo 'workspace ALL=(ALL) NOPASSWD:ALL' > /etc/sudoers.d/workspace \
  && chmod 0440 /etc/sudoers.d/workspace

RUN mkdir -p /etc/nix \
  && echo 'sandbox = relaxed' > /etc/nix/nix.conf \
  && echo 'extra-nix-path = nixpkgs=/nix/var/nix/profiles/per-user/root/channels/nixpkgs' >> /etc/nix/nix.conf

USER workspace
WORKDIR /workspace

COPY --chown=workspace:workspace image/install.sh /home/workspace/bin/install.sh
COPY --chown=workspace:workspace image/bootstrap.sh /home/workspace/bin/bootstrap.sh
COPY --chown=workspace:workspace image/entrypoint.sh /home/workspace/bin/entrypoint.sh

RUN chmod +x ~/bin/install.sh ~/bin/bootstrap.sh ~/bin/entrypoint.sh && ~/bin/install.sh

EXPOSE 7681

ENTRYPOINT ["/home/workspace/bin/entrypoint.sh"]
