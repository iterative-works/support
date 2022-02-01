module.exports = () => {
    const plugins = {
        'postcss-import': {},
        tailwindcss: require('./tailwind.config.js'),
        autoprefixer: {}
    }
    return {
        plugins
    }
}
