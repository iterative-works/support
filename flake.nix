{
  description = "IW support";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    flake-compat = {
      url = "github:edolstra/flake-compat";
      flake = false;
    };
  };

  outputs = { self, nixpkgs, flake-utils, flake-compat }:
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
      in
      {
        devShell = with pkgs;
          mkShell {
            buildInputs = [ jre ammonite coursier bloop sbt scalafmt nodejs-16_x ];
          };
      });
}
