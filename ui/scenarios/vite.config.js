import { defineConfig } from 'vite'
import path from 'path'
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs"

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => {
  return {
    plugins: [scalaJSPlugin({
      cwd: '../..',
      projectID: 'scenarios-ui',
      launcher: 'sbtn'
    })],
    publicDir: './src/main/static/public',
    resolve: {
      alias: {
        'resources': path.resolve(__dirname, './src/main/static/resources'),
        'stylesheets': path.resolve(__dirname, './src/main/static/stylesheets')
      }
    },
    base: '/ui/',
    build: {
      outDir: './target/vite'
    }
  }
})
