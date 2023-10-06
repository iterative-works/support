{
  description = "IW project";

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
            (self: super:
              let jvm = super.jdk17_headless;
              in {
                jre = jvm;
                jdk = jvm;
              })
          ];
        };
      in { devShell = with pkgs;
          mkShell {
            buildInputs = [ jre ammonite coursier bloop sbt scalafmt ];
          };
      });
}
