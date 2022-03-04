import path from 'path'
import { createHtmlPlugin } from 'vite-plugin-html'
import appInfo from './app-info.js'

//const moduleUrl = new URL(import.meta.url)
//const __dirname = path.dirname(moduleUrl.pathname)

// https://vitejs.dev/config/
export default ({ mode }) => {
    const mainJS = `./${appInfo.bundleMain(mode)}`
    console.log('mainJS', mainJS)
    const script = `<script type="module" src="${mainJS}"></script>`

    return {
        publicDir: './src/main/static/public',
        plugins: [
            createHtmlPlugin({
                minify: process.env.NODE_ENV === 'production',
                template: 'index.html',
                inject: {
                    data: {
                        script
                    }
                }
            })
        ],
        resolve: {
            alias: {
                'stylesheets': path.resolve(__dirname, './src/main/static/stylesheets'),
                'data': path.resolve(__dirname, appInfo.mockDataDir),
                'params': path.resolve(__dirname, './pdb-params'),
                'website-config': mode === 'production' ?
                    path.resolve(__dirname, './website-config/prod') :
                    path.resolve(__dirname, './website-config/dev')
            }
        },
        base: '/mdr/pdb/',
        server: {
            proxy: {
                '/mdr/pdb/api': 'http://localhost:8080'
            }
        },
        build: {
            outDir: 'target/vite'
        }
    }
}
