import path from 'path'
import {minifyHtml, injectHtml} from 'vite-plugin-html'
import appInfo from './app-info.js'

//const moduleUrl = new URL(import.meta.url)
//const __dirname = path.dirname(moduleUrl.pathname)

// https://vitejs.dev/config/
export default ({mode}) => {
    const mainJS = `./${appInfo.bundleMain(mode)}`
    console.log('mainJS', mainJS)
    const script = `<script type="module" src="${mainJS}"></script>`

    return {
        publicDir: './src/main/static/public',
        plugins: [
            ...(process.env.NODE_ENV === 'production' ? [
                minifyHtml(),
            ] : []),
            injectHtml({
                injectData: {
                    script
                }
            })
        ],
        resolve: {
            alias: {
                'stylesheets': path.resolve(__dirname, './src/main/static/stylesheets'),
                /*
                'website-config': mode === 'production' ?
                    resolve(__dirname, '../website-config/prod') :
                    resolve(__dirname, '../website-config/dev')
                */
            }
        },
        base: '/mdr/',
        build: {
            outDir: 'target/vite'
        }
    }
}
