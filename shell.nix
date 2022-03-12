{ pkgs ? import <nixpkgs> {
  overlays = [
    (final: prev: rec {
      jre = prev.adoptopenjdk-hotspot-bin-11;
      jdk = jre;
    })
  ];
} }:

with pkgs;
mkShell {
  buildInputs = [ jre ammonite coursier bloop mill sbt scalafmt nodejs-16_x ];
}
