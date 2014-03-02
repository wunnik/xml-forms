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
import java.text.*;
import org.apache.commons.lang.WordUtils
import org.apache.commons.lang.StringEscapeUtils

/**
 * XML forms tag library
 */
class FormTagLib {
	static namespace="form"
	def solrService

    /**
     * form:form
     * Renders a form
     * Attributes:
     * type Type of form to render, one of: request,edit,show,task
     * outcomes String of comma separated outcomes. Eacht outcome can be either a plain String or a colon-separated key:label pair
     * enctype Encoding type, added to the <form> element
     * style Style attribute added to the <form> element
     * lock Name of the lock attribute to set to the outer <div>. When used in cojunction with wfp-forms this will trigger setting the according lock
     * title The title of the form
     * action The action of the <form>
     * name The internal name of this form, posted as 'form'
     * process The name , posted as 'process'
     */
	def form = { attrs, body ->
		def enctype=""
		def style=""
		def outcomes = attrs.outcomes

		if (attrs.enctype) { enctype="""enctype="${attrs.enctype}" """}
		if (attrs.style) { style="""style="${attrs.style}" """}

        def lock=""
        def cssClass=""
        if (attrs.lock) {
            lock="""lock="${attrs.lock}" """
            cssClass+=" lock"
        }

		def formHead="""<div ${style}${lock} id="${attrs.name}" class="xml-form modal hide xfade${cssClass}" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
                            <div class="modal-header">
								<button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
								<div id="myModalLabel"><span class="modal-header">${attrs.title}</span>&nbsp;<span class="modal-explanation">${attrs.explanation}</span></div>
                            </div>
						"""
		// explanation
		formHead+="""<form class="form-horizontal xml-form" action="${attrs.action}" id="form" name="form" method="post" ${enctype} >"""
		formHead+="""<div class="modal-body">"""
		formHead+="""<div class="errors" style="display:none;"></div>"""
        formHead+="""<fieldset>"""
		formHead+="""<input type="hidden" name="form" value="${attrs.name}" />"""
		formHead+="""<input type="hidden" name="process" value="${attrs.process}" />"""
        formHead+="""<input id="${attrs.name}-outcome" type="hidden" name="outcome" value="" />"""

        out << formHead
        out << body()
        out << """</fieldset></div>"""
        out << """<div class="aaform-actions modal-footer">"""


		switch (attrs.type) {
			case "request":
				out << """<input type="submit" id="submit" value="${message(code:'submit')}" name="submit" class="btn btn-primary" role="button"  outcome="none" outcome-id="${attrs.name}-outcome"></input>"""
			break

			case "edit":
					out << """<input type="submit" id="submit" value="${message(code:'save',default:'Save')}" name="submit" class="btn" role="button"  outcome="none" outcome-id="${attrs.name}-outcome"></input>"""
			break

			case "show" :
				out <<""
			break

            /*
             * Format for outcomes:
             * outcome
             * outcome:messageId
             *
             */

			case "task" :
				if(attrs.outcomes) {
					attrs.outcomes.split(",").each { outcomeString ->
                        def outcome
                        def outcomeLabelId
                        def colonPosition=outcomeString.indexOf(":")
                        if (colonPosition>-1) {
                            outcome=outcomeString.substring(0,colonPosition)
                            outcomeLabelId=outcomeString.substring(colonPosition+1)
                        } else {
                            outcome=outcomeString
                            outcomeLabelId=outcomeString
                        }
                        def outcomeLabel=message(code:"outcome.${outcomeLabelId}.label",default:outcomeLabelId)
                        def outcomeTitle=message(code:"outcome.${outcomeLabelId}.title",default:outcomeLabel)
						out << """<input type="submit" class="btn help-tooltip outcome-submit" outcome-id="${attrs.name}-outcome" outcome="${outcome}" value="${outcomeLabel}" title="${outcomeTitle}" name="submit" class="button btn" role="button"  />"""
					}
			    }
			break
		}
	    out << """</div></form></div>"""
	}

    // Unused?
	def attributeString(attributeName,attributeValue) {
		if (attributeValue && attributeValue.length()>0) {
			return """${attributeName}="${attributeValue}" """
		} else {
			return ""
		}
	}

    /**
     * Expand atrribute map into key="value" ... String ready to be inserted in a an element
     *
     * @param attrs The attribute map
     * @return String
     */
	def expandAttribs(attrs) {
		def attribs=""
		attrs.each { attrKey, attrValue -> attribs+=""" ${attrKey}="${attrValue}" """ }
		return attribs
	}

