/*
* 
* Grails xml-forms plug-in
* 
* Copyright 2013 Open-T B.V., and individual contributors as indicated
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


if (!window.xmlforms) {
	window.xmlforms={};	
}

xmlforms.regexpValidator = function (value,element,expression) {
	var theRegExp=new RegExp(expression);

	//true if ok, false if nok
	return this.optional(element) || theRegExp.exec(value);
};

xmlforms.minExclValidator = function (value,element,expression) {
	var theRegExp=new RegExp(expression);
	//true if ok, false if nok
	return this.optional(element) || value > param;
};

xmlforms.maxExclValidator = function (value,element,expression) {
	var theRegExp=new RegExp(expression);
	//true if ok, false if nok
	return this.optional(element) || value < param;
};

xmlforms.evaluateDependencies = function(element) {
	val=element.value;
	name=element.name;
	$("*[depends-on='"+name+"']").each( function () {
		dependencyType=$(this).attr("dependency-type");
		dependencyParameter=$(this).attr("dependency-parameter");
		
		var show=false;
		
		switch (dependencyType) {
		case "nonempty":
			show = val !== null && val.length>0;
			break;
		case "empty":
			show = val === null || val.length===0;			
			break;
		case "true":
			show = val !== null && val.length>0;
			break;
		case "false":
			show = val === null || val.length===0 || val==="0" || val ==="false";			
			break;
		case "gt":
			show = val !== null && val>dependencyParameter;
		break;
		case "lt":
			show = val !== null && val<dependencyParameter;		
			break;
		case "eq":
			show = val !== null && val===dependencyParameter;
			break;
		case "ne":
			show = val !== null || val!==dependencyParameter;
			break;
		default:
			show = (val !== null) && (val.length>0);			
			break;
		}
		
	
		if (!show) {
			$(this).hide("fast");
			if ($(this).attr("name")) {
				$(this).attr("name",$(this).attr("name").replace("update-","hidden-"));
			}
			
		} else {
			$(this).show("fast");
			if ($(this).attr("name")) {
				$(this).attr("name",$(this).attr("name").replace("hidden-","update-"));
			}
		}
		
	}
	);
};




//This is performed on a full page reload
xmlforms.reload = function reload() {
    $("span.help").tooltip({});
    $("a.help").tooltip({});
    $("li.menu-item a").tooltip({});
	$(".dependency-source").each(function() {
  		xmlforms.evaluateDependencies(this);
	});
};

/**
 * Show dialog
 * @param id
 * @param controllerName
 * @param options
 * @param urlParams
 * @returns {Boolean}
 */
