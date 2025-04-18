{
  description = "IW project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [
            (self: super:
              let jvm = super.jdk21_headless;
              in {
                jre = jvm;
                jdk = jvm;
              })
          ];
        };
      in { devShell = with pkgs;
          mkShell {
            buildInputs = [ jre nodejs-18_x ];
          };
      });
}
