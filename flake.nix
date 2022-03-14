{
  description = "IW support";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [
            (final: prev: rec {
              jre = prev.adoptopenjdk-hotspot-bin-11;
              jdk = jre;
            })
          ];
        };
      in { devShell = import ./shell.nix { inherit pkgs; }; });
}
