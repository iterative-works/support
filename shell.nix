{ pkgs ? import <nixpkgs> {
  overlays = [
    (final: prev: rec {
      jre = prev.adoptopenjdk-hotspot-bin-11;
      jdk = jre;
    })
  ];
} }:

with pkgs;
mkShell { buildInputs = [ jre ammonite coursier bloop sbt scalafmt ]; }
