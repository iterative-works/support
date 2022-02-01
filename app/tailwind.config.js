const appInfo = require('./app-info.js')
const colors = require('tailwindcss/colors')
const typography = require('@tailwindcss/typography')
const forms = require('@tailwindcss/forms')
const path = require('path')

module.exports = {
    mode: 'jit',
    purge: [
        path.resolve(__dirname, `./${appInfo.bundlePath(process.env.NODE_ENV)}/*.js`),
        path.resolve(__dirname, './*.html'),
    ],
    theme: {
        extend: {
            fontFamily: {
                serif: ['Inter', 'ui-serif', 'Georgia', 'Cambria', '"Times New Roman"', 'Times', 'serif'],
            },
            colors: {
                gray: colors.coolGray,
            },
        },
    },
    corePlugins: {},
    plugins: [typography, forms],
}
