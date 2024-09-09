import { defineConfig } from "vite";
import { glob } from "glob";
import path from "path";
import { fileURLToPath } from "node:url";
import globResolverPlugin from "@raquo/vite-plugin-glob-resolver";
import importSideEffectPlugin from "@raquo/vite-plugin-import-side-effect";
import rollupCopyPlugin from 'rollup-plugin-copy'

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  return {
    publicDir: "public",
    resolve: {
      alias: {
        resources: path.resolve(__dirname, "src/main/static/resources"),
        stylesheets: path.resolve(
          __dirname,
          "src/main/static/stylesheets",
        ),
      },
    },
    plugins: [
      importSideEffectPlugin({
        // See https://github.com/raquo/vite-plugin-import-side-effect
        defNames: ['importStyle'],
        rewriteModuleIds: ['**/*.less', '**/*.css'],
        // verbose: true
      })
      // TODO: copy icons automatically, now I have to manually
      // yarn unplug @shoelace-style/shoelace
      // and copy from <project root>/.yarn/unplugged/@shoelace*/node_modules/@shoelace-style/shoelace/dist/assets
      // to src/main/static/public/assets
    ],
    build: {
      // generate .vite/manifest.json in outDir
      manifest: true,
      rollupOptions: {
        input: Object.fromEntries(
          glob
            // The scenario entries need to start scenario*, so that we can filter just these
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
