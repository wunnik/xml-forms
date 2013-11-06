grails.plugin.location.'jquery-dialog' = "../dialog"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {

        inherits true // Whether to inherit repository definitions from plugins 
 
        grailsPlugins() 
        grailsHome() 
        mavenLocal() 
        grailsCentral() 
        mavenCentral() 

    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
    }

    plugins {
		// for 2.3.x
        //build ":tomcat:7.0.42"
		
		// for 2.2.4
        build ":tomcat:$grailsVersion"
		
    }
}