    /**
     * form:input
     * Generic input field
     * Attributes:
     * type=text,textarea,checkbox,date,dateTime.time
     * value
     * name
     * gpath
     * class
     * helpTitle
     * helpBody
     */

	def input = { attrs, body ->

		// Copy all extra attributes, skip the ones that are only meaningful for textField or are handled manually
		def newAttrs=attrs.clone()
		def attribs=""
		def skipAttrs=['class','type','value','name','id']
		attrs.each { attrKey, attrValue ->
			if (!skipAttrs.contains(attrKey))
			{
				attribs+=""" ${attrKey}="${attrValue}" """
			 }
		}

		out << """<div class="dialog-horizontal-wrapper">"""
		switch (attrs.type) {

            // Text type simply uses a standard input element
			case "text":
				newAttrs.name="update-${attrs.gpath}"
				newAttrs.id="update-${attrs.gpath}"
				out << """<input ${expandAttribs(newAttrs)} />"""
			break

            // Date type uses the datepicker, along with a hidden field that holds the ISO formatted date
			case "date":
			  def xsDateformatter = new SimpleDateFormat("yyyy-MM-dd")
			  def dateFormatter = new SimpleDateFormat(g.message(code: 'input.date.format',default:"yyyy-MM-dd HH:mm:ss z"))
			  def dateValue
			  if (attrs.value.text().length() > 0) {
			    dateValue = xsDateformatter.parse(attrs.value.text())
			  }

              // If this field is required we need to put the required class on the hidden field rather than the visible inout field
			  def classes = attrs.class.split(" \\s*")
			  def inputClass = ""
			  def hiddenClass = ""
			  classes.each {
			    if (it == "required") {
        	        hiddenClass += "${it} "
			    }
			    else {
                    inputClass += "${it} "
			    }
			  }
				out << """<input id="entry-${attrs.gpath}" name="entry-${attrs.gpath}" type="text" class="${inputClass} datepicker" ${attribs} value="${dateValue ? dateFormatter.format(dateValue) : ''}"  />"""
				out << """<input id="update-${attrs.gpath}" name="update-${attrs.gpath}" type="hidden" class="${hiddenClass}" value="${attrs.value}" />"""
			break

              // Textarea simply uses a <textarea> element
			case "textarea":
				newAttrs.name="update-${attrs.gpath}"
				newAttrs.id="update-${attrs.gpath}"
				newAttrs.remove('value')
				out << """<textarea ${expandAttribs(newAttrs)} >${attrs.value?:""}</textarea>"""
			break

            // Checkbox
			case "checkbox":
				def checked=""
				if (attrs.value=="true") {
					checked="""checked="checked" """
				}
				out << """<input ${checked} class="${attrs.class}" name="entry-${attrs.gpath}" value="${attrs.value}" id="entry-${attrs.gpath}" type="checkbox"  />"""
				out << """<input name="update-${attrs.gpath}" value="${attrs.value}" id="update-${attrs.gpath}" type="hidden" />"""
			break

            // DateTime
			case "datetime":
                def xsDateTimeformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                def dateTimeFormatter = new SimpleDateFormat(g.message(code: 'input.dateTime.format'))
                def dateTimeValue
                if (attrs.value.text().length() > 0) {
                    dateTimeValue = xsDateTimeformatter.parse(attrs.value.text())
                }
                def classes = attrs.class.split(" \\s*")
                def inputClass = ""
                def hiddenClass = ""
                classes.each {
                    if (it == "required") {
                        hiddenClass += "${it} "
                    } else {
                        inputClass += "${it} "
                    }
                }
                out << """<input id="hidden-${attrs.gpath}" name="hidden-${attrs.gpath}" type="text" class="${inputClass}" ${attribs} value="${dateTimeValue ? dateTimeFormatter.format(dateTimeValue) : ''}" >"""
				out << """<input id="update-${attrs.gpath}" name="update-${attrs.gpath}" type="hidden" class="${hiddenClass}" value="${attrs.value}" ${title} />"""
			break
			case "time":
                def xsTimeformatter = new SimpleDateFormat("HH:mm:ss")
                def timeFormatter = new SimpleDateFormat(g.message(code: 'input.time.format'))
                def timeValue
                if (attrs.value.text().length() > 0) {
                    timeValue = xsTimeformatter.parse(attrs.value.text())
                }
                def classes = attrs.class.split(" \\s*")
                def inputClass = ""
                def hiddenClass = ""
                classes.each {
                    if (it == "required") {
                        hiddenClass += "${it} "
                    } else {
                        inputClass += "${it} "
                    }
                }
				out << """<input id="hidden-${attrs.gpath}" name="hidden-${attrs.gpath}" type="text" class="${inputClass}" ${attribs} value="${timeValue ? timeFormatter.format(timeValue) : ''}" />"""
				out << """<input id="update-${attrs.gpath}" name="update-${attrs.gpath}" type="hidden" class="${hiddenClass}" value="${attrs.value}" />"""
			break
		}

		if (attrs.helpTitle || attrs.helpBody) {
		  out << """&nbsp;<span class="help-icon help action" title="${attrs.helpTitle}" data-content="${attrs.helpBody}" href="#">&nbsp;</span>"""
		}

		out <<"</div>"

	}

