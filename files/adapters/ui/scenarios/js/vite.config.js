import { defineConfig } from "vite";
import glob from "glob";
import path from "path";
import { fileURLToPath } from "node:url";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  return {
    publicDir: "./js/src/main/static/public",
    resolve: {
      alias: {
        resources: path.resolve(__dirname, "./js/src/main/static/resources"),
        stylesheets: path.resolve(
          __dirname,
          "./js/src/main/static/stylesheets",
        ),
      },
    },
    build: {
      // generate .vite/manifest.json in outDir
      manifest: true,
      rollupOptions: {
        input: Object.fromEntries(
          glob
            .sync(`${process.env.VITE_SCALAJS_OUTPUT}/scenario*.js`)
            .map((file) => [
              // This remove `src/` as well as the file extension from each
              // file, so e.g. src/nested/foo.js becomes nested/foo
              path.relative(
                process.env.VITE_SCALAJS_OUTPUT,
                file.slice(0, file.length - path.extname(file).length),
              ),
              // This expands the relative paths to absolute paths, so e.g.
              // src/nested/foo becomes /project/src/nested/foo.js
              fileURLToPath(new URL(file, import.meta.url)),
            ]),
        ),
      },
      outDir: "./target/vite",
    },
  };
});
