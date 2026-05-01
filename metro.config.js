const {getDefaultConfig} = require('metro-config');

module.exports = (async () => {
	return getDefaultConfig(__dirname);
})();