    /**
     * form:select
     * Select field
     *
     * Attributes:
     * options
     * title
     * value
     * class
     * gpath
     * helpTitle
     * helpBody
     */
	def select = { attrs, body ->
        	def title = attrs.title ? """title="${attrs.title}" """ : ""

		def options
		attrs.options.each { def item ->
			String attrValue=attrs.value ? attrs.value : ""
			String key = item.getKey() ? item.getKey() : ""
			String value=item.getValue() ? item.getValue() : ""
			options = """${options ? options : ""}<option value="${key}"${key == attrValue ? ' selected' : ''}>${value}</option>"""
		}

		out << """<select class="${attrs.class}" name="update-${attrs.gpath}" id="update-${attrs.gpath}" ${title}>${options}</select>"""

		if (attrs.helpTitle || attrs.helpBody) {
		  out << """&nbsp;<a class="help-icon help action" title="${attrs.helpTitle}" data-content="${attrs.helpBody}"  href="#">&nbsp;</a>"""
		}
	}
    /**
     * form:radioButtonList
     * Radio buttons
     *
     * Attributes:
     * title
     * options
     * class
     * helpTitle
     * helpBody
     */

	def radioButtonList = { attrs, body ->
		def title = attrs.title ? """title="${attrs.title}" """ : ""

		def radioButtons

		attrs.options.each { def item ->
			if (item.getKey() != "") {
				radioButtons = """${radioButtons ? radioButtons : ""}<tr><td><input class="${attrs.class}" name="update-${attrs.gpath}" value="${item.getKey()}" id="update-${attrs.gpath}" ${title} type="radio" ${item.getKey() == attrs.value ? "selected" : ""}/></td><td>${item.getValue()}</td><td>${!radioButtons && (attrs.helpTitle || attrs.helpBody) ? """<a class="help-icon help action" title="${attrs.helpTitle}|${attrs.helpBody}" href="#">&nbsp;</a>""" : ""}</td></tr>"""
			}
		}

		if (!radioButtons) {
			radioButtons = """<tr><td>&nbsp;<a class="help-icon help action" title="${attrs.helpTitle}" data-content="${attrs.helpBody}"  href="#">&nbsp;</a></td></tr>"""
		}

		out << """<table>${radioButtons}</table>"""
	}