xmlforms.formDialog = function (id,controllerName, options ,urlParams) {
	var urlId=id+window.dialog.obj2ParamStr(urlParams);
    //    var urlId=id
	//var urlId="test";
	
	var dialogName = (options !== null && options["dialogname"] !== null) ? options["dialogname"] : "dialog";
	var submitName = (options !== null && options["submitname"] !== null) ? options["submitname"] : "submit"+dialogName;
		
	var refreshTableKey = (options !== null && options["refresh"] !== null) ? options["refresh"] : "NO_REFRESH";
	
	// if true, form submit will be used instead of AJAX
	var submitForm = (options !== null && options["submitform"] !== null) ? options["submitform"] : false;
	
	// if true, form will not be submitted at all
	var noSubmit = (options !== null && options["nosubmit"] !== null) ? options["nosubmit"] : false;
	
	var domainClass = (options !== null && options["domainclass"] !== null) ? options["domainclass"] : controllerName.capitalize();
	
	theUrl=window.dialog.baseUrl+'/'+controllerName+'/'+dialogName+'/'+urlId;	 	
	 
	 xmlforms.dialogHTML = $.ajax({
		url: theUrl,
		async: false,
		cache: false
	}).error(function(event, jqXHR, ajaxSettings, thrownError) { 
		window.location.reload();
	}).responseText;
    
	 
	var formelements=$(xmlforms.dialogHTML).find('form.xml-form');
        
	if (formelements.length===0) {
		window.location.reload();
	} else {
                
		var theWidth=$(xmlforms.dialogHTML).css("width") ? $(xmlforms.dialogHTML).css("width").replace("px","") : "800";
        var theHeight=$(xmlforms.dialogHTML).css("height") ? $(xmlforms.dialogHTML).css("height").replace("px","") : "600";
        
        // If there was a previous modal, remove it from the DOM.
        if (xmlforms.theDialog) {
            $(xmlforms.theDialog).remove();
        }
        xmlforms.dialogOpen=false;
        
        $("#page").append(xmlforms.dialogHTML);
        
        xmlforms.theDialog=$("#page div.modal");
        $(xmlforms.theDialog).modal({show:false,backdrop:'static'});

		 
		xmlforms.theDialog.draggable({
			handle: ".modal-header"
		});
		//theDialog.resizable();
        
		$(xmlforms.theDialog[0]).css("margin-left","-"+theWidth/2+"px");
        //$(xmlforms.theDialog[0]).css("top","50px");
		
		var submitCallback=function(frm) {
			var $form = $(frm);		
			var $target = $form.attr('data-target');
	
			var formdata=$form.serialize();
			 
			$.ajax({
				type: $form.attr('method'),
				url: $form.attr('action'),
				data: formdata,
				dataType: "json",		  
				success: function(data, status) {
					//alert (data);
					var jsonResponse=data.result;
					$(".dialog-events").trigger("dialog-refresh",{dc:domainClass,id:id,jsonResponse:jsonResponse});
				 	$(".dialog-events").trigger("dialog-message",{message:jsonResponse.message});
				 					 		
				 	if(jsonResponse.success){
				 		xmlforms.theDialog.modal("hide");				 		
				 		if (jsonResponse.nextDialog) {
				 			xmlforms.formDialog(jsonResponse.nextDialog.id,jsonResponse.nextDialog.controllerName,jsonResponse.nextDialog.options,jsonResponse.nextDialog.urlParams);
				 		}
				 	} else  {
				 		for (key in jsonResponse.errorFields) {
				 			var errorField=jsonResponse.errorFields[key];
				 			$("#"+errorField).parent().addClass("errors");				 			
				 		}
				 		xmlforms.theDialog.find("div.errors").html(jsonResponse.message);
				 		xmlforms.theDialog.find("div.errors").show();
					 	
			 		}				
				 	xmlforms.theDialog.modal("hide");
				}
			});
			//event.preventDefault();
		};
	
		xmlforms.theDialog.on('show', function (event) {
            if (!xmlforms.dialogOpen) {
                xmlforms.dialogOpen=true;
                
                $(this).trigger("dialog-open",{event:event,ui:null,'this':this,id:id,controllerName:controllerName});
	    
                $(this).keyup(function(e) {
                    if (e.keyCode === 13 && e.target.nodeName!=="TEXTAREA") {
                        $(this).parents('.ui-dialog').first().find('.ui-button').first().click();
                    return false;
                }
                });
		    	                

                $(this).find(".altselect").altselect();       	


                $(this).find(".help").tooltip({});

                // TODO use the validate submission callback, see http://jqueryvalidation.org/validate
                $(this).find('#form').validate({
                    submitHandler: function(form) {
                        submitCallback(form);
                    },
                     invalidHandler: function(event, validator) {
                        var errors = validator.numberOfInvalids();
                        if (errors) {
                            var message = errors === 1 ? '1 field has errors. It has been highlighted': errors + ' fields have errors. They have been highlighted';                            
                            var errorHTML='<div class="alert alert-error"><button type="button" class="close" data-dismiss="alert">&times;</button><strong>Error!</strong> '+message+'</div>';
                            //$(xmlforms.theDialog).find("div.modal-header").after(errorHTML);
                            $(xmlforms.theDialog).find("div.modal-body").prepend(errorHTML);
                        }
                     }
                });

                $(this).find("input[type!='hidden'],select,textarea").filter(":first").focus();

                //var fieldset=$(this).find("fieldset")[0];
	  		

            }
		});
		
		var theForm=$(xmlforms.theDialog).find("form")[0];
		 
		$(theForm).on('submit', function(event) {
			//submitCallback(theForm);
			var $form = $(this);		
			var $target = $form.attr('data-target');
	
			var formdata=$form.serialize();
			
			//alert('test');
			event.preventDefault();
		});
		
		xmlforms.theDialog.modal('show');
	}
	
};





/*
 * jQuery processsing which takes place after page reload 
 */

jQuery(function(){
  	xmlforms.reload();

  	// This copies the checkbox value into the corresponding (hidden) update field so we have a value in the post even if the checkbox value=false
  	$(document).on ("click" , 'input[type="checkbox"]',function(e) {  	
  		var updateName=this.name.replace("entry-","update-").replace(/\./g, "\\.").replace(/\[/g, "\\[").replace(/\]/g, "\\]");
  		$('input[name="'+updateName+'"]').val($(this).attr("checked")==='checked');
  	});

	// add method to validate plugin
	$.validator.addMethod("regexp",xmlforms.regexpValidator,"Value does not match regexp");
	$.validator.addMethod("minexcl",xmlforms.minExclValidator,"Value too small");
	$.validator.addMethod("maxexcl",xmlforms.maxExclValidator,"Value too large");
	
	$(document).on ("blur" , '.dependency-source',function(e) {	
			xmlforms.evaluateDependencies(e.target);
		}	
	);
	
	// Perform any jQuery initialization that needs to be redone after a form reload
	//onFormReload()
	
	// Add a node to a form
	$(document).on ("click" , '.action-insert',function(e) {
		var parentid=$(this).attr("parentid");
		var formData=$("#form").serialize();
		
		 $.ajax({
 			url: this.href,
 			async: true,
 			data: formData,
 			type:'POST',
 			success: function(data) {
 				var newHTML = $(data).find("#"+parentid);
 				$("#"+parentid).html(newHTML); 				
 				$(".dialog-events").trigger("dialog-refresh",{});
 			}
		 });

		return false;
	});
	
	
	// Delete a node from a form
	$(document).on ("click" , '.action-delete',function(e) {    	
    	var parentid=$(this).attr("parentid");
		var formData=$("#form").serialize();
		
		$.ajax({
 			url: this.href,
 			async: true,
 			data: formData,
 			type:'POST',
 			success: function(data) {
 				var newHTML = $(data).find("#"+parentid);
 				$("#"+parentid).html(newHTML); 				
 				$(".dialog-events").trigger("dialog-refresh",{});
 			}
		 });
		return false;
  });
    
    
    
});


jQuery(function() {
	$("body").on("dialog-refresh",xmlforms.reload);
		
});

