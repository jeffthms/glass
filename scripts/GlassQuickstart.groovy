import grails.util.GrailsNameUtils
import grails.util.Metadata
import groovy.text.SimpleTemplateEngine

USAGE = """
Usage: grails glass-quickstart <domain-class-package> <user-class-name> <user-credential-class-name>

Creates a user and user credential class for the specified package

Example: grails glass-quickstart com.yourapp User UserCredential
"""

//includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript('_GrailsBootstrap')

packageName = ''
userClassName = ''
userCredentialClassName = ''
overwriteAll = false
templateAttributes = [:]
templateDir = "$glassPluginDir/src/templates"
appDir = "$basedir/grails-app"
templateEngine = new SimpleTemplateEngine()

target(glassQuickstart: 'Creates artifacts for the Glass plugin') {
	depends(checkVersion, configureProxy, packageApp, classpath)

	if (!configure()) {
		return 1
	}
	createDomains()
	updateConfig()

	printMessage """
*******************************************************
* Created domain classes, controllers, and GSPs. Your *
* grails-app/conf/Config.groovy has been updated with *
* the class names of the configured domain classes;   *
* please verify that the values are correct.          *
*******************************************************
"""
}

private Boolean configure() {
	def argValues = parseArgs()
	if (!argValues) {
		return false
	}

	(packageName, userClassName, userCredentialClassName) = argValues

	templateAttributes = [packageName: packageName,
	                      userClassName: userClassName,
	                      userCredentialClassName: userCredentialClassName]
}

private void updateConfig() {
	def configFile = new File(appDir, 'conf/Config.groovy')
	if (configFile.exists()) {
		configFile.withWriterAppend {
			it.writeLine '\n// Added by the Glass plugin:'
			it.writeLine "grails.plugin.glass.userDomainClassName = '${packageName}.$userClassName'"
			it.writeLine "grails.plugin.glass.userCredentialDomainClassName = '${packageName}.$userCredentialClassName'"
			it.writeLine "//grails.plugin.glass.username = 'username of your app\'s google account (for email notifications - optional)'"
			it.writeLine "//grails.plugin.glass.password = 'password of your app\'s google account (for email notifications - optional)'"
			it.writeLine "grails.plugin.glass.appname = 'Change Me'"
			it.writeLine "grails.plugin.glass.imageurl = 'URL to an image representing your app'"
			it.writeLine "grails.plugin.glass.home.controller = 'controller'"
			it.writeLine "grails.plugin.glass.home.action = 'action to direct to after user has been authorised by Google OAuth2 (optional. Will default to a simple connected message)'"
			it.writeLine "grails.plugin.glass.oauth.clientid = 'client ID provided by Google'"
			it.writeLine "grails.plugin.glass.oauth.clientsecret = 'client secret provided by Google'"
		}
	}
}

private void createDomains() {
	String dir = packageToDir(packageName)
	generateFile "$templateDir/User.groovy.template", "$appDir/domain/${dir}${userClassName}.groovy"
	generateFile "$templateDir/UserCredential.groovy.template", "$appDir/domain/${dir}${userCredentialClassName}.groovy"
}

private parseArgs() {
	def args = argsMap.params

	if (3 == args.size()) {
		printMessage "Creating User class ${args[1]} and UserCredential class ${args[2]} in package ${args[0]}"
		return args
	}

	errorMessage USAGE
	null
}

packageToDir = { String packageName ->
	String dir = ''
	if (packageName) {
		dir = packageName.replaceAll('\\.', '/') + '/'
	}

	return dir
}

okToWrite = { String dest ->

	def file = new File(dest)
	if (overwriteAll || !file.exists()) {
		return true
	}

	String propertyName = "file.overwrite.$file.name"
	ant.input(addProperty: propertyName, message: "$dest exists, ok to overwrite?",
	          validargs: 'y,n,a', defaultvalue: 'y')

	if (ant.antProject.properties."$propertyName" == 'n') {
		return false
	}

	if (ant.antProject.properties."$propertyName" == 'a') {
		overwriteAll = true
	}

	true
}

generateFile = { String templatePath, String outputPath ->
	if (!okToWrite(outputPath)) {
		return
	}

	File templateFile = new File(templatePath)
	if (!templateFile.exists()) {
		errorMessage "\nERROR: $templatePath doesn't exist"
		return
	}

	File outFile = new File(outputPath)

	// in case it's in a package, create dirs
	ant.mkdir dir: outFile.parentFile

	outFile.withWriter { writer ->
		templateEngine.createTemplate(templateFile.text).make(templateAttributes).writeTo(writer)
	}

	printMessage "generated $outFile.absolutePath"
}

splitClassName = { String fullName ->

	int index = fullName.lastIndexOf('.')
	String packageName = ''
	String className = ''
	if (index > -1) {
		packageName = fullName[0..index-1]
		className = fullName[index+1..-1]
	}
	else {
		packageName = ''
		className = fullName
	}

	[packageName, className]
}

copyFile = { String from, String to ->
	if (!okToWrite(to)) {
		return
	}

	ant.copy file: from, tofile: to, overwrite: true
}

printMessage = { String message -> event('StatusUpdate', [message]) }
errorMessage = { String message -> event('StatusError', [message]) }

setDefaultTarget('glassQuickstart')