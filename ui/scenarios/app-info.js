// Mill output paths
export const bundleDir = (mode) =>
  `../../out/scenariosUI/${mode === "production" ? "fullLinkJS" : "fastLinkJS"}.dest`;
export const bundlePath = (mode) => `./${bundleDir(mode)}`;
export const bundleMain = (mode) => `${bundlePath(mode)}/main.js`;

export default {
  bundleDir,
  bundlePath,
  bundleMain,
};
