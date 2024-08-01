export const bundleDir = (mode) =>
  `target/scala-3.4.2/app-poptavky-live-${mode === "production" ? "opt" : "fastopt"}`;
export const bundlePath = (mode) => `./${bundleDir(mode)}`;
export const bundleMain = (mode) => `${bundlePath(mode)}/main.js`;

export default {
  bundleDir,
  bundlePath,
  bundleMain,
};
