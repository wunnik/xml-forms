/*
 * Open-T XML Forms
 * Copyright 2010-2013, Open-T B.V., and individual contributors as indicated
 * by the @author tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License
 * version 3 published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.

 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses
 */
package org.open_t.forms


import javax.servlet.http.HttpSession
import org.springframework.web.context.request.RequestContextHolder
import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext
import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
import grails.util.GrailsWebUtil


import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine
import groovy.text.*;
import java.text.*;



class FormService implements  ApplicationContextAware {

    static transactional = false
	
	def grailsApplication
	def dialogService 
	
	def applicationContext
	def servletContext = SCH.servletContext
	
	
	GroovyPagesTemplateEngine groovyPagesTemplateEngine
	
	void setApplicationContext(ApplicationContext inApplicationContext){
		applicationContext = inApplicationContext
		 //sessionFactoryBean = applicationContext.getBean('&sessionFactory')
	 }

	

	//def sessionFacory
  
    private HttpSession getSession() {
    		return RequestContextHolder.currentRequestAttributes().getSession()
    }
    

    // Process the update and execute request parameters.
    // This updates the document stored in the session
    def processParams(params,documentName='document') {
    	
    	params.each { key,value ->
			String test
			def p=key.indexOf("-")
		
			if (p>0 && value?.class?.name?.equals("java.lang.String")) {
				def instruction=key.substring(0,p);
				def gpath=key.substring(p+1);
				
				switch (instruction) {
					case "update":	
						if (!gpath.endsWith("]")) {
							gpath+="[0]"
						}
						gpath+="=\"\"\"${value}\"\"\""
						
						Binding binding = new Binding()
						binding.document=session[documentName]
						log.trace "Evaluating GPath ${gpath}"
					    def res=new GroovyShell(binding).evaluate(gpath)
	
						break;
					case "execute":
						gpath+=" ${value}"
						
						Binding binding = new Binding()
						binding.document=session[documentName]
					    def res=new GroovyShell(binding).evaluate(gpath)
						
						break;
				}
			}
    	}
	}
    
	def prettyPrint(String inputXml) {
	     Source xmlInput = new StreamSource(new StringReader(inputXml));
	     StreamResult xmlOutput = new StreamResult(new StringWriter());

	      // Configure transformer
	     Transformer transformer = TransformerFactory.newInstance().newTransformer(); // An identity transformer
	     
	     transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	     transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	     transformer.transform(xmlInput, xmlOutput);
	     return xmlOutput.getWriter().toString()
	    	     
	}
 
	// Build XML and re-parse it.
	// This fixes index sequence numbers
	def reProcess(String documentName, namespacesName='namespaces') {
		 def response = { 
				 mkp.declareNamespace(session[namespacesName])
				 mkp.yield(session[documentName]) }
         def builder = new groovy.xml.StreamingMarkupBuilder()
	     def res= builder.bind(response).toString()
	     def slurper = new XmlSlurper()
		 def document=slurper.parseText(res)
	     session[documentName]=document
		 [document:document,xml:res]
	}
	
	// Fetch the namespace map from the XML text because XmlSlurper doesn't want to share that 
    def namespaceMap(xmlText) {
        def namespaces=[:]
        xmlText.findAll("xmlns:[^=]+=\\s*[\"\'][^\"\']+[\"\']") { namespace ->
            String prefix=namespace.split("=")[0].split(":")[1].trim()
            String uri=namespace.split("=")[1].replace("\"","").replace("\'","").trim()
            namespaces.put (prefix,uri)
        }
        return namespaces
    }
   
	// Provide a Slurped instance of provided XML text
	def slurp(xmlText) {
		
            def slurper = new XmlSlurper()		
            def document=slurper.parseText(xmlText)		
            def namespaces=namespaceMap(xmlText)
            document.declareNamespace(namespaces)
            return document
	}
    
    
	
	// Generate a random number to use as document id in the session
	def generateRandomId() {	
		def random = new Random()
		return random.nextInt(1000000)		
	}
	

	// Delete a node from the document XML (JSON)
	def deleteNode(params,documentName='document') {
				
		//def session = sessionFactory.getCurrentSession()

		def process=params.process

		def gpath=params.gpath
		Binding binding = new Binding()
		
		processParams(params)
		
		
		binding.document=session[documentName]
		String evString="${gpath}.replaceNode {}"
		def rres=new GroovyShell(binding).evaluate(evString)
				
	
						
		reProcess(documentName)		

	}
    
	
		// Append a node in the document XML (JSON)
		
    def appendNode(params,documentName='document') {
		
		def gpath=params.gpath
		def node=params.node
		def process=params.process
			
		processParams(params,documentName)
			
		Binding binding = new Binding()
		binding.document=session[documentName]
			
		//String evString="""${gpath}.appendNode { ${node}; }
		//		"""
		String evString="""${gpath} + { ${node}; }
				"""

		def rres=new GroovyShell(binding).evaluate(evString)
			

			
		reProcess(documentName)
	}
	
	
	String runTemplate(String filename,model) {
		def s
		def gspFile=new File(filename)
		dialogService.check(gspFile.exists(),"formService.runTemplate.filenotfound",[filename])
		try {
			
		
			groovyPagesTemplateEngine.setApplicationContext(applicationContext)
			groovyPagesTemplateEngine.setServletContext(servletContext)
			groovyPagesTemplateEngine.setClassLoader(applicationContext.getClassLoader())
			
			Template t = groovyPagesTemplateEngine.createTemplate(gspFile)
			
			// Caching and compiling should be switched off when developing forms.
			if(grailsApplication.config.wfp.forms.cache=="false") {
				log.debug "Not caching..."
				groovyPagesTemplateEngine.reloadEnabled=true
				groovyPagesTemplateEngine.cacheResources=false
				groovyPagesTemplateEngine.clearPageCache()
			}
			
			StringWriter writer = new StringWriter();
			t.make(model).writeTo(writer)
			s= writer.toString()
			
		} catch (Exception e) {
			println "E:${e.message}"
			dialogService.error("templateService.runTemplate.processingerror",[filename,e.message])
		}
		return s
	}
	
	def dialog(xmlPath,gspPath,model=[:],documentName='document') {		
		def xsDateTimeformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
		model.putAt('dateCreated',xsDateTimeformatter.format(new Date()))
		def documentInstanceText = runTemplate(xmlPath,model)
		session[documentName]=slurp(documentInstanceText)
		session['namespaces']=namespaceMap(documentInstanceText)
		String html = runTemplate(gspPath,[document:session[documentName]])
		return html
	}
	
	def submit (params,documentName='document') {
		processParams(params,documentName)
		reProcess(documentName)
	}
    
    public String md5(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(s.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {}
        return null;
    }

    def avatar(params) {
        def size=params.s?:32
        String email=params.id.toLowerCase().trim()
        if (email && !email.contains("@")) {
            email=email+'@'+grailsApplication.config.gravatar.domain
        }
        return "https://secure.gravatar.com/avatar/"+md5(email)+"?s=${size}&d=monsterid"        
    }
    
}
