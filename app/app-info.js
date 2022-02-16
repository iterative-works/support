const scalaVersion = require('./scala-version.js')

const bundleDir = (mode) => `scala-${scalaVersion}/app-${mode === 'production' ? 'opt' : 'fastopt'}`
const bundlePath = (mode) => `target/${bundleDir(mode)}`
const bundleMain = (mode) => `${bundlePath(mode)}/main.js`
// Generated data for mockups
const mockDataDir = `target/mock_data`

module.exports = {
  bundleDir, bundlePath, bundleMain, mockDataDir
}