    /**
     * form:output
     * Output field
     *
     * Attributes:
     * type=text,date,textarea,datetime,time
     * class
     * gpath
     * value
     * helpTitle
     * helpBody
     */
	def output = { attrs, body ->
		out << """<div class="dialog-horizontal-wrapper">"""

        switch (attrs.type) {
            // Text
            case "text":
                out << """<span class="${attrs.class}" id="output-${attrs.gpath}">${attrs.value.toString().replaceAll('\n', '<br />')}</span>"""
            break

            // Date
            case "date":
                def xsDateformatter = new SimpleDateFormat("yyyy-MM-dd")
                def dateFormatter = new SimpleDateFormat(g.message(code: 'output.date.format'))
                def dateValue
                if (attrs.value.text().length() > 0) {
                  dateValue = xsDateformatter.parse(attrs.value.text())
                }
                out << """<span class="${attrs.class}" id="output-${attrs.gpath}">${dateValue ? dateFormatter.format(dateValue) : ''}</span>"""
            break

            // Textarea
            case "textarea":
                out << """<span class="${attrs.class}" id="output-${attrs.gpath}">${attrs.value.toString().replaceAll('\n', '<br />')}</span>"""
            break

            // Datetime
            case "datetime":
                def xsDateTimeformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                def dateTimeFormatter = new SimpleDateFormat(g.message(code: 'output.dateTime.format'))
                def dateTimeValue
                if (attrs.value.text().length() > 0) {
                  dateTimeValue = xsDateTimeformatter.parse(attrs.value.text())
                }
                out << """<span class="${attrs.class}" id="output-${attrs.gpath}">${dateTimeValue ? dateTimeFormatter.format(dateTimeValue) : ''}</span>"""
            break

            // Time
            case "time":
                def xsTimeformatter = new SimpleDateFormat("HH:mm:ss")
                def timeFormatter = new SimpleDateFormat(g.message(code: 'output.time.format'))
                def timeValue
                if (attrs.value.text().length() > 0) {
                  timeValue = xsTimeformatter.parse(attrs.value.text())
                }
                out << """<span class="${attrs.class}" id="output-${attrs.gpath}">${timeValue ? timeFormatter.format(timeValue) : ''}</span>"""
            break
		}

		if (attrs.helpTitle || attrs.helpBody) {
		  out << """&nbsp;<a class="help-icon help action" title="${attrs.helpTitle}" data-content="${attrs.helpBody}"  href="#">&nbsp;</a>"""
		}
		out << """</div>"""

	}

    /**
     * form:comment
     * Comment list and input text box
     *
     * Attributes:
     * rows
     * cols
     * dateTime
     * document
     * value
     * mode=edit,create
     * helpTitle
     * helpBody
     */
	def comment={attrs,body ->
		def formatter = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss")

		def rows=attrs.rows?:10
		def cols=attrs.cols?:80

		def html="""<ul class="comment">"""
		def doc=attrs.document

		doc.header.comments.comment.each { comment ->
			def formattedCommentDate="unknown"
			if(comment.dateTime) {
				def commentDate=(Date)formatter.parse(comment.dateTime.text())
				formattedCommentDate=commentDate.dateTimeString
			}
		    html+="""<li><div class="comment-item">
								<div class="comment-author"><img src="${createLink(action:'avatar',id:comment.user.text())}"/><span class="fn">${comment.user.text()}</span></div>
								<div class="comment-text">${comment.text.text()}</div>
								<div class="comment-date">${message(code:'form.comment.date.label')} <span class="date">${formattedCommentDate}</span></div>
							</div></li>"""
		}
		def value = attrs.value ? attrs.value : ""
		if (attrs.mode && (attrs.mode=="edit" || attrs.mode=="create")) {
			html+="""<li><div class="newcomment"><h5>${message(code:'form.comment.add.label')}</h5><textarea class="input-xxlarge" rows="${rows}" cols="${cols}" name="comment" >${value}</textarea>"""
			if (attrs.helpTitle || attrs.helpBody) {
				html+= """&nbsp;<a href="#" class="help-icon help action" title="${attrs.helpTitle}" data-content="${attrs.helpBody}"  >&nbsp;</a>"""
			}
			html +="</div></li>"
		}
		html+="</ul>"
		out << html
    }

    /**
     * form:hasFeature
     * Determine if current user has said feature
     * Attributes:
     * feature The feature to check for
     */
    def hasFeature = {attrs,body ->
        println "The features are: ${session.features}"
        if (session.features && session.features.contains(attrs.feature.toString())) {
            out << body()
        }
    }

    /**
     * form:outcome
     * Outcome button
     * The value of the outcome is in the element body
     */
    def outcome = {attrs,body ->
        out << """<input type="submit" id="submit" value="${body()}" name="submit" class="button ui-button ui-widget ui-state-default ui-corner-all" role="button" aria-disabled="false" />"""
    }

     /**
     * form:line
     * Create a bootstrap form line
     * Attributes
     * id    - the id of the input element. Used for the for= and as default key for the label
     * label - Label to be shown
     */
    def line= {attrs,body ->
        def id=attrs.id?:""

        def label=attrs.label?:message(code:'formline.'+id,default:WordUtils.capitalize(id))
        def html = """
        <div class="control-group" >
        <label class="control-label " for="${id}">${label}</label>
        <div class="controls ">"""
        out << html
        out << body()
        out << """</div></div>"""
    }

    /**
     * form:unescape
     * HTML-unescapes the body of the tag
     */
    def unescape = { attrs, body ->
        out << StringEscapeUtils.unescapeHtml(body().toString())
    }
}
