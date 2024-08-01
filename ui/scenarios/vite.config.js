import { defineConfig } from "vite";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  return {
    publicDir: "./src/main/static/public",
    resolve: {
      alias: {
        "scalajs:main.js": appInfo.bundleMain(mode),
        resources: path.resolve(__dirname, "./src/main/static/resources"),
        stylesheets: path.resolve(__dirname, "./src/main/static/stylesheets"),
      },
    },
    base: "/ui/",
    build: {
      outDir: "./target/vite",
    },
  };
});
