{
  description = "IW support project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.11";
    eid-pki.url = "git+https://gitlab.e-bs.cz/mph/eid-nix-pki.git?ref=main";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils, eid-pki }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          overlays = [ eid-pki.overlays.jdk21 ];
        };
      in { devShell = with pkgs;
          mkShell {
            buildInputs = [ jre nodejs-18_x ];
          };
      });
}
